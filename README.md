# Reviewboard Plugin for Intellij
- [Download plugin .zip archive file](https://github.com/vchu-vr/review-board-idea-plugin/blob/master/review-board-idea-plugin.zip)

Description
-------------
This plugin integrates Reviewboard in Intellij for code reviews. 
This plugin tries to ease interaction with ReviewBoard server from the IntelliJ IDE.

Features
-------------
* Do reviews directly from your IDE
* View all, pending or submitted reviews
* Compare (Diff) changes in review locally
* Submit changes to the reviewboard server
* Navigate to different versions of the selected review
* Comment on reviews
    * Usage: Add new comments to a line in file diff window with click on line marker
    * Usage: Add new comments to mutliple lines in file diff window by highlighting lines and clicking line marker
    * Usage: Add replies to existing comment/issue threads by entering in response and clicking thread to reply to
* Submit/Discard Reviews

Limitations
-------------
* Viewing multiple reviews is not supported
* Updating diff is not supported

Plugin Compatibility
-------------
This plugin was built with JDK 1.8 and idea 16 version.

How to install it?
-------------
1. Download the [ReviewBoardPlugin.zip archive file](https://github.com/vchu-vr/review-board-idea-plugin/blob/master/review-board-idea-plugin.zip)
2. Install the plugin
- In the main menu, open **File | Settings for Windows**, or **IntelliJ IDEA | Preferences...** on the Mac OS X 
- Select **Plugins** from the left panel
- Select **“Install plugin from disk…”**
- In the file navigator, find and select the downloaded ReviewBoardPlugin.zip archive file
- Make sure the Review Board Plugin has its check-box selected
- Restart IntelliJ for the changes to take effect


Project Setup
-------------
Required Plugins:
* Hg Integration

JDK: 1.8

You'll need to setup the appropriate SDK. IntelliJ SDK and plugin dependencies are required to be setup.

This was developed against version 16

* Go to File -> Project Structure
* Click on SDKs
* Click on plus icon at top of second pane -> IntelliJ IDEA Plugin SDK
* Browse to home of IntelliJ IDEA 16
* It should be named 'IDEA-IU-XXXX'
* Open Libraries and create library for hg4idea
    * This will be found in \<IDEA dir\>/plugins/hg4idea/lib/
    * You can add them to the module on creation
* Click OK

Building the Plugin
-------------
To build the plugin module, in the main menu select **Build | Make Project**, and then **Build | Prepare plugin module 'review-board-idea-plugin' For Deployment**.
The .zip artifact will be generated in the folder specified in the balloon or events log.

Contributing authors
-------------
* [Jessica Hoang](https://github.com/AtelierRadius)
* [Andrew Li](https://github.com/andrewkcli)
* Base plugin by open source author Ritesh Kapoor