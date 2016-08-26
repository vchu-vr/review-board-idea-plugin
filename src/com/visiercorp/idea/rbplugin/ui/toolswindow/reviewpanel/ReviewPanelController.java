// Copyright (c) [2010-2016] Visier Solutions Inc. All rights reserved.

//
// Copyright © [2010-2016] Visier Solutions Inc. All rights reserved.
//
package com.visiercorp.idea.rbplugin.ui.toolswindow.reviewpanel;

import com.visiercorp.idea.rbplugin.exception.InvalidConfigurationException;
import com.visiercorp.idea.rbplugin.exception.InvalidCredentialException;
import com.visiercorp.idea.rbplugin.messages.PluginBundle;
import com.visiercorp.idea.rbplugin.reviewboard.Repository;
import com.visiercorp.idea.rbplugin.reviewboard.Review;
import com.visiercorp.idea.rbplugin.reviewboard.ReviewDataProvider;
import com.visiercorp.idea.rbplugin.reviewboard.model.RBGroupList;
import com.visiercorp.idea.rbplugin.reviewboard.model.RBGroupList.Group;
import com.visiercorp.idea.rbplugin.reviewboard.model.RBUserList;
import com.visiercorp.idea.rbplugin.state.SettingsPage;
import com.visiercorp.idea.rbplugin.ui.Icons;
import com.visiercorp.idea.rbplugin.ui.TaskUtil;
import com.visiercorp.idea.rbplugin.util.Page;
import com.visiercorp.idea.rbplugin.util.ThrowableFunction;
import com.intellij.icons.AllIcons;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vcs.changes.patch.ApplyPatchAction;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.awt.RelativePoint;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReviewPanelController {
    private Project project;
    private ReviewsPanel view;

    public enum ReviewListFilter {
        ALL, INCOMING, OUTGOING
    }

    private ReviewListFilter reviewListFilter = ReviewListFilter.INCOMING;
    private String status;
    private String repositoryId;
    private List<Repository> repositories;
    private List<String> groups;
    private String incomingReviewGroupFilter = "none";
    private boolean toMeDirectly = false;

    private int revisionVersion = 0;

    private Page<Review> reviews;
    private Review selectedReview;
    private static final int COUNT = 25;
    private int start = 0, count = COUNT;

    private Map<String, Integer> reviewCache = new HashMap<>(); // maps review.id key to total number of revisions
    private Map<String, List<Review.File>> reviewRevisionCache = new HashMap<>(); // maps review.id_revision.num keys to list of review files

    //Map of fileId and new comments list
    private Map<String, List<Review.File.Comment>> newComments = new HashMap<>();
    private List<Review.File> selectedFiles;

    public ReviewPanelController(Project project, ReviewsPanel view) {
        this.project = project;
        this.view = view;
    }

    public boolean isConfigured() {
        return ReviewDataProvider.isConfiguredProperly(project);
    }

    public boolean isConnected() {
        return ReviewDataProvider.isConnected(project);
    }

    public void showSettingsDialog() {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, SettingsPage.NAME);
            }
        });
    }

    public void refreshReviews() {
        view.enablePanel(false);
        TaskUtil.queueTask(project, "Loading reviews", false, new ThrowableFunction<ProgressIndicator, Page<Review>>() {
            @Override
            public Page<Review> throwableCall(final ProgressIndicator progressIndicator) throws Exception {

                clearCaches();

                try {
                    final String username = ReviewDataProvider.getConfiguration(project).username;
                    final String fromUser = reviewListFilter == ReviewListFilter.OUTGOING ? username : null;
                    final String toUser = reviewListFilter == ReviewListFilter.INCOMING ? username : null;
                    final String toMe = toMeDirectly ? username : null;

                    reviews = ReviewDataProvider.getInstance(project).listReviews(fromUser, toUser, toMe, incomingReviewGroupFilter, status, repositoryId
                            , start, count);
                    view.setReviewsList(start / COUNT + 1, reviews.getResult());
                    view.enablePanel(true);

                    view.updateSelectedReviewRow(reviews.getReviewIndex(selectedReview));


                    return reviews;
                } catch (InvalidConfigurationException |InvalidCredentialException e) {
                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            ShowSettingsUtil.getInstance().showSettingsDialog(project, SettingsPage.NAME);
                        }
                    });
                    throw e;
                }
            }
        }, null, null);
    }


    public void loadRepositories(final String defaultRepository) {
        TaskUtil.queueTask(project, "Loading Repositories", false, new ThrowableFunction<ProgressIndicator, Void>() {
            @Override
            public Void throwableCall(ProgressIndicator params) throws Exception {
                repositories = ReviewDataProvider.getInstance(project).repositories();
                List<String> repositoryNames = new ArrayList<>();
                for (Repository repository : repositories) {
                    repositoryNames.add(repository.name);
                }
                view.updateRepositories(repositoryNames, defaultRepository);
                return null;
            }
        }, null, null);
    }

    public void loadReviewGroups(final String defaultReviewGroup) {
        TaskUtil.queueTask(project, "Loading Review Groups", false, new ThrowableFunction<ProgressIndicator, Void>() {
            @Override
            public Void throwableCall(ProgressIndicator params) throws Exception {

                final String username = ReviewDataProvider.getConfiguration(project).username;

                RBGroupList groupList =
                        ReviewDataProvider.getInstance(project).getGroups();
                groups = new ArrayList<String>();
                for (Group group : groupList.groups) {
                    String groupName = group.name;
                    RBUserList users = ReviewDataProvider.getInstance(project).groupUsers(groupName);
                    for (RBUserList.RBUser user : users.users) {
                        if (user.username.equals(username)) {
                            groups.add(groupName);
                            break;
                        }
                    }
                }
                view.updateMyReviewGroups(groups, defaultReviewGroup);
                return null;
            }
        }, null, null);
    }


    public void discardReview(final Review review) {
        TaskUtil.queueTask(project, "Discarding Review", false, new ThrowableFunction<ProgressIndicator, Void>() {
            @Override
            public Void throwableCall(ProgressIndicator params) throws Exception {
                ReviewDataProvider.getInstance(project).discardedReviewRequest(review);
                return null;
            }
        }, null, null);
    }


    public void submitReview(final Review review) {
        TaskUtil.queueTask(project, "Submitting Review", false, new ThrowableFunction<ProgressIndicator, Void>() {
            @Override
            public Void throwableCall(ProgressIndicator params) throws Exception {
                ReviewDataProvider.getInstance(project).submittedReviewRequest(review);
                return null;
            }
        }, null, null);
    }

    public void shipReview(final Review review) {
        TaskUtil.queueTask(project, "Ship It", false, new ThrowableFunction<ProgressIndicator, Void>() {
            @Override
            public Void throwableCall(ProgressIndicator params) throws Exception {
                ReviewDataProvider.getInstance(project).shipIt(review);
                return null;
            }
        }, null, null);
    }


    public void loadComments(final Review.File file) {
        TaskUtil.queueTask(project, "Loading Comments", false, new ThrowableFunction<ProgressIndicator, List<Review.File.Comment>>() {
            @Override
            public List<Review.File.Comment> throwableCall(ProgressIndicator params) throws Exception {
                List<Review.File.Comment> comments = ReviewDataProvider.getInstance(project).comments(selectedReview, file);

                List<Review.File.Comment> commentsForFile = newComments.get(file.fileId);
                if (commentsForFile != null) comments.addAll(commentsForFile);

                if (file.dstFileContents.contains("\"stat\": \"fail\"")
                        && file.dstFileContents.contains("\"There was an error fetching a source file.\"")
                        && file.dstFileContents.contains("\"code\": 221")) {
                    showFileDisplayErrorMessage(selectedReview, file);
                    return new ArrayList<Review.File.Comment>();
                }
                view.showCommentsDiff(file, comments);
                return comments;
            }
        }, null, null);
    }

    private void showFileDisplayErrorMessage(final Review review, final Review.File file) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                String message = String.format("There was an error fetching a source file to display the diff in this review revision. " +
                        "See this review revision in the web application for more details.\n" +
                        "Review id: %s, revision: #%s, file: '%s' ", review.id, revisionVersion, file.dstFileName);

                StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
                JBPopupFactory.getInstance()
                        .createHtmlTextBalloonBuilder(message, MessageType.ERROR, null)
                        .setFadeoutTime(7500)
                        .createBalloon()
                        .show(RelativePoint.getNorthEastOf(statusBar.getComponent()),
                                Balloon.Position.atRight);
                Notifications.Bus.notify(new Notification("ReviewBoard", "Error", message, NotificationType.ERROR));
            }
        });
    }


    public void updateNewComments(Review.File file, List<Review.File.Comment> comments) {
        if (comments.isEmpty()) {
            newComments.remove(file.fileId);
        } else {
            newComments.put(file.fileId, comments);
        }
    }

    public void publishAll(final String reviewComment) {
        view.enablePanel(false);
        final List<Review.File.Comment> allComments = new ArrayList<>();
        for (List<Review.File.Comment> values : newComments.values()) {
            allComments.addAll(values);
        }
        final List<Review.File.Comment> comments = new ArrayList<>();
        final List<Review.File.Comment> replies = new ArrayList<>();

        Map<String, List<Review.File.Comment>> commentsMap = allComments.stream().collect(Collectors.groupingBy((Review.File.Comment c) -> c.replyToCommentId));
        for (Map.Entry<String, List<Review.File.Comment>> entry : commentsMap.entrySet()) {
            if (!entry.getKey().equals("null")) {
                replies.addAll(entry.getValue());
            } else {
                comments.addAll(commentsMap.get("null"));
            }
        }
        String taskTitle = (comments.size() > 0 && replies.size() > 0) ? "Publishing Review and Replies" : ((comments.size() > 0) ? "Publishing Review" : "Publishing Replies");
        TaskUtil.queueTask(project, taskTitle, false, new ThrowableFunction<ProgressIndicator, Void>() {
            @Override
            public Void throwableCall(final ProgressIndicator progressIndicator) throws Exception {
                ReviewDataProvider.getInstance(project).createRepliesAndComments(selectedReview, comments, replies, reviewComment,
                        new ReviewDataProvider.Progress() {
                            @Override
                            public void progress(String text, float percentage) {
                                progressIndicator.setFraction(percentage);
                                progressIndicator.setText(text);
                            }
                        });
                clearNewComments();
                return null;
            }
        }, new ThrowableFunction<Void, Void>() {
            @Override
            public Void throwableCall(Void params) throws Exception {
                loadSelectedReview(selectedReview, false);
                view.enablePanel(true);
                return null;
            }
        }, null);
    }


    private boolean confirmReviewDiscard() {
        return Messages.showOkCancelDialog(
                String.format("Do you want to discard your review on review request #%s: '%s'?", selectedReview.id, selectedReview.summary)
                , "Confirmation",
                Icons.NewCommentIcon) != Messages.OK;
    }

    public void loadSelectedReview(Review newlySelectedReview, boolean refreshButtonClicked) {
        if (newlySelectedReview == null) return;

        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {

                if (anyAvailableForPublish() && confirmReviewDiscard()) {
                    view.clearAllSelectedRows();
                    // Revert the selected review row in the table if possible
                    view.updateSelectedReviewRow(reviews.getReviewIndex(selectedReview));
                    return;
                }
                selectedReview = newlySelectedReview;
                clearNewComments();
                view.enablePanel(false);

                TaskUtil.queueTask(project, "Closing all diff windows", false, new ThrowableFunction<ProgressIndicator, Void>() {
                    @Override
                    public Void throwableCall(final ProgressIndicator progressIndicator) throws Exception {
                        view.closeAllCommentsDiff();
                        return null;
                    }
                }, null, null);

                TaskUtil.queueTask(project, "Loading review revisions", false, new ThrowableFunction<ProgressIndicator, Void>() {
                    @Override
                    public Void throwableCall(final ProgressIndicator progressIndicator) throws Exception {
                        view.enableDiffPanel(false);
                        int revisions;
                        if (!reviewCache.containsKey(selectedReview.id) || refreshButtonClicked) {
                            revisions = ReviewDataProvider.getInstance(project).revisions(selectedReview);
                            reviewCache.put(selectedReview.id, revisions);
                        } else {
                            revisions = reviewCache.get(selectedReview.id);
                        }

                        revisionVersion = 0;
                        view.updateRevisions(revisions);
                        return null;
                    }
                }, null, null);

            }
        });
    }

    private void loadSelectedDiffRevision(int revisionNumber, int totalRevisions) {

        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                TaskUtil.queueTask(project, "Closing all diff windows", false, new ThrowableFunction<ProgressIndicator, Void>() {
                    @Override
                    public Void throwableCall(final ProgressIndicator progressIndicator) throws Exception {
                        view.closeAllCommentsDiff();
                        return null;
                    }
                }, null, null);

                TaskUtil.queueTask(project, String.format("Loading review revision %s", revisionNumber), false, new ThrowableFunction<ProgressIndicator, List<Review.File>>() {
                    @Override
                    public List<Review.File> throwableCall(final ProgressIndicator progressIndicator) throws Exception {

                        view.enablePanel(false);

                        List<Review.File> files;
                        if (reviewRevisionCache.containsKey(selectedReview.id + "_" + String.valueOf(revisionNumber))) {
                            files = reviewRevisionCache.get(selectedReview.id + "_" + String.valueOf(revisionNumber));
                        } else {
                            files = ReviewDataProvider.getInstance(project).files(selectedReview, revisionNumber, totalRevisions,
                                    new ReviewDataProvider.Progress() {
                                        @Override
                                        public void progress(String text, float percentage) {
                                            progressIndicator.setText(text);
                                            progressIndicator.setFraction(percentage);
                                        }
                                    }, project);
                            reviewRevisionCache.put(selectedReview.id + "_" + String.valueOf(revisionNumber), files);
                        }
                        view.enablePanel(true);
                        view.enableDiffPanel(true);
                        view.enableDiffRevisionComboBox(true);
                        selectedFiles = files;
                        view.setCurrentReview(selectedFiles);
                        return files;
                    }
                }, null, null);

            }
        });
    }


    public void updateReviewRequest(final Review selectedReview, final String summary, final String description,
                                    final String targetPeople, final String targetGroup) {
        TaskUtil.queueTask(project, "Updating Review", false, new ThrowableFunction<ProgressIndicator, Void>() {
            @Override
            public Void throwableCall(final ProgressIndicator progressIndicator) throws Exception {
                ReviewDataProvider.getInstance(project).updateReviewRequest(selectedReview, summary, description,
                        targetPeople, targetGroup);
                return null;
            }
        }, null, null);
    }


    public void statusChanged(String status) {
        this.status = status;
        this.start = 0;
        refreshReviews();
    }

    public void repositoryChanged(String repositoryName) {
        this.repositoryId = null;
        for (Repository repository : repositories) {
            if (repository.name == repositoryName) {
                this.repositoryId = repository.id;
                break;
            }

        }
        this.start = 0;
        refreshReviews();
    }

    public void filterChanged(ReviewListFilter reviewListFilter) {
        this.reviewListFilter = reviewListFilter;
        this.incomingReviewGroupFilter = "none";
        this.toMeDirectly = false;
        this.start = 0;
        refreshReviews();
    }

    public void filterChangedToIncomingToMeDirectly() {
        if (!toMeDirectly) {
            this.incomingReviewGroupFilter = "none";
            this.toMeDirectly = true;
            this.reviewListFilter = ReviewListFilter.INCOMING;
            this.start = 0;
            refreshReviews();
        }
    }

    public void filterChangedToIncomingReviewGroup(String reviewGroup) {
        if (this.reviewListFilter != ReviewListFilter.INCOMING || !reviewGroup.equals(this.incomingReviewGroupFilter) || toMeDirectly) {
            this.incomingReviewGroupFilter = reviewGroup;
            this.toMeDirectly = false;
            this.reviewListFilter = ReviewListFilter.INCOMING;
            this.start = 0;
            refreshReviews();
        }
    }

    public boolean isSelectedReviewSameAsCurrent(int newlySelectedReviewIndex) {
        Review newlySelectedReview = reviews.getResult().get(newlySelectedReviewIndex);
        return newlySelectedReview.equals(selectedReview);
    }

    public void selectedReviewChanged(int selectedReviewIndex) {
        loadSelectedReview(reviews.getResult().get(selectedReviewIndex), false);
    }

    public void diffRevisionChanged(int selectedRevisionValue, int totalRevisions) {
        if (this.revisionVersion != selectedRevisionValue && selectedRevisionValue != 0) {
            this.revisionVersion = selectedRevisionValue;
            view.enableDiffPanel(false);
            loadSelectedDiffRevision(selectedRevisionValue, totalRevisions);
        }
    }

    public void applyPatch() {
        view.enablePanel(false);
        TaskUtil.queueTask(project, "Loading patch", false, new ThrowableFunction<ProgressIndicator, Void>() {
            @Override
            public Void throwableCall(final ProgressIndicator progressIndicator) throws Exception {
                java.io.File patchFile = null;
                try {
                    String patchString = ReviewDataProvider.getInstance(project).patch(selectedReview, String.valueOf(revisionVersion));
                    java.io.File newPatchFile = new java.io.File(project.getBasePath(), "rb" + selectedReview.id + "_" + revisionVersion + ".patch");
                    patchFile = newPatchFile;

                    FileWriter fw = new FileWriter(newPatchFile);
                    fw.write(patchString);
                    fw.close();
                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        public void run() {
                            if ((view.getHgRepository().equals(selectedReview.repository) || acknowledgeWrongProjectWarning(view.getHgRepository(), selectedReview)) && isProjectClean()) {
                                ApplyPatchAction.showApplyPatch(project, LocalFileSystem.getInstance().refreshAndFindFileByPath(newPatchFile.getPath()));
                            }
                            view.enablePanel(true);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    if (patchFile != null) patchFile.delete();
                }
                return null;
            }
        }, null, null);

    }

    private boolean isProjectClean() {

        if (!view.getHgRepository().equals("unknown")) {
            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
            String[] command;
            String line;
            if (!isWindows) {
                String[] commandHelper = {"/bin/bash", "-c", "export PATH=PATH-TO-ADD:/usr/local/bin && exec && hg status -m -a -r -d"};
                command = commandHelper;
            } else {
                String[] commandHelper = {"hg", "status", "-m", "-a", "-r", "-d"};
                command = commandHelper;
            }

            try {
                ProcessBuilder builder = new ProcessBuilder(command);
                builder.directory(new java.io.File(project.getBasePath()));
                builder.redirectErrorStream(true);
                Process p = builder.start();
                BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
                line = r.readLine();
                if (line == null) {
                    return true;
                } else if (line.startsWith("M ") || line.startsWith("A ") || line.startsWith("R ") || line.startsWith("! ")) {
                    showProjectHasLocalChangesDialog();
                    return false;
                } else {
                    return acknowledgeLocalChangeDetectionFailureWarning();
                }

            } catch (IOException e) {
                return acknowledgeLocalChangeDetectionFailureWarning();
            }
        }
        return true;
    }

    private void showProjectHasLocalChangesDialog() {
        String warningMessage = "There are local changes detected in this project. Please shelve changes before attempting to apply patch.";
        String warningTitle = "Apply Patch Interrupted: Local Changes Detected";
        Messages.showInfoMessage(warningMessage, warningTitle);
    }

    private boolean acknowledgeLocalChangeDetectionFailureWarning() {
        return Messages.showYesNoCancelDialog("Unable to determine if the project has local changes. " +
                        "Select Yes to proceed to apply patch if you have verified that there are no local changes.",
                "Apply Patch Warning: Failed To Detect Local Changes",
                AllIcons.General.BalloonWarning) == Messages.YES;
    }

    private boolean acknowledgeWrongProjectWarning(String projectHgRepository, Review review) {
        String warningMessage = String.format(PluginBundle.message(PluginBundle.PATCH_WARNING_NOTIFICATION), review.id, review.summary) +
                "\n\n" +
                (projectHgRepository.equals("unknown") ?
                        String.format(PluginBundle.message(PluginBundle.PATCH_WARNING_UNKNOWN_REPO), review.repository) :
                        String.format(PluginBundle.message(PluginBundle.PATCH_WARNING_PROJECT_MISMATCH), projectHgRepository, review.repository)) +
                "\n\n" +
                PluginBundle.message(PluginBundle.PATCH_WARNING_OPTIONS);
        String warningTitle =
                (projectHgRepository.equals("unknown") ?
                        PluginBundle.message(PluginBundle.PATCH_WARNING_TITLE_UNKNOWN_REPO) :
                        PluginBundle.message(PluginBundle.PATCH_WARNING_TITLE_PROJECT_MISMATCH));
        return Messages.showOkCancelDialog(warningMessage, warningTitle,
                AllIcons.General.BalloonWarning) == Messages.OK;
    }

    private void clearCaches() {
        reviewCache.clear();
        reviewRevisionCache.clear();
    }

    public Review getSelectedReview() {
        return selectedReview;
    }

    public ReviewListFilter getReviewListFilter() {
        return reviewListFilter;
    }

    public List<Review.File> getSelectedFiles() {
        return selectedFiles;
    }

    public void clearNewComments() {
        newComments.clear();
    }

    public boolean anyAvailableForPublish() {
        return !newComments.isEmpty();
    }

    public int commentsAvailableForPublish() {
        final List<Review.File.Comment> comments = new ArrayList<>();
        for (List<Review.File.Comment> values : newComments.values()) {
            comments.addAll(values);
        }
        Map<String, List<Review.File.Comment>> commentsMap = comments.stream().collect(Collectors.groupingBy((Review.File.Comment c) -> c.replyToCommentId));
        if (commentsMap.containsKey("null")) {
            return commentsMap.get("null").size();
        }
        return 0;
    }

    public int repliesAvailableForPublish() {
        return newComments.values().size() - commentsAvailableForPublish();
    }

    public void loadNext() {
        if (hasNext()) {
            start = reviews.getOffset() + COUNT;
            refreshReviews();
        }
    }

    public void loadPrevious() {
        if (hasPrevious()) {
            start = reviews.getOffset() - COUNT;
            refreshReviews();
        }
    }

    public void loadFirst() {
        start = 0;
        refreshReviews();
    }

    public void loadLast() {
        start = (reviews.getTotal() / COUNT) * COUNT;
        refreshReviews();
    }

    public boolean hasNext() {
        return reviews != null && reviews.getOffset() + COUNT < reviews.getTotal();
    }

    public boolean hasPrevious() {
        return reviews != null && reviews.getOffset() > 0;
    }

    public boolean isFirstEnabled() {
        return !(start == 0);
    }

    public boolean isLastEnabled() {
        return !(reviews != null && start == ((reviews.getTotal() / COUNT) * COUNT));
    }

}

