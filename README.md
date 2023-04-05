# Quine Camera
Quine Camera is an Android application that uses the device camera to stream MJPEG video over HTTP. It can also capture snapshots from the camera and display them as JPEG images. The app is built using the CameraX API and NanoHTTPD server library.

## Features
- Stream video from the device camera over HTTP
- Capture snapshots from the camera and display them as JPEG images
- Support for MJPEG video format
- Simple and easy to use interface
## Installation
1. Clone this repository: 
```
git clone https://github.com/shaunmulligan/quine-camera.git
```
2. Open the project in Android Studio
3. Run the app on an Android device or emulator

## Usage
When you open the app, you will see a viewfinder that displays the camera output. You can stream the video to your Mainsail UI buy adding a new camera. 
1. Click on the interface settings cogs at the top right of Mainsail
2. find "Webcams" in the menu and click "ADD WEBCAM".
3. Give the webcam a name and in the `service` drop down select `Adaptive MJPEG-Streamer (experimental)`
4. In the `URL stream` field past `http://<PHONE_IP_ADDRESS>:8080/webcam/?action=stream`, with your correct local phone IP address.
5. Do the same as #4 for `URL snapshot` field but change the end of the url to `?action=snapshot`.
6. Click `SAVE WEBCAM`

![Alt text](images/mainsail-quine-setup.png.jpg?raw=true "Mainsail Webcam Setup")

## Limitations
Currently the camera will only stream to the Mainsail UI when the app is running in the foreground of the phone and you can see the video stream on the phones screen. This should be fixed soon.
