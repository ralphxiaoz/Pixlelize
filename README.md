# This is a failed project as of 12/22/2024. The camera feed is not working.

# Pixelize

Pixelize is an Android application that allows users to apply pixelation effects to their camera feed in real-time. The app utilizes OpenGL for rendering and provides a simple user interface for capturing images and adjusting pixelation settings.

## Features

- Real-time camera feed with pixelation effects.
- Adjustable pixel size for the pixelation effect.
- Simple and intuitive user interface.
- Capture and save images with applied effects.

## Requirements

- Android Studio
- Android SDK
- Minimum SDK version: 21 (Lollipop)

## Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/pixelize.git
   cd pixelize
   ```

2. Open the project in Android Studio.

3. Ensure you have the necessary SDKs installed.

4. Build and run the application on an Android device or emulator.

## Usage

1. Launch the Pixelize app.
2. Grant camera permissions when prompted.
3. Use the "Pixelation" toggle to enable or disable the pixelation effect.
4. Adjust the pixel size using the SeekBar.
5. Tap the "Take Picture" button to capture the current frame with the applied effect.

## Code Structure

- `MainActivity.java`: The main activity that handles the camera setup and user interactions.
- `CustomGLSurfaceView.java`: A custom GLSurfaceView for rendering the camera feed and effects.
- `CustomRenderer.java`: The OpenGL renderer that processes the camera feed and applies pixelation effects.
- `activity_main.xml`: The layout file for the main activity.
- `build.gradle.kts`: The Gradle build file for managing dependencies and project configuration.

## Contributing

Contributions are welcome! If you have suggestions for improvements or new features, feel free to open an issue or submit a pull request.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [OpenGL ES](https://www.khronos.org/opengles/) for rendering graphics.
- [Android Camera2 API](https://developer.android.com/reference/android/hardware/camera2/package-summary) for camera functionality.
