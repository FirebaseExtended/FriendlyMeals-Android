# FriendlyMeals – Firebase Android Samples

Welcome to **FriendlyMeals**, an app that demonstrates how to integrate Firebase features into an Android app.

<img width="818" height="524" alt="hero-art" src="https://github.com/user-attachments/assets/eebe4219-0605-460d-985f-69358b727ed2" />

## 📺 Video Series

- [Firebase Fundamentals on YouTube](https://www.youtube.com/playlist?list=PLl-K7zZEsYLnfwBe4WgEw9ao0J0N1LYDR)

## 🚀 Features

- **Firebase AI Logic**: Learn how build AI-powered features to enhance your app's functionality.
- **Firestore Enterprise edition**: Learn how to store data in Firestore Enterprise edition and build Pipeline queries.
- **Firebase Authentication**: Learn how to keep your user's data safe with Firebase Authentication.
- **Cloud Storage**: Learn how to store images in Cloud Storage, and how to display them in a Jetpack Compose UI.
- **Remote Config**: Learn how to change the behavior and appearance of your app remotely with Remote Config.
- **App Check**: Learn how to protect your app backend from abuse with App Check.
- **Upcoming**: More Firebase features will be added soon!

## 🛠️ Getting Started

1. **Clone this repository**
   ```sh
   git clone https://github.com/FirebaseExtended/FriendlyMeals-Android.git
   ```
1. **Open the project**
   - Open `FriendlyMeals-Android` in Android Studio.

## 🔥 Setting Up Firebase

1. **Create a Firebase Project**
   - Sign into the [Firebase console](https://console.firebase.google.com/).
   - Click the button to create a new Firebase project, and follow the on-screen workflow.
   - Enable the products listed in the 'Features' section by navigating to the corresponding area of the console.

1. **Register the Android App**
   -  Click **Add app** > **Android**.
   -  Follow the on-screen workflow.  You can enter the package name: `com.google.firebase.example.friendlymeals`
   -  Click **Register app**.

1. During the "Add app" workflow, **download the `google-services.json` file**.
   - Move this file into this directory of the app: `FriendlyMeals-Android/app`.
   - If you didn't download this file during the "Add app" workflow, you can always [obtain it later](https://support.google.com/firebase/answer/7015592).
  
1. **Enable Firebase services in the Firebase console**
     - Go to the [**Firebase Authentication**](https://console.firebase.google.com/project/_/authentication/?useAutoProject=true) section of the console, enable the service and choose the Anonymous authentication method.
     - Go to the [**Firestore**](https://console.firebase.google.com/project/_/firestore/?useAutoProject=true) section of the console and enable Firestore Enterprise edition.
     - Go to the [**Firebase AI Logic**](https://console.firebase.google.com/project/_/ailogic/?useAutoProject=true) section of the console and enable the Gemini Developer API. You will also need to go to the [Prompt templates tab](https://console.firebase.google.com/project/_/ailogic/templates/?useAutoProject=true) and create the prompt templates that will be fetched by the Android app. You can find the Prompt templates [inside the prompts folder](https://github.com/FirebaseExtended/FriendlyMeals-Android/tree/main/prompts).
     - Go to the [**App Check**](https://console.firebase.google.com/project/_/appcheck/?useAutoProject=true) section of the console, enable this service, and register your Android app using the Play Integrity provider (you'll need to provide the SHA-256 fingerprint of your app's signing certificate).
     - Go to the [**Remote Config**](https://console.firebase.google.com/project/_/config/?useAutoProject=true) section of the console and enable this service.
     - Go to the [**Firebase Storage**](https://console.firebase.google.com/project/_/storage/?useAutoProject=true) section of the console and enable this service (you'll need to set up a billing account since this service is only available on the Blaze plan).

1. **Run the app**
   - Build and run in Android Studio on an Android emulator or physical device.
  
1. **Set up App Check**
     - Once your app is running on the emulator, look for the App Check debug token in your logs, and copy it.
     - Then, in the Firebase console, go to the [Security > App Check > Apps tab](https://console.firebase.google.com/project/_/appcheck/apps/?useAutoProject=true) and locate your Android app.
     - Click the three-dot menu and select **Manage debug tokens**.
     - Paste your token in the "value" field and save it.

The log will be similar to this:

     DebugAppCheckProvider: Enter this debug secret into the allow list
     in the Firebase Console for your project: 123a4567-b89c-12d3-e456-789012345678

## 🤝 Contributing

Contributions and suggestions are welcome! Feel free to open issues or pull requests as you follow along with the series.

## 📄 License

This project is licensed under the [Apache License](../../LICENSE), which can be found in the root of this repository. It is provided for educational purposes as part of the Firebase Fundamentals video series.
