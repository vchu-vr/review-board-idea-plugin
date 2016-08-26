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

package com.visiercorp.idea.rbplugin.ui.toolswindow.reviewpanel;

import com.visiercorp.idea.rbplugin.reviewboard.Review;
import com.visiercorp.idea.rbplugin.reviewboard.ReviewDataProvider;
import com.visiercorp.idea.rbplugin.state.SettingsPage;
import com.visiercorp.idea.rbplugin.ui.ExceptionHandler;
import com.visiercorp.idea.rbplugin.ui.Icons;
import com.visiercorp.idea.rbplugin.ui.action.CustomComponentActionBase;
import com.visiercorp.idea.rbplugin.ui.diff.CommentsDiffTool;
import com.visiercorp.idea.rbplugin.ui.diff.ReviewDiffRequest;
import com.visiercorp.idea.rbplugin.ui.panels.DraftReviewPanel;
import com.visiercorp.idea.rbplugin.ui.toolswindow.ReviewChangesTreeList;
import com.visiercorp.idea.rbplugin.ui.toolswindow.ReviewTableModel;

import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.diff.DiffRequest;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.SimpleContentRevision;
import com.intellij.ui.GuiUtils;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.intellij.openapi.util.text.StringUtil.join;

/**
 * @author Ritesh
 */
public class ReviewsPanel extends JPanel {

//    reviewListActionGroup.add(new DoNothingAction(TextFieldWithAutoCompletion.create(project, Arrays.asList("asd", "Asd"), true, null)));
//    Messages.showWarningDialog("Svn is still in refresh. Please try again later.", "Alter");
    // TODO: Check for binary files
    private Map<Review.File, CommentsDiffTool> commentsDiffTools = new HashMap<>();

    private JBTable reviewsTable = new JBTable();
    private ReviewChangesTreeList changesTree;
    private ComboBox statusComboBox = new ComboBox(new String[]{"all", "discarded", "pending", "submitted"});
    private ComboBox repositoryComboBox = new ComboBox(new String[]{"Select Repository"});
    private ComboBox incomingReviewsComboBox = new ComboBox(new String[]{"Select Incoming Reviews Filter", "to me directly"});
    private ComboBox diffRevisionComboBox = new ComboBox(new Integer[]{0});
    private JBLabel page = new JBLabel();
    private JComponent mainReviewToolbar;
    private JComponent diffPanelToolbar;

    final Logger LOG = Logger.getInstance(ReviewsPanel.class);

    private ReviewPanelController controller;
    private final Project project;

    private boolean isReconfigured = false;

    public ReviewsPanel(final Project project) {
        this.project = project;
        this.controller = new ReviewPanelController(project, this);
        initUI();
    }

