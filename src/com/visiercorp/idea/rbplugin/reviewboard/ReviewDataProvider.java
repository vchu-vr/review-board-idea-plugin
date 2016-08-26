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

package com.visiercorp.idea.rbplugin.reviewboard;
import com.visiercorp.idea.rbplugin.exception.InvalidConfigurationException;
import com.visiercorp.idea.rbplugin.state.Configuration;
import com.visiercorp.idea.rbplugin.state.DefaultState;
import com.visiercorp.idea.rbplugin.util.Page;
import com.intellij.notification.Notification;
import com.intellij.notification.Notifications;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.awt.RelativePoint;
import com.visiercorp.idea.rbplugin.reviewboard.model.*;
import com.visiercorp.idea.rbplugin.state.ConfigurationPersistance;
import com.visiercorp.idea.rbplugin.state.DefaultStatePersistance;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

/**
 * @author Ritesh
 */
public class ReviewDataProvider {
    private ReviewBoardClient client;
    private static Map<Project, ReviewDataProvider> reviewDataProviderMap = new WeakHashMap<>();

    public static ReviewDataProvider getInstance(Project project) {
        Configuration configuration = getConfiguration(project);

        if (!reviewDataProviderMap.containsKey(project)) {
            ReviewBoardClient client = new ReviewBoardClient(configuration.url, configuration.username, configuration.password);
            reviewDataProviderMap.put(project, new ReviewDataProvider(client));
        }
        return reviewDataProviderMap.get(project);
    }

    public static void reset(){
        reviewDataProviderMap.clear();
    }

    public static Configuration getConfiguration(final Project project) {
        Configuration state = ConfigurationPersistance.getInstance(project).getState();
        if (state == null || StringUtils.isEmpty(state.url) || StringUtils.isEmpty(state.username) || StringUtils.isEmpty(state.password)) {
            throw new InvalidConfigurationException("Review board not configured properly");
        }
        return state;
    }

    public static ReviewDataProvider getMockInstance() {
        return new ReviewDataProvider(new ReviewBoardClient("", "", ""));
    }

    public static boolean isConfiguredProperly(Project project) {
        Configuration state = ConfigurationPersistance.getInstance(project).getState();
        return state != null && !StringUtils.isEmpty(state.url) && !StringUtils.isEmpty(state.username) && !StringUtils.isEmpty(state.password);
    }

