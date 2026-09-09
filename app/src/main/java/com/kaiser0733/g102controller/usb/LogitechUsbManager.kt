package com.kaiser0733.g102controller.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import com.kaiser0733.g102controller.protocol.ProtocolPackets

/**
 * All USB plumbing for one Logitech mouse: discovery, permission, interface
 * selection, HID SET_REPORT transport, and the response read.
 *
 * Design notes:
 *  - The UsbDevice object is NEVER cached across commands. Android hands out a new
 *    object after reconnect, so every public entry point re-enumerates by name.
 *  - The HID++ interface is selected by scanning for a HID-class interface that is
 *    not the plain mouse/boot interface — plus the hardcoded fallback of the
 *    reference implementation (interface 1). Both must agree on G102/G203.
 */
class LogitechUsbManager(
    private val context: Context,
    private val log: (String) -> Unit,
) {

    companion object {
        const val LOGITECH_VENDOR_ID = 0x046D

        /** Known LIGHTSYNC-capable mice in this family. Preference list, never a gate. */
        val KNOWN_LIGHTSYNC_PIDS = listOf(0xC092, 0xC09D, 0xC084)

        private const val TAG = "G102Controller"
    }

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

    // --- Discovery ---------------------------------------------------------------

    /** All Logitech devices currently attached, most-preferred first. */
    fun findLogitechDevices(): List<UsbDevice> =
        usbManager.deviceList.values
            .filter { it.vendorId == LOGITECH_VENDOR_ID }
            .sortedByDescending { it.productId in KNOWN_LIGHTSYNC_PIDS }

    fun hasPermission(device: UsbDevice): Boolean = usbManager.hasPermission(device)

    // --- Permission ----------------------------------------------------------

    /** Action string of the USB permission broadcast this manager requests. */
    val permissionAction = "${context.packageName}.USB_PERMISSION"

    /**
     * Registers attach/detach/permission receivers. Must be paired with [unregister].
     * Uses RECEIVER_NOT_EXPORTED on T+ (required) and FLAG_MUTABLE on S+ so the system
     * can attach EXTRA_DEVICE/EXTRA_PERMISSION_GRANTED to the broadcast.
     */
    fun register(onEvent: (Intent) -> Unit) {
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction(permissionAction)
        }
        // BroadcastReceiver is an abstract class, not a SAM interface — object expression required.
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                onEvent(intent)
            }
        }
        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(receiver, filter)
        }
        registeredReceiver = receiver
    }

    private var registeredReceiver: BroadcastReceiver? = null

    fun unregister() {
        registeredReceiver?.let { context.unregisterReceiver(it) }
        registeredReceiver = null
    }

    fun requestPermission(device: UsbDevice) {
        // Explicit + mutable: the system must attach extras before broadcasting back.
        val intent = Intent(permissionAction).setPackage(context.packageName)
        val flags = if (Build.VERSION.SDK_INT >= 31) {
            PendingIntent.FLAG_MUTABLE
        } else {
            0
        }
        val pi = PendingIntent.getBroadcast(context, 0, intent, flags)
        log("Requesting USB permission for VID=0x%04x PID=0x%04x".format(device.vendorId, device.productId))
        usbManager.requestPermission(device, pi)
    }

    // --- Interface selection ---------------------------------------------------

    /**
     * Selects the HID++ vendor interface.
     *
     * Selection order (matches the G102/G203 LIGHTSYNC reality, where interface 1
     * is the vendor HID++ node and interface 0 is the plain mouse):
     *  1. A HID-class interface whose subclass/protocol is NOT the standard
     *     mouse (subclass 1, protocol 2) — the vendor node.
     *  2. If only one HID interface exists at all, use it.
     *  3. Fallback: the reference implementation's wIndex=1.
     */
    fun selectHidppInterface(device: UsbDevice): UsbInterface? {
        val hidInterfaces = (0 until device.interfaceCount)
            .map { device.getInterface(it) }
            .filter { it.interfaceClass == UsbConstants.USB_CLASS_HID }

        // Preference order:
        //  1. the classic vendor node: HID class, subclass 0, protocol 0 (G102/G203 iface 1)
        //  2. any HID interface that is neither the boot mouse (1/2) nor a keyboard (1/1)
        //  3. the only HID interface, if there is exactly one
        //  4. the reference implementation's fixed interface index 1
        val vendorNode = hidInterfaces.firstOrNull {
            it.interfaceSubclass == 0 && it.interfaceProtocol == 0
        } ?: hidInterfaces.firstOrNull {
            !(it.interfaceSubclass == 1 && (it.interfaceProtocol == 1 || it.interfaceProtocol == 2))
        }
        val chosen = vendorNode
            ?: hidInterfaces.takeIf { it.size == 1 }?.first()
            ?: device.getInterface(1).takeIf { device.interfaceCount > 1 }

        log(
            if (chosen != null) {
                "Selected HID++ interface index ${interfaceIndex(device, chosen)} " +
                    "(class=${chosen.interfaceClass} subclass=${chosen.interfaceSubclass} " +
                    "protocol=${chosen.interfaceProtocol} endpoints=${chosen.endpointCount})"
            } else {
                "No HID++ interface found on this device"
            }
        )
        return chosen
    }

    private fun interfaceIndex(device: UsbDevice, iface: UsbInterface): Int {
        for (i in 0 until device.interfaceCount) {
            if (device.getInterface(i) == iface) return i
        }
        return -1
    }

    // --- Transport ----------------------------------------------------------------

    /**
     * Sends one HID++ report via HID SET_REPORT on endpoint 0 (mirrors the reference
     * implementation's control transfer) and reads the device's 20-byte response
     * from the claimed interface's IN interrupt endpoint.
     *
     * Returns the response bytes, or null when the read times out — a null response
     * with a positive write result still means the command reached the device.
     */
    fun sendReportAndRead(
        connection: UsbDeviceConnection,
        iface: UsbInterface,
        report: ByteArray,
        timeoutMs: Int = 500,
    ): SendResult {
        val wValue = when (val mapped = ProtocolPackets.wValueForReportSize(report.size)) {
            null -> {
                log("Refusing to send malformed report of ${report.size} bytes")
                return SendResult(error = "malformed report size ${report.size}")
            }
            else -> mapped
        }

        // Android's controlTransfer: requestType 0x21 (class, interface, host→device),
        // request 0x09 (HID SET_REPORT), wValue = report type|ID, wIndex = interface
        // number, payload = the report bytes. This is the exact shape of the
        // reference's libusb ctrl_transfer(0x21, 0x09, ...).
        val ifaceIndex = interfaceIndexFor(iface)
        val sent = connection.controlTransfer(
            /* requestType = */ 0x21,
            /* request = */ 0x09,
            /* value = */ wValue,
            /* index = */ ifaceIndex,
            /* buffer = */ report,
            /* length = */ report.size,
            /* timeout = */ timeoutMs,
        )
        log(
            "SET_REPORT wValue=0x%04x wIndex=%d len=%d TX=%s -> %d bytes".format(
                wValue, ifaceIndex, report.size, ProtocolPackets.toHex(report), sent,
            )
        )
        if (sent < 0) {
            return SendResult(bytesWritten = sent, error = "control transfer failed, errno $sent")
        }

        // Response read: interrupt IN endpoint of the claimed interface. Android exposes
        // interrupt endpoints through bulkTransfer; the reference reads 20 bytes from 0x82.
        val inEndpoint: UsbEndpoint? = findInEndpoint(iface)
        if (inEndpoint == null) {
            log("No IN endpoint on claimed interface — write succeeded, response unavailable")
            return SendResult(bytesWritten = sent, response = null)
        }
        val buf = ByteArray(20)
        val read = connection.bulkTransfer(inEndpoint, buf, buf.size, timeoutMs)
        val response = if (read > 0) buf.copyOf(read.coerceAtMost(20)) else null
        if (response != null) {
            log("RX(%d)=%s".format(response.size, ProtocolPackets.toHex(response)))
        } else {
            log("Response read returned $read (timeout or empty) — command may still have applied")
        }
        return SendResult(bytesWritten = sent, response = response)
    }

    /**
     * Drains any reports already sitting in the response endpoint (the reference
     * implementation's clear_ls_buffer): mouse power-on effects or a previous app
     * session can leave stale reports queued, which would otherwise be misread as
     * the response to the next command. Short 10ms timeout per attempt.
     */
    fun drainStaleResponses(connection: UsbDeviceConnection, iface: UsbInterface, maxAttempts: Int = 8) {
        val endpoint = findInEndpoint(iface) ?: return
        val buf = ByteArray(20)
        var drained = 0
        repeat(maxAttempts) {
            val read = connection.bulkTransfer(endpoint, buf, buf.size, 10)
            if (read <= 0) return
            drained++
            log("Drained stale response: ${ProtocolPackets.toHex(buf.copyOf(read.coerceAtMost(20)))}")
        }
        if (drained >= maxAttempts) log("Endpoint still had data after $maxAttempts drains — continuing anyway")
    }

    /**
     * wIndex for control transfers: the interface's bInterfaceNumber (iface.id),
     * which is what the device's descriptor table uses — not the enumeration position.
     */
    private fun interfaceIndexFor(iface: UsbInterface): Int = iface.id

    private fun findInEndpoint(iface: UsbInterface): UsbEndpoint? =
        (0 until iface.endpointCount)
            .map { iface.getEndpoint(it) }
            .firstOrNull { it.direction == UsbConstants.USB_DIR_IN && it.type == UsbConstants.USB_ENDPOINT_XFER_INT }

    // --- Connection lifecycle -------------------------------------------------------

    fun openDevice(device: UsbDevice): UsbDeviceConnection? {
        if (!usbManager.hasPermission(device)) {
            log("openDevice called without permission — refusing")
            return null
        }
        return usbManager.openDevice(device)
    }

    fun claimInterface(connection: UsbDeviceConnection, iface: UsbInterface): Boolean =
        try {
            connection.claimInterface(iface, true)
        } catch (t: Throwable) {
            Log.e(TAG, "claimInterface failed", t)
            log("claimInterface failed: ${t.message}")
            false
        }

    fun close(connection: UsbDeviceConnection, iface: UsbInterface?) {
        try {
            iface?.let { connection.releaseInterface(it) }
        } catch (_: Throwable) {
        }
        try {
            connection.close()
        } catch (_: Throwable) {
        }
    }
}

/** Outcome of one report send. bytesWritten < 0 = transfer error. response null = no reply read. */
data class SendResult(
    val bytesWritten: Int = 0,
    val response: ByteArray? = null,
    val error: String? = null,
)