    public void setReviewsList(final int pageNumber, final List<Review> reviews) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                ReviewTableModel reviewTableModel = new ReviewTableModel(reviews);
                reviewsTable.setModel(reviewTableModel);
                reviewsTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
                    @Override
                    public Component getTableCellRendererComponent(JTable table,
                                                                   Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                        String status = reviewTableModel.getStatusAt(row);

                        if ("discarded".equals(status)) {
                            setForeground(new Color(255, 100, 100).darker());
                            setFont(new Font("Segoe UI", Font.ITALIC, 12));
                        } else if ("pending".equals(status)){
                            setForeground(table.getForeground());
                        } else if ("submitted".equals(status)){
                            setForeground(new Color(98, 150, 85));
                        } else {
                            setForeground(table.getForeground());
                        }

                        if (col == 0 && reviewTableModel.getIssueOpenCountAt(row) > 0) {
                            setIcon(Icons.OpenIssueIcon);
                        } else {
                            setIcon(null);
                        }
                        return this;
                    }
                });
                reviewsTable.getColumnModel().getColumn(ReviewTableModel.Columns.SUMMARY.getIndex()).setPreferredWidth(400);
                reviewsTable.getColumnModel().getColumn(ReviewTableModel.Columns.SUBMITTED_TO.getIndex()).setPreferredWidth(50);
                reviewsTable.getColumnModel().getColumn(ReviewTableModel.Columns.SUBMITTER.getIndex()).setPreferredWidth(50);
                reviewsTable.getColumnModel().getColumn(ReviewTableModel.Columns.LAST_MODIFIED.getIndex()).setPreferredWidth(50);
                page.setText(String.valueOf(pageNumber));
                GuiUtils.enableChildren(true, ReviewsPanel.this);
            }
        });
    }

    public void setCurrentReview(List<Review.File> files) {
        final List<Change> changes = new ArrayList<>();
        for (Review.File file : files) {
            FilePath srcFilePath;
            FilePath patchFilePath;
            try {
                Class<?> aClass = Class.forName("com.intellij.openapi.vcs.LocalFilePath");
                srcFilePath = (FilePath) aClass.getDeclaredConstructor(String.class, boolean.class).newInstance(file.srcFileName, false);
                patchFilePath = (FilePath) aClass.getDeclaredConstructor(String.class, boolean.class).newInstance(file.dstFileName, false);
            } catch (Exception e) {
                try {
                    srcFilePath = (FilePath) Class.forName("com.intellij.openapi.vcs.FilePathImpl")
                            .getDeclaredMethod("createNonLocal", String.class, boolean.class)
                            .invoke(null, file.srcFileName, false);
                    patchFilePath = (FilePath) Class.forName("com.intellij.openapi.vcs.FilePathImpl")
                            .getDeclaredMethod("createNonLocal", String.class, boolean.class)
                            .invoke(null, file.dstFileName, false);
                } catch (Exception e1) {
                    throw new RuntimeException(e1);
                }
            }
            SimpleContentRevision original = new SimpleContentRevision(file.srcFileContents, srcFilePath, file.sourceRevision);
            SimpleContentRevision patched = new SimpleContentRevision(file.dstFileContents, patchFilePath, "New Change");
            changes.add(new Change(original, patched));
        }
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            public void run() {
                changesTree.setChangesToDisplay(changes);
                GuiUtils.enableChildren(true, ReviewsPanel.this);
            }
        });
    }

    public void updateRepositories(final List<String> repositories, final String defaultRepository) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                repositoryComboBox.removeAllItems();
                repositoryComboBox.addItem("Select Repository");
                for (String repository : repositories) {
                    repositoryComboBox.addItem(repository);
                }
                if (defaultRepository == null) {
                    repositoryComboBox.setSelectedIndex(0);
                } else {
                    repositoryComboBox.setSelectedItem(defaultRepository);
                }
                repositoryComboBox.setEnabled(true);
            }
        });
    }

    public void updateMyReviewGroups(final List<String> groups, final String defaultIncomingFilter) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                incomingReviewsComboBox.setEnabled(false);
                for (String group : groups) {
                    incomingReviewsComboBox.addItem(group);
                }
                if (defaultIncomingFilter == null) {
                incomingReviewsComboBox.setSelectedIndex(0);
                } else {
                    incomingReviewsComboBox.setSelectedItem(defaultIncomingFilter);
                }
                incomingReviewsComboBox.setEnabled(true);
            }
        });
    }

    public void updateRevisions(final int revisions) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                diffRevisionComboBox.setEnabled(false);
                diffRevisionComboBox.removeAllItems();
                if(revisions > 0) {
                    for (int i = 1; i <= revisions; i++) {
                        diffRevisionComboBox.addItem(i);
                    }
                    diffRevisionComboBox.setEnabled(true);
                    diffRevisionComboBox.setSelectedIndex(revisions-1);
                } else {
                    diffRevisionComboBox.addItem("0");

                    diffRevisionComboBox.setEnabled(true);
                    diffRevisionComboBox.setSelectedIndex(0);
                }
            }
        });
    }

    //TODO remove join
    private void draftReview() {
        Review selectedReview = controller.getSelectedReview();
        if (selectedReview != null) {
            final DraftReviewPanel reviewPanel = new DraftReviewPanel(project, "Update Review Request", selectedReview.summary,
                    selectedReview.description, join(selectedReview.targetPeople, ","),
                    join(selectedReview.targetGroups, ","), selectedReview.repository);

            if (reviewPanel.showAndGet()) {
                controller.updateReviewRequest(selectedReview, reviewPanel.getSummary(), reviewPanel.getDescription(),
                        reviewPanel.getTargetPeople(), reviewPanel.getTargetGroup());
            }
        }
    }

    public void enablePanel(final boolean enable) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            public void run() {
                GuiUtils.enableChildren(ReviewsPanel.this, enable, mainReviewToolbar);

            }
        });
    }

    public void enableDiffPanel(final boolean enable) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            public void run() {
                GuiUtils.enableChildren(enable, changesTree);
                GuiUtils.enableChildren(enable, diffPanelToolbar);

            }
        });
    }

    public void enableDiffRevisionComboBox(final boolean enable) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            public void run() {
                if (enable && diffRevisionComboBox.getItemCount() > 1 ) {
                    diffRevisionComboBox.setEnabled(enable);
                } else {
                    diffRevisionComboBox.setEnabled(false);
                }
            }
        });
    }

    private void loadFileComments() {
        final Change selectedChange = changesTree.getSelectedChanges().get(0);
        List<Review.File> selectedFiles = controller.getSelectedFiles();
        for (Review.File file : selectedFiles) {
            if (file.srcFileName.equals(selectedChange.getBeforeRevision().getFile().getPath())) {
                if (commentsDiffTools.containsKey(file) && !commentsDiffTools.get(file).isClosed()) {
                    commentsDiffTools.get(file).close();
                }
                controller.loadComments(file);
                break;
            }
        }
    }

    public void showCommentsDiff(final Review.File file, final List<Review.File.Comment> comments) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                final Change selectedChange = changesTree.getSelectedChanges().get(0);
                DiffRequest request = new ReviewDiffRequest(project, selectedChange);
                final CommentsDiffTool commentsDiffTool = new CommentsDiffTool(file, comments);
                commentsDiffTool.setActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        controller.updateNewComments(file, commentsDiffTool.getNewComments());
                    }
                });
                commentsDiffTool.show(request);
                commentsDiffTools.put(file, commentsDiffTool);
            }
        });
    }

    private int getSelectedReviewIndex() {
        return reviewsTable.getSelectedRow();
    }

    @SuppressWarnings("unchecked")
    private void initUI() {
        setLayout(new BorderLayout());

        changesTree = new ReviewChangesTreeList(project, new ArrayList());

        mainReviewToolbar = createMainReviewToolbar();

        JPanel reviewsListPanel = new JPanel(new BorderLayout());
        JPanel toolbarGroup = new JPanel(new BorderLayout());
        toolbarGroup.add(mainReviewToolbar, BorderLayout.WEST);
        toolbarGroup.add(createReviewListToolbar(), BorderLayout.CENTER);

        reviewsListPanel.add(toolbarGroup, BorderLayout.PAGE_START);
        reviewsListPanel.add(new JBScrollPane(reviewsTable), BorderLayout.CENTER);

        JPanel diffPanel = new JPanel(new BorderLayout());
        diffPanelToolbar = createDiffPanelToolbar();
        diffPanel.add(diffPanelToolbar, BorderLayout.PAGE_START);
        diffPanel.add(new JBScrollPane(changesTree), BorderLayout.CENTER);

        Splitter splitter = new Splitter(false, 0.8f);
        splitter.setFirstComponent(reviewsListPanel);
        splitter.setSecondComponent(diffPanel);

        add(splitter);

        reviewsTable.setRowHeight(20);
        reviewsTable.setShowGrid(false);
        reviewsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting() || getSelectedReviewIndex() < 0) return;
                if (!controller.isSelectedReviewSameAsCurrent(getSelectedReviewIndex())) {
                    controller.selectedReviewChanged(getSelectedReviewIndex());
                }
            }
        });

        statusComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.statusChanged((String) statusComboBox.getSelectedItem());
            }
        });
        repositoryComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.repositoryChanged((String) repositoryComboBox.getSelectedItem());
            }
        });
        incomingReviewsComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                switch ((String) incomingReviewsComboBox.getSelectedItem()) {
                    case "Select Incoming Reviews Filter":
                        controller.filterChangedToIncomingReviewGroup("none");
                        break;
                    case "to me directly":
                        controller.filterChangedToIncomingToMeDirectly();
                        break;
                    default:
                        controller.filterChangedToIncomingReviewGroup((String) incomingReviewsComboBox.getSelectedItem());
                }
            }
        });
        diffRevisionComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (diffRevisionComboBox.getItemCount() > 0 && diffRevisionComboBox.isEnabled() && ((Integer) diffRevisionComboBox.getSelectedItem() != 0)) {
                    controller.diffRevisionChanged((Integer) diffRevisionComboBox.getSelectedItem(), (Integer) diffRevisionComboBox.getItemAt(diffRevisionComboBox.getItemCount() - 1));
                }
            }
        });
        changesTree.setDoubleClickHandler(new Runnable() {
            @Override
            public void run() {
                ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading Comments") {
                    @Override
                    public void run(@NotNull ProgressIndicator progressIndicator) {
                        ApplicationManager.getApplication().invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    loadFileComments();
                                } catch (Exception e) {
                                    ExceptionHandler.getMessage(e);
                                }
                            }
                        });
                    }
                });
            }
        });
        new AnAction() {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                loadFileComments();
            }
        }.registerCustomShortcutSet(new CustomShortcutSet(CommonShortcuts.getDiff().getShortcuts()), changesTree);

        // Trigger to load reviews
        enablePanel(false);
        diffRevisionComboBox.setEnabled(false);
        repositoryComboBox.setEnabled(false);
        enableDiffPanel(false);
        if (controller.isConfigured() && controller.isConnected()) {
            statusComboBox.setSelectedIndex(0);
            controller.loadRepositories(null);
            incomingReviewsComboBox.setEnabled(false);
            controller.loadReviewGroups(null);
        } else {
            controller.showSettingsDialog();
            isReconfigured = true;
        }
    }

    private void refreshPanel() {
        repositoryComboBox.setEnabled(false);
        enableDiffPanel(false);
        if (isReconfigured) {
            statusComboBox.setSelectedIndex(0);
            controller.loadRepositories(null);
            controller.loadReviewGroups(null);
            isReconfigured = false;
        } else {
            controller.loadRepositories((String) repositoryComboBox.getSelectedItem());
        }
        controller.refreshReviews();
    }

    private JComponent createMainReviewToolbar() {

        DefaultActionGroup actionGroup = new DefaultActionGroup("ReviewBoardMainActionsGroup", false);
        actionGroup.add(new AnAction("Refresh reviews", "Refresh reviews", AllIcons.Actions.Refresh) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                refreshPanel();
            }
        });
        actionGroup.add(new AnAction("Browse", "Open the selected review in browser", AllIcons.General.Web) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                if (controller.getSelectedReview() == null)
                    BrowserUtil.browse(ReviewDataProvider.getInstance(project).reviewBoardUrl(project));
                else
                    BrowserUtil.browse(ReviewDataProvider.getInstance(project).reviewUrl(project, controller.getSelectedReview()));
            }
        });
        actionGroup.add(new AnAction("Settings", "Open review board settings", AllIcons.General.Settings) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, SettingsPage.NAME);
            }
        });

        return ActionManager.getInstance().createActionToolbar("ReviewBoardMainActionsGroup", actionGroup, true)
                .getComponent();

    }

    private JComponent createDiffPanelToolbar() {
        DefaultActionGroup actionGroup = new DefaultActionGroup("ReviewBoardDiffActionsGroup", false);
        actionGroup.add(new AnAction("Refresh", "Refresh", AllIcons.Actions.Refresh) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                controller.loadSelectedReview(controller.getSelectedReview(), true);
            }

            @Override
            public void update(AnActionEvent e) {
                e.getPresentation().setEnabled(getSelectedReviewIndex() != -1);
            }
        });
        actionGroup.addAction(new AnAction("Publish Review and Replies", "Publish comments, replies and changes to server", AllIcons.Actions.Export) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                if (controller.commentsAvailableForPublish() > 0 && controller.repliesAvailableForPublish() > 0) {
                    // TODO: needs to make
                    final String reviewComment = Messages.showInputDialog(project, "Review Comment", "Review Comment", null);
                    if (reviewComment != null) {
                        controller.publishAll(reviewComment);
                    }
                }
                else if (controller.commentsAvailableForPublish() > 0) {
                    final String reviewComment = Messages.showInputDialog(project, "Review Comment", "Review Comment", null);
                    if (reviewComment != null) {
                        controller.publishAll(reviewComment);
                    }
                }

                else if (controller.repliesAvailableForPublish() > 0) {
                    Review selectedReview = controller.getSelectedReview();
                    if (Messages.showOkCancelDialog(
                            String.format("Do you want to publish your replies on review request #%s: '%s'?", selectedReview.id, selectedReview.summary)
                            , "Confirmation",
                            Icons.NewCommentIcon) == Messages.OK) {
                        controller.publishAll(null);
                    }
                }
            }

            @Override
            public void update(AnActionEvent e) {
                e.getPresentation().setEnabled(controller.anyAvailableForPublish());
            }
        });
        actionGroup.add(new Separator());
        actionGroup.add(new AnAction("Apply Patch", "Apply Patch locally from diff on ReviewBoard", Icons.applyPatchIcon) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                controller.applyPatch();
            }

            @Override
            public void update(AnActionEvent e) {
                e.getPresentation().setEnabled(getSelectedReviewIndex() != -1);
            }
        });
        actionGroup.add(new Separator());
        actionGroup.add(new CustomComponentActionBase(new JBLabel("Revision : ")));
        actionGroup.add(new CustomComponentActionBase(diffRevisionComboBox));
        actionGroup.add(new Separator());
        actionGroup.addAll(changesTree.getTreeActions());
        return ActionManager.getInstance().createActionToolbar("ReviewBoardDiffActionGroup", actionGroup, true)
                .getComponent();
    }

    private JComponent createReviewListToolbar() {
        DefaultActionGroup actionGroup = new DefaultActionGroup("ReviewBoardListActionGroup", false);

        actionGroup.add(new Separator());
        actionGroup.add(new CustomComponentActionBase(new JBLabel("Status : ")));
        actionGroup.add(new CustomComponentActionBase(statusComboBox));
        actionGroup.add(new Separator());
        actionGroup.add(new CustomComponentActionBase(repositoryComboBox));
        actionGroup.add(new Separator());
        actionGroup.add(new ToggleAction("Incoming (default: all)", "Show incoming reviews", AllIcons.Ide.IncomingChangesOn) {
            @Override
            public boolean isSelected(AnActionEvent anActionEvent) {
                return controller.getReviewListFilter() == ReviewPanelController.ReviewListFilter.INCOMING;
            }

            @Override
            public void setSelected(AnActionEvent anActionEvent, boolean b) {
                controller.filterChangedToIncomingReviewGroup("none");
                incomingReviewsComboBox.setSelectedIndex(0);
            }
        });
        actionGroup.add(new CustomComponentActionBase(incomingReviewsComboBox));
        actionGroup.add(new ToggleAction("Outgoing", "Show outgoing reviews", AllIcons.Ide.OutgoingChangesOn) {
            @Override
            public boolean isSelected(AnActionEvent anActionEvent) {
                return controller.getReviewListFilter() == ReviewPanelController.ReviewListFilter.OUTGOING;
            }

            @Override
            public void setSelected(AnActionEvent anActionEvent, boolean b) {
                incomingReviewsComboBox.setSelectedIndex(0);
                controller.filterChanged(ReviewPanelController.ReviewListFilter.OUTGOING);
            }
        });

        actionGroup.add(new ToggleAction("All", "Show all reviews", AllIcons.General.ProjectStructure) {
            @Override
            public boolean isSelected(AnActionEvent anActionEvent) {
                return controller.getReviewListFilter() == ReviewPanelController.ReviewListFilter.ALL;
            }

            @Override
            public void setSelected(AnActionEvent anActionEvent, boolean b) {
                incomingReviewsComboBox.setSelectedIndex(0);
                controller.filterChanged(ReviewPanelController.ReviewListFilter.ALL);
            }
        });

        actionGroup.add(new Separator());
        actionGroup.add(new AnAction("First", "Go to first page", AllIcons.Actions.AllLeft) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                controller.loadFirst();
            }

            @Override
            public void update(AnActionEvent e) {
                e.getPresentation().setEnabled(controller.isFirstEnabled());
            }
        });

        actionGroup.add(new AnAction("Back", "Go to previous page", AllIcons.Actions.Left) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                controller.loadPrevious();
            }

            @Override
            public void update(AnActionEvent e) {
                e.getPresentation().setEnabled(controller.hasPrevious());
            }
        });

        actionGroup.add(new CustomComponentActionBase(page));
        actionGroup.add(new AnAction("Forward", "Go to next page", AllIcons.Actions.Right) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                controller.loadNext();
            }

            @Override
            public void update(AnActionEvent e) {
                e.getPresentation().setEnabled(controller.hasNext());
            }
        });

        actionGroup.add(new AnAction("Last", "Go to last page", AllIcons.Actions.AllRight) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                controller.loadLast();
            }

            @Override
            public void update(AnActionEvent e) {
                e.getPresentation().setEnabled(controller.isLastEnabled());
            }
        });

        actionGroup.add(new Separator());
        actionGroup.add(new AnAction("Edit Review", "Edit selected review", AllIcons.Actions.EditSource) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                draftReview();
            }

            @Override
            public void update(AnActionEvent e) {
                e.getPresentation().setEnabled(controller.getSelectedReview() != null);
            }
        });

        actionGroup.add(new AnAction("Ship It", "Ship It", AllIcons.Graph.NodeSelectionMode) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                controller.shipReview(controller.getSelectedReview());
            }

            @Override
            public void update(AnActionEvent e) {
                e.getPresentation().setEnabled(controller.getSelectedReview() != null);
            }
        });
        actionGroup.add(new AnAction("Submit Review", "Submit the selected review", AllIcons.Actions.MoveToAnotherChangelist) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                controller.submitReview(controller.getSelectedReview());
            }

            @Override
            public void update(AnActionEvent e) {
                e.getPresentation().setEnabled(controller.getSelectedReview() != null);
            }
        });

        actionGroup.add(new AnAction("Discard Review", "Discard the selected review", AllIcons.Actions.Cancel) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                controller.discardReview(controller.getSelectedReview());
            }

            @Override
            public void update(AnActionEvent e) {
                e.getPresentation().setEnabled(controller.getSelectedReview() != null);
            }
        });

        return ActionManager.getInstance().createActionToolbar("ReviewBoardActions", actionGroup, true)
                .getComponent();
    }

    // Reverts the selected review row in the table if possible
    public void updateSelectedReviewRow(int selectedRow) {
        if (selectedRow != -1) {
            ApplicationManager.getApplication().invokeLater(new Runnable() {
                @Override
                public void run() {
                    reviewsTable.getSelectionModel().setSelectionInterval(selectedRow, selectedRow);
                }
            });
        }
    }

    public void clearAllSelectedRows() {
        reviewsTable.clearSelection();
    }

    public void closeAllCommentsDiff() {
        for (CommentsDiffTool commentsDiffTool: commentsDiffTools.values()) {
            ApplicationManager.getApplication().invokeLater(new Runnable() {
                @Override
                public void run() {
                    commentsDiffTool.close();
                }
            });
        }
        commentsDiffTools.clear();
    }

    public String getHgRepository() {
        return findHgRepository(project);
    }

    private String findHgRepository(Project project) {
        String repo = "unknown";
        try {
            String line;

            FileReader fileReader = new FileReader(project.getBasePath() + java.io.File.separator + ".hg" + java.io.File.separator + "hgrc");

            BufferedReader bufferedReader = new BufferedReader(fileReader);

            while((line = bufferedReader.readLine()) != null) {
                if (line.startsWith("[paths]")) {
                    repo = bufferedReader.readLine().replace("default = http://saga.visier.corp/hg/visier/", "").split("/")[0];
                }
            }

            // Always close files.
            bufferedReader.close();
        } catch (FileNotFoundException e) {

        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return repo;
    }

}
