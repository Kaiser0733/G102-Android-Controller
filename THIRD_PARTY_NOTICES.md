# Third-party notices

This project incorporates no third-party runtime libraries (zero runtime
dependencies by design, see DECISIONS.md D6). The materials below informed
development or are compiled into the APK by the toolchain.

## Reference implementations (protocol knowledge; no code incorporated)

### smasty/g203-led — MIT

- Upstream: https://github.com/smasty/g203-led
- License: MIT — Copyright (c) 2018 Smasty
- What was used: the HID++/LIGHTSYNC command byte layouts (mode-switch
  sequence, color/effect report structure, zone triple command) were derived
  from its behavior and cross-checked against its output. Unit-test vectors in
  this repository were mechanically extracted from its formatted command
  output by script. No source code from the reference is included in this
  repository; the controller is an independent Kotlin implementation.
- Upstream notice (MIT):

  ```
  MIT License

  Copyright (c) 2018 Smasty

  Permission is hereby granted, free of charge, to any person obtaining a copy
  of this software and associated documentation files (the "Software"), to deal
  in the Software without restriction, including without limitation the rights
  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  copies of the Software, and to permit persons to whom the Software is
  furnished to do so, subject to the following conditions:

  The above copyright notice and this permission notice shall be included in all
  copies or substantial portions of the Software.

  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  SOFTWARE.
  ```

Note: g203-led states it is inspired by and based on g810-led (GPL). This
project did not consult or incorporate g810-led source; protocol command
layouts are interoperability facts (see LICENSING.md).

### libratbag — MIT

- Upstream: https://github.com/libratbag/libratbag
- License: MIT — Copyright © 2015-2017 Red Hat, Inc.;
  Copyright © 2015 David Herrmann
- What was used: general HID++ background knowledge (report IDs, wired
  device index, feature-index conventions). No source code incorporated.
- Upstream notice (MIT):

  ```
  Copyright © 2015-2017 Red Hat, Inc.
  Copyright © 2015 David Herrmann <dh.herrmann@gmail.com>

  Permission is hereby granted, free of charge, to any person obtaining a
  copy of this software and associated documentation files (the "Software"),
  to deal in the Software without restriction, including without limitation
  the rights to use, copy, modify, merge, publish, distribute, sublicense,
  and/or sell copies of the Software, and to permit persons to whom the
  Software is furnished to do so, subject to the following conditions:

  The above copyright notice and this permission notice (including the next
  paragraph) shall be included in all copies or substantial portions of the
  Software.

  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
  FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
  DEALINGS IN THE SOFTWARE.
  ```

## Compiled into the APK by the toolchain

### Kotlin Standard Library — Apache License 2.0

- Copyright: JetBrains s.r.o.
- The Kotlin compiler bundles the stdlib into the APK (classes.dex /
  kotlin_builtins resources). Not a declared dependency of this project.
- License: https://www.apache.org/licenses/LICENSE-2.0

### Android SDK / platform libraries — Apache License 2.0

- Copyright: The Android Open Source Project
- The app links against the Android SDK at compile time; no Android platform
  code is redistributed inside the APK beyond the compile toolchain's output.

## Test-only dependencies (not distributed in the APK)

### JUnit 4.13.2 — Eclipse Public License 1.0

- Copyright: JUnit contributors. https://junit.org
- Used exclusively by the JVM unit-test suite (`testImplementation`).

## Assets

The launcher icon and all drawables in this repository are original vector
artwork created for this project. No third-party images, fonts, or icons are
included.
