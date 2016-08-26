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

package com.visiercorp.idea.rbplugin.ui.diff;

import com.visiercorp.idea.rbplugin.reviewboard.Review.File.Comment;
import com.visiercorp.idea.rbplugin.ui.panels.CommentPanel;
import com.visiercorp.idea.rbplugin.ui.panels.CommentsListViewPanel;
import com.intellij.openapi.diff.DiffRequest;
import com.intellij.openapi.diff.impl.DiffPanelImpl;
import com.intellij.openapi.diff.impl.DiffUtil;
import com.intellij.openapi.diff.impl.external.FrameDiffTool;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.editor.event.EditorMouseAdapter;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseEventArea;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.editor.markup.*;
import com.intellij.openapi.ui.FrameWrapper;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.JBColor;
import com.visiercorp.idea.rbplugin.reviewboard.Review;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ritesh
 */
public class CommentsDiffTool extends FrameDiffTool {
    private Review.File file;
    private List<Review.File.Comment> comments;
    private List<RangeHighlighter> newCommentHighlighters = new ArrayList<>();
    private ListCellRenderer<Review.File.Comment> listCellRenderer;
    private ActionListener actionListener;
    private FrameWrapper frameWrapper;

    private int popupLineIndex = -1;
    private JBPopup popup;

    public CommentsDiffTool(Review.File file, List<Review.File.Comment> comments) {
        this.file = file;
        this.comments = new ArrayList<>(comments);
        this.listCellRenderer = new ListCellRenderer<Review.File.Comment>() {
            @Override
            public Component getListCellRendererComponent(JList list, final Review.File.Comment value, final int index, boolean isSelected, boolean cellHasFocus) {
                return new CommentPanel(value).getPanel();
            }
        };
    }

    @Override
    public void show(DiffRequest request) {
        frameWrapper = new FrameWrapper(request.getProject(), request.getGroupKey());
        final DiffPanelImpl diffPanel = createDiffPanelImpl(request, frameWrapper.getFrame(), frameWrapper);
        final Editor editor = diffPanel.getEditor2();
        updateHighLights(editor);

        editor.addEditorMouseListener(new EditorMouseAdapter() {
            @Override
            public void mouseClicked(EditorMouseEvent e) {
                if (e.getArea() != null && e.getArea().equals(EditorMouseEventArea.LINE_MARKERS_AREA)) {
                    final int lineNumber = getStartLineNumber(editor, e.getMouseEvent());

                    if (popupLineIndex != lineNumber){
                        if (popup != null && !popup.isDisposed()) {
                            popup.dispose();
                        }
                        popupLineIndex = lineNumber;
                        final int numLinesSelected = getLinesSelected(editor);
                        showCommentsView(e.getMouseEvent(), lineNumber, numLinesSelected, editor);
                    } else {
                        if (popup == null || popup.isDisposed()) {
                            final int numLinesSelected = getLinesSelected(editor);
                            showCommentsView(e.getMouseEvent(), lineNumber, numLinesSelected, editor);
                        } else if (!popup.isVisible() || !popup.isFocused()) {
                            popup.showInScreenCoordinates(e.getMouseEvent().getComponent(), e.getMouseEvent().getLocationOnScreen());
                        }
                    }
                } else if (popup != null && !popup.isDisposed()){
                    popup.dispose();
                    popupLineIndex = -1;
                }
            }
        });

        DiffUtil.initDiffFrame(request.getProject(), frameWrapper, diffPanel, diffPanel.getComponent());
        frameWrapper.setTitle(request.getWindowTitle());
        frameWrapper.show();
    }

    public void setActionListener(ActionListener actionListener) {
        this.actionListener = actionListener;
    }

