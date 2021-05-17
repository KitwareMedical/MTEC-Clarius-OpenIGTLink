# MTEC-Clarius-OpenIGTLink

## How to install

Copy the =ClariusStream.apk= file to the Android tablet and install it.

## How to use ClariusStream

On the tablet containing the Clarius App:
1. Install the ClariusStream.apk application
2. Open the Clarius app and begin an imaging session with the Clarius probe.
3. Close the Clarius app (imaging should still be running), and run the ClariusStream app.
4. Click on "Start Service".
5. Obtain the local IP address of the tablet, which you can see by finding the clarius stream notification.

On the computer:
1. Install 3D Slicer
2. Install the OpenIGTLink extension
3. Navigate to "Modules" > "OpenIGTLinkIF"
4. Create a new node via the "+" button, and set it to "client"
5. Enter in the tablet IP, and the port is 18944.
6. Check the button "Active", and it should begin receiving images.

## How to build

1. Install and open Android Studio, and open the ClariusStream project.
2. Press `Ctrl` twice to open the "Run Anywhere" dialog, and type "gradle app:externalNativeBuildRelease"
  - If you want the debug version, run "gradle app:externalNativeBuildDebug"
3. Click on "Build" in the top bar, and run "Make".

### How to run on Android

1. Enable adb debugging on the device
2. Connect the device to the computer, and allow computer access.
3. On the toolbar, middle-right, select your device from the second drop-down menu. Then, click the Run arrow button nearby to run the app.
