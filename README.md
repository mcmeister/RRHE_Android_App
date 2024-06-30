# 🌿 RRHE Android App

RRHE is an Android application designed to manage and display information about plants. It utilizes Jetpack Compose for modern UI development, Retrofit for networking, and Glide for image loading.

## Features

- **Jetpack Compose**: Used for modern and efficient UI development.
- **Retrofit**: For network requests to fetch plant data.
- **Glide**: For efficient image loading and caching.
- **View Binding**: To simplify interactions with UI components.
- **Parcelize**: To easily pass data between activities.

# 🚀 Getting Started

## Prerequisites

- **Android Studio** (latest version recommended)
- **Android SDK** (minimum SDK version 24)
- **Kotlin 1.8 or later**

## Installation

- **Clone the repository:**
- **git clone https://github.com/mcmeister/RRHE_Android_App.git**
- **cd RRHE_Android_App**

# Open the project in Android Studio:

- **File -> Open -> Select the project directory**

# Build the project:

- **Click on the Build menu and select Make Project**

# Run the project:

- **Select an emulator or a physical device and click the Run button.**

# 📱 Usage

- **Main Screen:** Displays a list of plants fetched from the server.
- **Search Functionality:** Allows searching plants by name or stock ID.
- **Plant Details:** View detailed information about a specific plant.
- **Edit Plant:** Edit and update plant details.

# 🛠️ Code Overview

## Key Classes and Files

- **Main Activity:** StockActivity.kt
- **Detail Activity:** PlantDetailsActivity.kt
- **Edit Activity:** EditPlantActivity.kt

## Network Configuration:

- **ApiConfig.kt:** Contains base URL for API
- **ApiClient.kt:** Sets up Retrofit client
- **ApiService.kt:** Defines API endpoints
- **Image Loading:** Glide is configured in MyAppGlideModule.kt

## UI Components:

- **Color.kt, Theme.kt, Type.kt:** Define the theme and typography
- **activity_stock.xml, image_popup.xml, item_plant.xml:** Layout files for the UI

## Dependencies:

- **Dependencies are managed in build.gradle.kts.** 

## Key dependencies include:

- **Retrofit**
- **Glide**
- **Jetpack Compose**
- **Material Components**
- **RecyclerView**
- **AndroidX libraries for lifecycle and view binding**
- **Proguard:** Proguard is configured to optimize and obfuscate the release builds. See proguard-rules.pro for custom rules.

# 🤝 Contributing

- **Fork the repository.**
- **Create a new branch:** git checkout -b feature/your-feature-name
- **Commit your changes:** git commit -m 'Add some feature'
- **Push to the branch:** git push origin feature/your-feature-name
- **Open a pull request.**

# 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.
