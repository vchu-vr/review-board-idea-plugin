# Reviewboard Plugin for Intellij


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
Download this plugin from your IDE (Reviewboard Plugin)

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
* Open Libraries and create libraries for hg4idea
    * These will be found in \<IDEA dir\>/plugins/hg4idea/lib/
    * You can add them to the module on creation
* Click ok


Contributing authors
-------------
Jessica Hoang (https://github.com/AtelierRadius)
Andrew Li (https://github.com/andrewkcli)
Base plugin by open source author Ritesh Kapoor