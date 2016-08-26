// Copyright (c) [2010-2016] Visier Solutions Inc. All rights reserved.

//
// Copyright © [2010-2016] Visier Solutions Inc. All rights reserved.
//
package com.visiercorp.idea.rbplugin.ui;


import com.visiercorp.idea.rbplugin.exception.InvalidConfigurationException;
import com.visiercorp.idea.rbplugin.exception.InvalidCredentialException;
import com.visiercorp.idea.rbplugin.exception.ReviewBoardServerException;
import com.visiercorp.idea.rbplugin.messages.PluginBundle;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vcs.VcsException;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.apache.commons.lang.StringUtils.defaultString;

/**
 * @author ritesh
 */
public class ExceptionHandler {
    private static final Logger LOG = Logger.getInstance(ExceptionHandler.class);

    public static class Message {
        public String message;
        public Type type;

        public Message(String message, Type type) {
            this.message = message;
            this.type = type;
        }

        public enum Type {
            ERROR, WARNING
        }
    }

    public static Message getMessage(Exception exception) {
        if (exception instanceof InvalidCredentialException || exception instanceof InvalidConfigurationException) {
            LOG.warn(exception.getMessage());
            return new Message(defaultString(exception.getMessage()), Message.Type.WARNING);
        } else if (exception instanceof ReviewBoardServerException) {
            LOG.warn(exception.getMessage());
            return new Message(exception.getMessage(), Message.Type.WARNING);
        } else if (exception instanceof URISyntaxException || exception instanceof IOException) {
            LOG.warn(exception.getMessage());
            return new Message(PluginBundle.message(PluginBundle.CONNECTION_ERROR_MSG), Message.Type.WARNING);
        } else if (exception instanceof VcsException) {
            LOG.warn(exception.getMessage());
            return new Message(exception.getMessage(), Message.Type.WARNING);
        } else {
            LOG.error(exception);
            return new Message(defaultString(exception.getMessage()), Message.Type.ERROR);
        }

    }

    public static void handleException(Exception exception) {
        final Message message = getMessage(exception);
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                Notifications.Bus.notify(new Notification("ReviewBoard", PluginBundle.message(PluginBundle.NOTIFICATION_TITLE),
                        message.message, message.type == Message.Type.ERROR ? NotificationType.ERROR : NotificationType.WARNING));
            }
        });
    }
}