    private void showCommentsView(MouseEvent mouseEvent, final int lineNumber, final int numLinesSelected, final Editor editor) {
        List<Review.File.Comment> comments = sortCommentsByThread(lineComments(CommentsDiffTool.this.comments).get(lineNumber));

        String selectedLines = (numLinesSelected == 1) ?
                String.format("line %s", lineNumber) :
                String.format("lines %s to %s", lineNumber, lineNumber+numLinesSelected-1);

        final CommentsListViewPanel<Comment> commentsListViewPanel = new CommentsListViewPanel(comments, listCellRenderer, selectedLines);
        popup = JBPopupFactory.getInstance().createComponentPopupBuilder(commentsListViewPanel, null)
                .setTitle(String.format("Comments starting on Line %s", lineNumber))
                .setMovable(true)
                .setRequestFocus(true)
                .setCancelOnWindowDeactivation(true)
                .setCancelOnClickOutside(true)
                .setCancelOnOtherWindowOpen(true)
                .setAdText("Hit Ctrl+Enter to add a new comment/issue, click comment for option to add a new reply or to delete new comment.")
                .setResizable(true)
                .createPopup();
        popup.showInScreenCoordinates(mouseEvent.getComponent(), mouseEvent.getLocationOnScreen());

        commentsListViewPanel.setListener(new CommentsListViewPanel.CommentListener<Review.File.Comment>() {
            @Override
            public void onAdd(String value, boolean issueOpened) {
                if (!value.isEmpty()) {
                    popup.dispose();
                    Review.File.Comment newComment = new Review.File.Comment();
                    newComment.text = value;
                    newComment.firstLine = lineNumber;
                    newComment.numberOfLines = numLinesSelected;
                    newComment.file = file;
                    newComment.replyToCommentId = "null";
                    newComment.issueOpened = issueOpened;
                    CommentsDiffTool.this.comments.add(newComment);
                    updateHighLights(editor);
                    actionListener.actionPerformed(new ActionEvent(this, 0, "Added comment"));
                }
            }

            @Override
            public void onReply(Review.File.Comment replyToComment, String value) {
                popup.dispose();
                Review.File.Comment newComment = new Review.File.Comment();
                newComment.text = value;
                newComment.firstLine = lineNumber;
                newComment.numberOfLines = replyToComment.numberOfLines;
                newComment.file = file;
                newComment.reviewId = replyToComment.reviewId;
                newComment.replyToCommentId = (replyToComment.replyToCommentId.equals("null")) ? replyToComment.id : replyToComment.replyToCommentId ;
                CommentsDiffTool.this.comments.add(newComment);
                updateHighLights(editor);
                actionListener.actionPerformed(new ActionEvent(this, 0, "Added reply"));

            }

            @Override
            public void onDelete(Review.File.Comment value) {
                CommentsDiffTool.this.comments.remove(value);
                updateHighLights(editor);
                popup.dispose();
                popupLineIndex = -1;
                actionListener.actionPerformed(new ActionEvent(this, 0, "Deleted comment/reply"));
            }
        });
    }

    private List<Review.File.Comment> sortCommentsByThread(List<Review.File.Comment> comments) {
        List<Review.File.Comment> sortedComments = new ArrayList<>();
        if (comments != null && !comments.isEmpty()) {
            Map<String, List<Review.File.Comment>> threadsMap = new HashMap<>();
            List<String> threadList = new ArrayList<>();
            List<Review.File.Comment> newThreadList = new ArrayList<>();
            for (Review.File.Comment comment: comments) {
                if (comment.replyToCommentId.equals("null")) {
                    if (comment.id != null) {
                        List<Review.File.Comment> newList = new ArrayList<>();
                        newList.add(comment);
                        threadsMap.put(comment.id, newList);
                        threadList.add(comment.id);
                    } else {
                        newThreadList.add(comment);
                    }
                } else {
                    List<Review.File.Comment> existingList = threadsMap.get(comment.replyToCommentId);
                    existingList.add(comment);
                }
            }

            for (String threadKey: threadList) {
                sortedComments.addAll(threadsMap.get(threadKey));
            }
            sortedComments.addAll(newThreadList);
        }
        return sortedComments;
    }

