// Copyright (c) [2010-2016] Visier Solutions Inc. All rights reserved.

/*
 * Copyright 2015 Ritesh Kapoor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.visiercorp.idea.rbplugin.ui.panels;

import com.visiercorp.idea.rbplugin.ui.Icons;
import com.intellij.icons.AllIcons;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.visiercorp.idea.rbplugin.reviewboard.Review;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.SimpleDateFormat;

/**
 * @author Ritesh
 */
public class CommentPanel {
    private JBTextField comment;
    private JBLabel username;
    private JBLabel timestamp;
    private JPanel pane;

    public CommentPanel(Review.File.Comment comment) {
        String onSelectedLines = (comment.numberOfLines == 1) ?
                " on line " + comment.firstLine :
                " on lines " + comment.firstLine + " to " + (comment.firstLine+comment.numberOfLines-1);

        this.username.setText(comment.user);
        this.username.setIconTextGap(1);
        this.comment.setText(comment.text);
        if (comment.timestamp != null) {
            if (comment.replyToCommentId.equals("null")) {
                this.timestamp.setText(new SimpleDateFormat("yy-MM-dd HH:mm").format(comment.timestamp) + onSelectedLines);
            } else {
                this.timestamp.setText(new SimpleDateFormat("yy-MM-dd HH:mm").format(comment.timestamp));
            }
            if (comment.issueStatus.equals("open")) {
                this.username.setIcon(Icons.OpenIssueIcon);
            } else {
                this.username.setIcon(AllIcons.General.Balloon);
            }
        } else {
            this.username.setIcon(Icons.NewCommentIcon);
            if (comment.issueOpened) {
                this.timestamp.setText("New Issue" + onSelectedLines);
            } else if (comment.replyToCommentId.equals("null")){
                this.timestamp.setText("New Comment" + onSelectedLines);
            }
        }

        if (!comment.replyToCommentId.equals("null")) {
            this.pane.setBorder(new EmptyBorder(0, 20, 0, 0));
            if (comment.timestamp != null) {
                this.timestamp.setText(new SimpleDateFormat("yy-MM-dd HH:mm").format(comment.timestamp));
            }
        }

        this.comment.setBackground(new JBColor(new Color(0, 0, 0, 0), new Color(0, 0, 0, 0)));
        this.comment.setBorder(BorderFactory.createEmptyBorder());
    }

    public JComponent getPanel() {
        return pane;
    }
}
