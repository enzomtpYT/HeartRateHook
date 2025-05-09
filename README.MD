![HeartRateHook](https://socialify.git.ci/enzomtpYT/HeartRateHook/image?font=Inter&forks=1&issues=1&language=1&name=1&owner=1&pattern=Solid&pulls=1&stargazers=1&theme=Auto)

![above](https://img.shields.io/badge/Android-8.0%20or%20above-brightgreen.svg)

[![license](https://img.shields.io/github/license/enzomtpYT/HeartRateHook.svg)](https://www.gnu.org/licenses/gpl-3.0.html)

---

# HeartRateHook

## Project Overview

HeartRateHook is a project for monitoring and reporting heart rate data from Mi Health app to a webhook URL.

This project is a fork of [xihan123/HeartRateHook](https://github.com/xihan123/HeartRateHook) with modifications to simplify the data reporting process.

## Supported Applications

* Mi Health (Mi Fitness) version 3.32.0+

## Main Features

* Monitor heart rate data updates in real-time
* Debounce heart rate data to reduce noise
* Send heart rate data to webhook URL
* Only send data when heart rate values change
* Support for both sport mode and non-sport mode monitoring

## Build Information

### Requirements

* Android SDK 35 (target)
* Android SDK 26 (minimum) / Android 8.0+
* JDK 17
* Gradle 8.0+

### Build Instructions

1. Clone the repository:
   ```bash
   git clone https://github.com/enzomtpYT/HeartRateHook.git
   cd HeartRateHook
   ```

2. Create a `keystore.properties` file in the project root with the following content:
   ```properties
   storeFile=your_keystore_path.jks
   storePassword=your_store_password
   keyAlias=your_key_alias
   keyPassword=your_key_password
   ```

3. Build the project:
   ```bash
   ./gradlew assembleRelease
   ```

4. To install directly to a connected device:
   ```bash
   ./gradlew installDebug
   ```
   This will automatically stop and restart the Mi Health app.

### Available Build Variants

The project generates multiple APK variants for different CPU architectures:

* `arm32` - For 32-bit ARM devices
* `arm64` - For 64-bit ARM devices
* `armAll` - For both 32-bit and 64-bit ARM devices
* `universal` - For all supported architectures (ARM and x86)

## Project Structure

* `keystore.properties`: Contains signing configurations like signature passwords
* `local.properties`: Contains local configurations like Android SDK paths

## How to Use

1. Select the module scope in your Xposed framework (LSPosed, EdXposed, etc.)

2. In the supported app (Mi Health) -> About -> Click on the icon -> Configure webhook URL in the dialog box

3. Make sure you visit the device page at least once to establish proper connection

## Contributing

Issues and pull requests are welcome. Please fork this project and submit pull requests with your contributions.

## License

This project is licensed under the GNU General Public License v3.0. See the LICENSE file for more details.

---

## Star History

[![Star History Chart](https://api.star-history.com/svg?repos=enzomtpYT/HeartRateHook&type=Date)](https://star-history.com/#enzomtpYT/HeartRateHook&Date)
