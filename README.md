# Nearby Chat

A simple Android app that allows you to chat with people nearby. It uses the Google Nearby Connections
API to discover and communicate with nearby devices.
> This app is a course project for HKU COMP7506 Smart phone apps development.

## ⚙️Features

- Send and receive messages to nearby devices, OFFLINE is ok. No chat history is stored.
- Automatically discover nearby devices / advertise your device
- Automatically connect to nearby devices
- Text/image/file/voice messages are supported

## 🛠️Build and Run

### Github Actions ![workflow badge](https://github.com/liuweikarlie/CHAT/actions/workflows/android.yml/badge.svg)
> We deployed the Android CI pipeline on Github Actions, you can check the latest build status on  
> the [Actions page](https://github.com/liuweikarlie/CHAT/actions).  
> The latest APK file is available in the actions artifacts.  
  
Go to Actions -> click the latest workflow -> scroll to the artifacts -> download the APK file.
  
### Build Locally

1. Clone the repository  
  
```shell
git clone https://github.com/liuweikarlie/CHAT.git
```
  
2. Open the project in Android Studio  
3. Build / Run the project in Android Studio, or run the following command in the terminal (change
   directory to the project root)  
  
```shell
./gradlew build
```
### Run the app
You may need multiple devices to run the application; you can distribute the APK file to the devices or use `adb` to connect multiple devices and test them in Android Studio.
**Make sure you have read the [Important Notes](https://github.com/liuweikarlie/CHAT?tab=readme-ov-file#%EF%B8%8Fimportant-notes) in the readme file**

For `adb`, you might need the following commands for your testing:
```shell
adb devices #list the connected devices
adb shell pm grant <PACKAGE_NAME> android.permissions.<PERMISSION_NAME> # grant the permission through adb, of course, you can grant permissions by the popup dialog on your devices.
```
Once the app is launched, it will automatically start advertising/discovering nearby devices (the app should also be active on those devices).

  
## ⚠️Important Notes

- **GMS service is required**, Huawei devices are not supported.
- **For testing the app: Android Virtual Devices (AVDs) may not work properly**: it could discover
  the virtual devices but cannot establish a connection; it seems the AVDs cannot emulate a proper
  Bluetooth connection. *You should use physical devices to test the app if AVDs are not working.*
- This app is only tested on Android 14 devices, so you should check the permissions to see if it is not
  working on your device. Also, different ROMs may have different behaviors on specific
  permissions `e.g., NEARBY_WIFI_DEVICES`.
- This app is not optimized for performance or code cleanliness; it is a course project for the
  Nearby Connections API.
- `file send` is implemented but not tested; it may not work properly; I do not have enough physical
  devices to test it now.
