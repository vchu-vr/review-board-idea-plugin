// Copyright (c) [2010-2016] Visier Solutions Inc. All rights reserved.

package com.visiercorp.idea.rbplugin.ui.diff;

import com.visiercorp.idea.rbplugin.ui.Icons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Created by jhoang on 29/07/2016.
 */
public class OpenIssuesGutterIconRenderer extends CommentGutterIconRenderer {
    @NotNull
    @Override
    public Icon getIcon() {
        return Icons.OpenIssueIcon;
    }
}