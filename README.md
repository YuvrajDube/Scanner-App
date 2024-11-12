# Scanner-App
This is a QR Code Scanner App built with Kotlin and Jetpack Compose. It leverages CameraX for real-time camera preview and ML Kit's Barcode Scanning API for detecting and reading QR codes from the camera feed.

App Functionality:-
1) Real-Time QR Code Scanning: The app continuously scans the camera feed for QR codes. When a QR code is detected, the data is displayed or
   opened in Chrome if it’s a URL.
3) Clipboard Integration: If the scanned data isn’t a URL, the app copies it directly to the clipboard, providing an easy way for users to
   access or share the QR code content.
5) Chrome Redirection: When a QR code contains a URL, the app launches Chrome (or the default browser if Chrome isn’t installed) to open
   the link directly.
