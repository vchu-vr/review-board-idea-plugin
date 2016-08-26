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

import com.intellij.openapi.ui.Splitter;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.sun.istack.internal.NotNull;
import com.visiercorp.idea.rbplugin.reviewboard.Review;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.EventListener;
import java.util.List;

/**
 * @author Ritesh
 */
public class CommentsListViewPanel<T> extends JPanel {
    private final JBList commentList = new JBList();
    private final ListCellRenderer listCellRenderer;
    private final JCheckBox openIssue = new JCheckBox("Open issue");

    private CommentListener listener;

    @SuppressWarnings("unchecked")
    public CommentsListViewPanel(@NotNull List<T> list, ListCellRenderer listCellRenderer, String selectedLines) {
        this.listCellRenderer = listCellRenderer;
        DefaultListModel listModel = new DefaultListModel();
        if (list != null) {
            for (T t : list) {
                listModel.addElement(t);
            }
        }
        commentList.setModel(listModel);
        initUI(selectedLines);
    }

    public void setListener(CommentListener<T> listener) {
        this.listener = listener;
    }

    private void initUI(String selectedLines) {
        final JBScrollPane scrollPane = new JBScrollPane(commentList);
        Splitter splitter = new Splitter(true);
        splitter.setProportion(0.7f);
        splitter.setFirstComponent(scrollPane);

        JPanel newCommentPanel = new JPanel();
        newCommentPanel.setLayout(new BoxLayout(newCommentPanel, BoxLayout.PAGE_AXIS));
        newCommentPanel.add(openIssue);
        final EditorTextField commentEditor = new EditorTextField();
        newCommentPanel.add(commentEditor);
        splitter.setSecondComponent(newCommentPanel);

        commentEditor.setOneLineMode(false);
        commentEditor.setPlaceholder("Add a comment/reply on " + selectedLines);
        commentList.setBackground(new JBColor(new Color(0, 0, 0, 0), new Color(0, 0, 0, 0)));
        commentList.setCellRenderer(listCellRenderer);
        commentList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1 && commentList.getSelectedIndex() > -1) {
                    Review.File.Comment selectedComment = (Review.File.Comment) commentList.getModel().getElementAt(commentList.getSelectedIndex());
                    if (selectedComment.id == null || !commentEditor.getText().isEmpty()) {
                        showCommentOptionMenu(e, selectedComment, commentEditor.getText());
                    }
                }
            }
        });
        commentEditor.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).
                put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), "postComment");
        commentEditor.getActionMap().put("postComment", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                CommentsListViewPanel.this.listener.onAdd(commentEditor.getText(), openIssue.isSelected());
            }
        });

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(300, 200));
        add(splitter);
    }

    private void showCommentOptionMenu(MouseEvent e, Review.File.Comment comment, String newCommentString) {
        boolean showDeleteOption = comment.id == null;
        boolean showReplyOption = !newCommentString.isEmpty() && (!comment.replyToCommentId.equals("null") || comment.id != null);
        final CommentsOptionMenu<Review.File.Comment> commentsOptionMenu = new CommentsOptionMenu(showDeleteOption, showReplyOption);

        commentsOptionMenu.setListener(new CommentsOptionMenu.DeleteCommentListener<Review.File.Comment>() {
            @Override
            public void onDelete() {
                CommentsListViewPanel.this.listener.onDelete(comment);
            }

            @Override
            public void onReply() {
                CommentsListViewPanel.this.listener.onReply(comment, newCommentString);
            }
        });
        commentsOptionMenu.show(e.getComponent(), e.getX(), e.getY());
    }

    public interface CommentListener<T> extends EventListener {
        void onAdd(String value, boolean issueOpened);
        void onReply(T replyToThread, String value);
        void onDelete(T value);
    }
}
