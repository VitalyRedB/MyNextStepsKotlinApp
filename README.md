# MyNextStepsKotlinApp

## 🚶‍♂️ "My Next Steps" - Step Counter App

This is a step counter application for Android, developed using Kotlin. It allows the user to track daily steps, measure the number of steps for a specific period, and save a history of steps for the last 10 days. The project is designed for future integration with a web platform to unlock more advanced features.

## ✨ Features

- **Real-time Step Counting**: Utilizes the hardware step counter sensor (`Sensor.TYPE_STEP_COUNTER`) for accurate daily step tracking.
- **Step History**: Stores and displays a history of steps for the last 10 days.
- **Segment Measurement**: Provides a "Reset" function to measure the number of steps and time spent on a specific route.
- **Data Visualization**: Presents step history graphically for a clear view of progress.
- **Reliable Data Storage**: Uses `SharedPreferences` to persistently store data on steps, history, and measurement results, even after the app is closed.

## 🛠️ Technologies Used

- **Language**: Kotlin
- **Framework**: Android SDK
- **Sensors**: `Sensor.TYPE_STEP_COUNTER`
- **Data Persistence**: `SharedPreferences`
- **Animation**: [ValueAnimator](https://developer.android.com/reference/android/animation/ValueAnimator) for smooth graph rendering
- **Graphics Libraries**: [Glide](https://github.com/bumptech/glide) for loading GIF animations

## 🚀 Installation & Usage

1.  Clone the repository:
    ```bash
    git clone [https://github.com/VitalyRedB/MyNextStepsKotlinApp.git](https://github.com/VitalyRedB/MyNextStepsKotlinApp.git)
    ```
2.  Open the project in Android Studio.
3.  Connect a physical Android device or launch an emulator.
4.  Click the "Run" button in Android Studio.

## 🧑‍💻 How It Works

The app uses a `SensorEventListener` to continuously receive data from the step counter sensor. To correctly track steps for the day and for specific segments, the app records the initial sensor value at the beginning of the day or a new measurement. The step history is updated upon a new day, using `SharedPreferences` as a robust storage solution.

## 📝 Future Plans

- **GPS Integration**: Adding a GPS module to track the user's geographical route.
- **Map Visualization**: Drawing the route on a map (e.g., with the Google Maps API) directly within the app.
- **Website Integration**: Creating a web service to synchronize user data (steps, routes).
- **Multi-user Platform**: Expanding the project to support new user registrations, saving their movement history, and creating social features.
- **Notifications**: Adding notifications for achieving step goals.
- **Advanced Statistics**: Displaying average speed, distance traveled, and other metrics.
- **Material Design 3 Support**.