    public static boolean isConnected(Project project) {
        try {
            Configuration state = ConfigurationPersistance.getInstance(project).getState();
            ReviewDataProvider.getMockInstance().testConnection(state.url, state.username, state.password);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static DefaultState getDefaultState(Project project) {
        DefaultState state = DefaultStatePersistance.getInstance(project).getState();
        if (state == null) state = new DefaultState();
        return state;
    }

    private Reference<List<Repository>> repositoriesCache;

    private ReviewDataProvider(ReviewBoardClient client) {
        this.client = client;
    }

    public String reviewBoardUrl(Project project) {
        return getConfiguration(project).url;
    }

    public String reviewUrl(Project project, Review review) {
        return reviewBoardUrl(project) + "/r/" + review.id + "/";
    }

    public RBGroupList groups(String q) throws URISyntaxException, IOException {
        return client.groupsApi(q ,10);
    }

    public RBGroupList getGroups() throws URISyntaxException, IOException {
        return client.getGroupsApi();
    }

    public RBUserList groupUsers(@NotNull String q) throws URISyntaxException, IOException {
        return client.groupUsersApi(q);
    }

    public RBUserList users(String q) throws URISyntaxException, IOException {
        return client.usersApi(q);
    }

    public void updateReviewRequest(Review reviewRequest, String summary, String description, String targetPeople,
                                    String targetGroup) throws Exception {
        client.updateReviewApi(reviewRequest.id, description, summary, targetGroup, targetPeople, true);
    }


    public void createReviewRequest(String summary, String description, String targetPeople, String targetGroup,
                                    String repositoryId, String diffContent) throws Exception {
        RBCreateReview reviewRequestApi = client.createReviewRequestApi(repositoryId);
        String reviewRequestId = String.valueOf(reviewRequestApi.review_request.id);
        client.draftDiffUploadApi(reviewRequestId, diffContent, "/");
        client.updateReviewApi(reviewRequestId, description, summary, targetGroup, targetPeople, true);
    }

    public void discardedReviewRequest(Review reviewRequest) throws Exception {
        client.updateReviewRequestStatus(reviewRequest.id, "discarded");
    }

    public void submittedReviewRequest(Review reviewRequest) throws Exception {
        client.updateReviewRequestStatus(reviewRequest.id, "submitted");
    }

    public static void saveDefaultState(Project project, DefaultState defaultState) {
        DefaultStatePersistance.getInstance(project).loadState(defaultState);
    }

    public void testConnection(String url, String username, String password) throws Exception {
        client.testConnection(url, username, password);
    }

    public interface Progress {
        void progress(String text, float percentage);
    }

    public Page<Review> listReviews(String fromUser, String toUser, String toMe, String toReviewGroup, String status, String repositoryId, int start, int count) throws Exception {
        List<Review> reviews = new ArrayList<>();
        RBReviewRequestList reviewRequestList = client.reviewRequestListApi(fromUser, toUser, toMe, toReviewGroup, status, repositoryId, start, count);
        for (RBReviewRequestList.ReviewRequest request : reviewRequestList.review_requests) {
            String[] targetPeople = new String[request.target_people.length];
            for (int i = 0; i < targetPeople.length; i++) targetPeople[i] = request.target_people[i].title;

            String[] targetGroups = new String[request.target_groups.length];
            for (int i = 0; i < targetGroups.length; i++) targetGroups[i] = request.target_groups[i].title;

            Review.Builder reviewBuilder = new Review.Builder()
                    .id(request.id)
                    .summary(request.summary)
                    .description(request.description)
                    .branch(request.branch)
                    .lastUpdated(request.last_updated)
                    .status(request.status)
                    .targetPeople(targetPeople)
                    .issueOpenCount(request.issue_open_count);
            if (request.links != null && request.links.submitter != null)
                reviewBuilder.submitter(request.links.submitter.title);
            if (request.links != null && request.links.repository != null)
                reviewBuilder.repository(request.links.repository.title);

            Review review = reviewBuilder.targetGroups(targetGroups)
                    .build();
            reviews.add(review);
        }
        return new Page<>(reviews, start, count, reviewRequestList.total_results);
    }

    public void shipIt(final Review reviewRequest) throws Exception {
        final RBReview review = client.createReviewApi(reviewRequest.id, true);
        client.updateReviewApi(reviewRequest.id, String.valueOf(review.review.id), true, null, null);
    }

    public void createRepliesAndComments(final Review reviewRequest, final List<Review.File.Comment> comments, final List<Review.File.Comment> replyComments, String reviewComment,
                                         final Progress progress) throws Exception {
        if (replyComments.size() > 0) createReplies(reviewRequest, replyComments, progress);
        if (comments.size() > 0) createReview(reviewRequest, comments, reviewComment, progress);
    }

    private void createReview(final Review reviewRequest, final List<Review.File.Comment> comments, String reviewComment,
                              final Progress progress) throws Exception {
        final RBReview review = client.createReviewApi(reviewRequest.id, null);
        final MutableFloat progressF = new MutableFloat(0f);
        for (final Review.File.Comment comment : comments) {
            progress.progress("Updating comment", progressF.floatValue());
            client.createDiffComment(reviewRequest.id, String.valueOf(review.review.id),
                    comment.file.fileId, comment.firstLine, comment.numberOfLines, comment.text, comment.issueOpened);

            progressF.setValue(progressF.floatValue() + 1.0f / (comments.size() - 1));
        }
        progress.progress("Making review public", progressF.floatValue());
        client.updateReviewApi(reviewRequest.id, String.valueOf(review.review.id), true, reviewComment, null);
        progress.progress("Review Completed", 1);
    }

    private void createReplies(final Review reviewRequest, final List<Review.File.Comment> comments,
                                       final Progress progress) throws Exception {
        final MutableFloat progressF = new MutableFloat(0f);

        Map<String, RBReply> replies = new HashMap<>();
        List<String> repliedToIDs = new ArrayList<>();
        List<Review.File.Comment> commentsToAddLater = new ArrayList<>();
        List<Review.File.Comment> commentsToAddNow = comments;
        int numComments = comments.size();
        while (commentsToAddNow.size() > 0) {
            for (final Review.File.Comment comment : commentsToAddNow) {
                progress.progress("Updating comment", progressF.floatValue());
                RBReply reply;
                if (replies.containsKey(comment.reviewId)) {
                    reply = replies.get(comment.reviewId);
                } else {
                    reply = client.createReplyApi(reviewRequest.id, comment.reviewId);
                    replies.put(comment.reviewId, reply);
                }
                if (!repliedToIDs.contains(comment.replyToCommentId)) {
                    repliedToIDs.add(comment.replyToCommentId);
                    client.createDiffCommentReply(reviewRequest.id, comment.reviewId, String.valueOf(reply.reply.id), comment.replyToCommentId, comment.text);
                    progressF.setValue(progressF.floatValue() + 1.0f / (numComments - 1));
                } else {
                    commentsToAddLater.add(comment);
                }
            }
            if (!replies.isEmpty()) {
                progress.progress("Making replies public", progressF.floatValue());
                for (Map.Entry<String, RBReply> replyEntry : replies.entrySet()) {
                    client.updateReplyApi(reviewRequest.id, String.valueOf(replyEntry.getKey()), String.valueOf(replyEntry.getValue().reply.id), true);
                }
            }
            replies.clear();
            repliedToIDs.clear();
            commentsToAddNow = commentsToAddLater;
            commentsToAddLater = new ArrayList<>();
        }
        progress.progress("Published Replies", 1);
    }

    public List<Repository> repositories() throws Exception {
        if (repositoriesCache == null || repositoriesCache.get() == null) {
            final RBRepository repositories = client.repositories(200);
            List<Repository> result = new ArrayList<>();
            for (RBRepository.Repository repository : repositories.repositories) {
                result.add(new Repository(repository.id, repository.name));
            }
            repositoriesCache = new SoftReference<>(result);
            return result;
        }
        return repositoriesCache.get();
    }

    public int revisions(final Review review) throws Exception {
        final RBDiffList diffList = client.diffListApi(review.id);

        return diffList.total_results;
    }

    public List<Review.File> files(final Review review, final int revision, final int totalRevisions, final Progress progress, final Project project) throws Exception {
        List<Review.File> result = new ArrayList<>();
        final List<Future> futures = new CopyOnWriteArrayList<>();
        final MutableFloat progressF = new MutableFloat(0f);

        if (revision > 0) {

            RBFileDiff fileDiff = client.fileDiffApi(review.id, String.valueOf(revision));

            if (fileDiff.total_results == 200) {
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        String message = String.format("The selected review #%s: %s diff revision #%s may include changes to more than 200 files.\n" +
                                "Only the first 200 files are shown by this plugin.", review.id, review.summary, revision);

                        StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
                        JBPopupFactory.getInstance()
                                .createHtmlTextBalloonBuilder(message, MessageType.WARNING, null)
                                .setFadeoutTime(7500)
                                .createBalloon()
                                .show(RelativePoint.getNorthEastOf(statusBar.getComponent()),
                                        Balloon.Position.atRight);
                        Notifications.Bus.notify(new Notification("ReviewBoard", "Warning", message, NotificationType.WARNING));
                    }
                });
            }

            for (final RBFileDiff.File file : fileDiff.files) {
                final Review.File diffFile = new Review.File();

                diffFile.fileId = file.id;
                diffFile.srcFileName = file.source_file;
                diffFile.dstFileName = file.dest_file;
                diffFile.sourceRevision = file.source_revision;
                diffFile.revision = String.valueOf(revision);

                futures.add(ApplicationManager.getApplication().executeOnPooledThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                progress.progress("Loading file contents "
                                        + Paths.get(diffFile.srcFileName).getFileName(), progressF.floatValue());
                                diffFile.srcFileContents = client.contents(file.links.original_file.href);
                                progressF.setValue(progressF.floatValue() + 1.0f / totalRevisions);
                                progress.progress("Completed loading contents", progressF.floatValue());
                            }
                        }
                ));

                futures.add(ApplicationManager.getApplication().executeOnPooledThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                progress.progress("Loading file contents "
                                        + Paths.get(diffFile.dstFileName).getFileName(), progressF.floatValue());
                                diffFile.dstFileContents = client.contents(file.links.patched_file.href);
                                progressF.setValue(progressF.floatValue() + 1.0f / totalRevisions);
                                progress.progress("Completed loading contents", progressF.floatValue());
                            }
                        }
                ));
                result.add(diffFile);
            }
        }
        for (Future future : futures) future.get();
        return result;
    }

    public List<Review.File.Comment> comments(Review review, Review.File file) throws Exception {
        RBComments comments = client.diffCommentListApi(review.id, file.revision, file.fileId);
        List<Review.File.Comment> result = new ArrayList<>();
        int numOpenComments = 0;
        for (RBComments.DiffComment diff_comment : comments.diff_comments) {
            Review.File.Comment comment = new Review.File.Comment();
            comment.id = diff_comment.id;
            comment.text = diff_comment.text;
            comment.issueOpened = diff_comment.issue_opened;
            comment.issueStatus = diff_comment.issue_status;
            comment.firstLine = diff_comment.first_line;
            comment.numberOfLines = diff_comment.num_lines;
            comment.timestamp = diff_comment.timestamp;
            comment.user = diff_comment.links.user.title;
            comment.file = file;

            comment.reviewId = diff_comment.links.self.href.split("reviews/")[1].split("/")[0];
            comment.replyToCommentId = diff_comment.links.reply_to == null ? "null" : diff_comment.links.reply_to.href.split("diff-comments/")[1].replace("/","");

            result.add(comment);

            if (comment.issueStatus.equals("open")) {
                numOpenComments++;
            }
        }
        file.hasComments = comments.diff_comments.length > 0;
        file.hasOpenComments = numOpenComments > 0;

        return result;
    }

    public String patch(Review review, String revision) throws Exception {
        return client.getDiffApi(review.id, revision);
    }

}
