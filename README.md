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

## Time Break Features

This app helps you maintain a healthy balance in your digital life by enforcing regular breaks based on your app usage patterns.

### Key Features

1. **Smart App Monitoring**

   - Automatically detects which app is in the foreground
   - Classifies apps as either "Entertainment" or "Productivity"
   - Countdown for each app session

2. **Customizable Break Schedules**

   - Set different work durations for entertainment apps (default: 30 minutes)
   - Set different work durations for productivity apps (default: 60 minutes)
   - Customize break durations for each app type (default: 10 minutes for entertainment, 30 minutes for productivity)

3. **Gentle Break Enforcement**

   - Displays countdown timer showing time until next break
   - Shows notifications when break time is reached
   - Persistent notification during break period with remaining time

4. **Background Monitoring**
   - Continues tracking even when our app is minimized
   - Uses Android's foreground service for reliable operation

### How to Use

1. Navigate to the "Section 2" section from the main menu
2. Press "Start" to begin monitoring your app usage
3. Use your device normally - the app works in the background
4. When it's time for a break, you'll receive a notification
5. Take your break! The app will notify you when break time is over

### Customizing Settings

1. Navigate to the "Time Balance" section
2. Select the "Page 2" tab at the bottom
3. Adjust the work and break durations for both app categories
4. Press "Save" to apply your new settings

Note: Settings cannot be changed while monitoring is active. Stop monitoring first, then adjust settings.
