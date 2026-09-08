package com.kaiser0733.g102controller.usb

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface

/**
 * Turns a UsbDevice into a plain-text descriptor dump for the diagnostics panel.
 * Pure formatting over the Android USB classes; no state, no side effects.
 */
object UsbDiagnostics {

    fun describeDevice(device: UsbDevice): String = buildString {
        appendLine("Device: ${device.deviceName}")
        appendLine("  VID=0x%04x PID=0x%04x".format(device.vendorId, device.productId))
        appendLine("  Class=${device.deviceClass} Subclass=${device.deviceSubclass} Protocol=${device.deviceProtocol}")
        appendLine("  Configurations=${device.configurationCount} Interfaces=${device.interfaceCount}")
        appendLine("  Manufacturer=${safeString { device.manufacturerName }}")
        appendLine("  Product=${safeString { device.productName }}")
        val serial = try {
            device.serialNumber
        } catch (_: SecurityException) {
            null
        }
        appendLine("  Serial=${serial ?: "<unavailable>"}")
        for (i in 0 until device.interfaceCount) {
            append(describeInterface(device.getInterface(i), i))
        }
    }

    fun describeInterface(iface: UsbInterface, index: Int): String = buildString {
        appendLine("Interface $index:")
        appendLine("  ID=${iface.id} Class=${iface.interfaceClass} Subclass=${iface.interfaceSubclass} Protocol=${iface.interfaceProtocol}")
        appendLine("  Endpoints=${iface.endpointCount}")
        for (e in 0 until iface.endpointCount) {
            append(describeEndpoint(iface.getEndpoint(e)))
        }
    }

    fun describeEndpoint(endpoint: UsbEndpoint): String = buildString {
        val address = endpoint.address
        appendLine("  Endpoint: address=0x%02x".format(address))
        appendLine("    Number=${endpoint.endpointNumber} Direction=${directionName(address)}")
        appendLine("    Type=${typeName(endpoint.type)} MaxPacketSize=${endpoint.maxPacketSize} Interval=${endpoint.interval}")
    }

    fun directionName(address: Int): String =
        if (address and UsbConstants.USB_DIR_IN != 0) "IN" else "OUT"

    fun typeName(type: Int): String = when (type) {
        UsbConstants.USB_ENDPOINT_XFER_CONTROL -> "CONTROL"
        UsbConstants.USB_ENDPOINT_XFER_ISOC -> "ISOCHRONOUS"
        UsbConstants.USB_ENDPOINT_XFER_BULK -> "BULK"
        UsbConstants.USB_ENDPOINT_XFER_INT -> "INTERRUPT"
        else -> "UNKNOWN($type)"
    }

    private fun safeString(getter: () -> String?): String = try {
        getter() ?: "<unavailable>"
    } catch (_: SecurityException) {
        "<permission denied>"
    }
}
