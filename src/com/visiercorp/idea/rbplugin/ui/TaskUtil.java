// Copyright (c) [2010-2016] Visier Solutions Inc. All rights reserved.

//
// Copyright © [2010-2016] Visier Solutions Inc. All rights reserved.
//
package com.visiercorp.idea.rbplugin.ui;

import com.visiercorp.idea.rbplugin.util.ThrowableFunction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author ritesh
 */
public class TaskUtil {
    public static <T> void queueTask(@NotNull Project project, @NotNull String title,
                                     boolean isModal,
                                     @NotNull final ThrowableFunction<ProgressIndicator, T> task,
                                     @Nullable final ThrowableFunction<T, Void> success,
                                     @Nullable final Runnable onFailure) {

        if (task == null || title == null) throw new IllegalArgumentException();
        if (isModal) {
            new Task.Modal(project, title, true) {
                @Override
                public void run(final ProgressIndicator progressIndicator) {
                    runTask(progressIndicator, task, success, onFailure);
                }
            }.queue();
        } else {
            new Task.Backgroundable(project, title, true) {
                @Override
                public void run(final ProgressIndicator progressIndicator) {
                    runTask(progressIndicator, task, success, onFailure);
                }
            }.queue();
        }
    }


    private static <T> void runTask(final ProgressIndicator progressIndicator,
                                    @NotNull final ThrowableFunction<ProgressIndicator, T> task,
                                    @Nullable final ThrowableFunction<T, Void> success,
                                    @Nullable final Runnable onFailure) {
        Future<Exception> future = ApplicationManager.getApplication().executeOnPooledThread(new Callable<Exception>() {
            @Override
            public Exception call() throws Exception {
                try {
                    T result = task.throwableCall(progressIndicator);
                    if (success != null) success.throwableCall(result);
                } catch (Exception e) {
                    return e;
                }
                return null;
            }
        });

        while (true) {
            try {
                final Exception exception = future.get(100, TimeUnit.MILLISECONDS);
                if (exception != null) {
                    ExceptionHandler.handleException(exception);
                    if (onFailure != null) onFailure.run();
                }
                return;
            } catch (TimeoutException ignore) {
                try {
                    progressIndicator.checkCanceled();
                } catch (ProcessCanceledException e) {
                    return;
                }
            } catch (Exception e) {
                ExceptionHandler.handleException(e);
                return;
            }
        }
    }
}
