# Nearby Chat

A simple Android app that allows you to chat with people nearby. It uses the Google Nearby Messages
API to discover and communicate with nearby devices.
> This app is a course project for HKU COMP7506 Smart phone apps development.

## ⚙️Features

- Send and receive messages to nearby devices, OFFLINE is ok. No chat history is stored.
- Automatically discover nearby devices / advertise your device
- Automatically connect to nearby devices
- Text/image/file/voice messages are supported

## 🛠️Build and Run

### Github Actions
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
  
4. Run the project on your Android device  
  
## ⚠️Important Notes

- **GMS service is required**, Huawei devices are not supported.
- **For testing the app: Android Virtual Devices (AVDs)** may not work properly: it could discover
  the virtual devices but cannot establish a connection, seems the AVDs cannot emulate a proper
  bluetooth connection. *You should use physical devices to test the app if AVDs are not working.*
- This app is only tested on Android 14 devices, you should check the permissions if it is not
  working on your device. Also, different ROM may have different behaviors on specific
  permissions `e.g. NEARBY_WIFI_DEVICES`.
- This app is not optimized for performance or code cleanliness, it is a course project for the
  Nearby Connections API.
- `file send` is implemented but not tested, it may not work properly, I do not have enough physical
  devices to test it now.