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
2. In the main menu, open **File | Settings for Windows**, or **IntelliJ IDEA | Preferences...** on the Mac OS X 
3. Select **Plugins** from the left panel
4. Select **“Install plugin from disk…”**
5. In the file navigator, find and select the downloaded ReviewBoardPlugin.zip archive file
6. Make sure the Review Board Plugin has its check-box selected
7. Restart IntelliJ for the changes to take effect


How to use it?
-------------
1. Open the review board plugin from the bottom toolbar
2. Configure the settings via the Settings button (third button from the left)
3. Refresh the plugin via the refresh button (first button from the left)

Comment Icons: 
![alt text](https://github.com/vchu-vr/review-board-idea-plugin/blob/master/resources/balloon.png "Blue Comment Icon") = published general comments
![alt text](https://github.com/vchu-vr/review-board-idea-plugin/blob/master/resources/com/visiercorp/idea/rbplugin/resources/yellow_balloon.png "Blue Comment Icon") = published open issues
![alt text](https://github.com/vchu-vr/review-board-idea-plugin/blob/master/resources/com/visiercorp/idea/rbplugin/resources/magenta_balloon.png "Magenta Comment Icon") = new comments/open issues/replies

Adding comments:
>To add a comment to a single line, simply click on the line number you wish to comment on in the right line marker panel to display the comments dialogue. 
>- or -
>To add a comment to a section of code, highlight multiple lines of code in the right diff editor and click anywhere on the right line marker panel to reveal the comments dialogue. 
>
>Check off the “Open issue” checkbox to create an open issue. This does not apply for replies.
>Add a comment by typing into the comment editor box and hitting Ctrl+Enter. 
>
>Click a comment to reveal options to add a reply to the selected thread (must compose the comment reply first) or to delete this selected draft comment/reply. Important Note: A reply cannot be added to a new (pre-draft) comment thread. While replying to the same comment thread multiple times in one session is supported by the plugin, they are published separately which causes a lot of email noise to the target people and groups. 
>
>The status of open issues cannot be set to resolved or dropped through the plugin. You can quickly open the selected review in a browser via the browser button.
>
>Adding comments and open issues with the plugin will be temporarily cache the new additions in an *pre-draft* state. These are **not** persisted in the review board server as draft nor published. Only by publishing the review/replies are these new comments then added to the server as draft and finally published once the execution completes. 

Applying a patch:
>Code navigation in the file diffs is not possible. By applying the patch, you can test the code locally and gain access to the code navigation feature of IntelliJ.
>
>The plugin will prevent you from applying a patch if it detects local changes in your Mercurial project. Also, patches cannot be applied to files outside of the project root.
>
>Select a review in the review list to see the latest file diffs. 
>Optionally, select the revision of the patch you wish to apply via the Revision dropdown menu.
>Click the Patch icon above the changed files list panel (third from the left).
>Refer to the Steps 3 and beyond in [IntelliJ Help](https://www.jetbrains.com/help/idea/2016.2/applying-patches.html) to finish applying the patch.

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