    private void updateHighLights(Editor editor) {
        MarkupModel markup = editor.getMarkupModel();

        for (RangeHighlighter customRangeHighlighter : newCommentHighlighters) {
            markup.removeHighlighter(customRangeHighlighter);
        }
        newCommentHighlighters.clear();

        int lineCount = markup.getDocument().getLineCount();

        Map<Integer, List<Review.File.Comment>> lineComments = lineComments(comments);
        for (Map.Entry<Integer, List<Review.File.Comment>> entry : lineComments.entrySet()) {
            if (entry.getKey() > lineCount) continue;

            boolean hasNewComments = false;
            boolean hasOpenIssues = false;
            for (Review.File.Comment comment : entry.getValue()) {
                if (comment.id == null) {
                    hasNewComments = true;
                    break;
                }
                if (comment.issueStatus.equals("open")) {
                    hasOpenIssues = true;
                }
            }

            TextAttributes attributes = new TextAttributes();
            if (hasNewComments) attributes.setBackgroundColor(JBColor.MAGENTA);
            else if (hasOpenIssues)  attributes.setBackgroundColor(new JBColor(JBColor.YELLOW.darker(),new Color(155, 149, 59)));
            else attributes.setBackgroundColor(JBColor.CYAN);
            boolean firstComment = true;
            for (Review.File.Comment comment : entry.getValue()) {
                // Colours entire selected area
                for (int i = 0; i < comment.numberOfLines; i++) {
                    RangeHighlighter rangeHighlighter = markup
                            .addLineHighlighter(entry.getKey() - 1 + i, HighlighterLayer.SELECTION + (hasNewComments ? 2 : 1), attributes);

                    // There should only be a single gutter icon on the first line of selection
                    if (i == 0 && firstComment) {
                        firstComment = false;
                        if (hasNewComments) {
                            rangeHighlighter.setGutterIconRenderer(new NewCommentGutterIconRenderer());
                        } else if (hasOpenIssues) {
                            rangeHighlighter.setGutterIconRenderer(new OpenIssuesGutterIconRenderer());
                        } else {
                            rangeHighlighter.setGutterIconRenderer(new CommentGutterIconRenderer());
                        }
                    }
                    newCommentHighlighters.add(rangeHighlighter);
                }
            }
        }
    }

    private Map<Integer, List<Review.File.Comment>> lineComments(List<Review.File.Comment> comments) {
        Map<Integer, List<Review.File.Comment>> result = new HashMap<>();
        for (Review.File.Comment comment : comments) {
            if (!result.containsKey(comment.firstLine)) {
                result.put(comment.firstLine, new ArrayList<Review.File.Comment>());
            }
            result.get(comment.firstLine).add(comment);
        }
        return result;
    }

    public List<Review.File.Comment> getNewComments() {
        final List<Review.File.Comment> newComments = new ArrayList<>();
        for (Review.File.Comment comment : comments) {
            if (comment.id == null) newComments.add(comment);
        }
        return newComments;
    }

    /**
     * Returns the first line of the selected (highlighted) section, otherwise the line user clicked in the line number area
     */
    private int getStartLineNumber(Editor editor, MouseEvent clickEvent) {
        if (editor.getSelectionModel().hasSelection()) {
            return editor.getSelectionModel().getSelectionStartPosition().getLine() + 1;
        } else {
            return EditorUtil.yPositionToLogicalLine(editor, clickEvent) + 1;
        }
    }

    /**
     * Returns the number of selected lines, else 1
     */
    private int getLinesSelected(Editor editor) {
        if (editor.getSelectionModel().hasSelection()) {
            SelectionModel selectedArea = editor.getSelectionModel();
            int startLine = editor.getSelectionModel().getSelectionStartPosition().getLine();
            int endLine = editor.getSelectionModel().getSelectionEndPosition().getLine();
            if (selectedArea.getSelectionEndPosition().getColumn() == 0) {
                return endLine - startLine;
            } else {
                return endLine - startLine + 1;
            }
        } else {
            return 1;
        }
    }


    // helpers for frame wrappers
    public boolean isClosed() {
        return frameWrapper.isDisposed();
    }

    public void close() {
        if (!frameWrapper.isDisposed()) frameWrapper.close();
    }
}
