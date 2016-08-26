// Copyright (c) [2010-2016] Visier Solutions Inc. All rights reserved.

//
// Copyright © [2010-2016] Visier Solutions Inc. All rights reserved.
//
package com.visiercorp.idea.rbplugin.messages;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * @author ritesh
 */
public class PluginBundle {

    public static final String CONNECTION_ERROR_MSG = "reviewboard.message.connection.error";
    public static final String LOGIN_SUCCESS_MESSAGE = "reviewboard.message.login.success";
    public static final String CONNECTION_STATUS_TITLE = "reviewboard.message.connection.status.title";
    public static final String CONNECTION_TEST_TITLE = "reviewboard.message.connection.title";
    public static final String NOTIFICATION_TITLE = "reviewboard.notification.title";

    public static final String PATCH_WARNING_NOTIFICATION = "patch.message.warning.notification";
    public static final String PATCH_WARNING_UNKNOWN_REPO = "patch.message.warning.unknownRepository";
    public static final String PATCH_WARNING_PROJECT_MISMATCH = "patch.message.warning.projectMismatch";
    public static final String PATCH_WARNING_OPTIONS = "patch.message.warning.options";
    public static final String PATCH_WARNING_TITLE_UNKNOWN_REPO = "patch.notification.title.noVCS";
    public static final String PATCH_WARNING_TITLE_PROJECT_MISMATCH = "patch.notification.title.wrongProject";


    public static String message(@NotNull String key, @NotNull Object... params) {
        return getBundle().getString(key);
    }

    private static Reference<ResourceBundle> bundle;
    @NonNls
    public static final String BUNDLE = "com.visiercorp.idea.rbplugin.resources.reviewboard";

    private PluginBundle() {
    }

    private static ResourceBundle getBundle() {
        if (bundle == null || bundle.get() == null) {
//            try {InputStream inputStream = ClassLoader.getSystemResourceAsStream(BUNDLE + ".properties");}
//            catch (Exception e) {
//                e.printStackTrace();
//            }
            bundle = new SoftReference<>(ResourceBundle.getBundle(BUNDLE));
        }
        return bundle.get();
    }
}
