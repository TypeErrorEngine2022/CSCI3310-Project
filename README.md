# CSCI3310 Project

## Prerequisites
1. Download the `gemma3-1b-it-int4.task` model (555 MB) from [litert-community/Gemma3-1B-IT](https://huggingface.co/litert-community/Gemma3-1B-IT/tree/main).
2. Place the model in the `app/src/main/assets/models/` folder. Create the `assets/models/` subdirectories if they don’t exist.

## ScreenTime Tracking Features
1. **Track the time spent on each app today**
   - refreshes automatically on app resume.
   - using ViewHolder + recyclerView to show the list of apps efficiently
   - using ViewModel to ensure the data survives configuration changes like screen rotation
   - using DiffUtil to efficiently update the list of apps, only the changed items will be updated
2. **Offline built-in AI**
   - utilizes the latest Gemma3-1B-IT model, which is a lightweight (555MB) and efficient model
   - requires no internet connection, ensuring privacy and security
3. **Background AI classification of apps into `PRODUCTIVE` and `NON_PRODUCTIVE`**
   - only classified once and saved in the database
   - user can manually change the productivity label
4. **AI-Generated comment on today's app usage**
   - generation takes approximately 5 seconds
   - persist across app restarts, so the user can view the comment later
   - cached for 15 minutes, regenerates only on user request or after timeout.

## ScreenTime Tracking Permissions
1. **PACKAGE_USAGE_STATS**: Required to track the time spent on each app 
   - if not granted, the app will redirect the user to the settings page to grant the permission
2. **QUERY_ALL_PACKAGES**: Required to get the app name and icon
   - will be automatically granted on launch