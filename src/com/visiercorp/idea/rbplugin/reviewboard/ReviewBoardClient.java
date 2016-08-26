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

import com.visiercorp.idea.rbplugin.exception.InvalidCredentialException;
import com.visiercorp.idea.rbplugin.exception.ReviewBoardServerException;
import com.visiercorp.idea.rbplugin.util.HttpRequestBuilder;
import com.google.common.io.CharStreams;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.visiercorp.idea.rbplugin.reviewboard.model.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;

/**
 * @author Ritesh
 */
public class ReviewBoardClient {

    private static final String API = "api";
    private static final String REVIEW_REQUESTS = "review-requests";
    private static final String DIFFS = "diffs";
    private static final String FILES = "files";
    private static final String DIFF_COMMENTS = "diff-comments";
    private static final String DRAFT = "draft";
    private static final String REVIEWS = "reviews";
    private static final String REPLIES = "replies";
    private static final String AUTHORIZATION = "Authorization";
    private static final String REPOSITORIES = "repositories";
    private static final String GROUPS = "groups";
    private static final String USERS = "users";

    private static final String ACCEPT = "Accept";
    private static final String TEXT_XPATCH = "text/x-patch";

    private static final String ERRORCODE_LOGINFAILED = "104";

    private static final int MAX_RESULT_HARD_LIMIT = 200;

    private final String url;
    private final String userName;
    private final String password;

    public ReviewBoardClient(String url, String userName, String password) {
        this.url = url;
        this.userName = userName;
        this.password = password;
    }

    private String getAuthorizationHeader() {
        return getAuthorizationHeader(userName, password);
    }

    private String getAuthorizationHeader(String userName, String password) {
        return "Basic " + Base64.encodeBase64String((userName + ":" + password).getBytes());
    }

    private <T extends RBModel> T checkSuccess(T model) {
        if (!model.stat.equalsIgnoreCase("ok")) {
            //TODO : more error information in message
            if (model.err.code.equals(ERRORCODE_LOGINFAILED)) {
                throw new InvalidCredentialException(String.format("Server: %s, update your credentials and refresh the reviews.", model.err.msg));
            }
            String error = String.format("Server: %s, reason: %s, fields: %s, file: %s, revision: %s",
                    model.err.msg, model.reason, model.fields, model.file, model.revision);
            throw new ReviewBoardServerException(error);
        }
        return model;
    }

    public RBReviewRequestList reviewRequestListApi(String fromUser, String toUser, String toMe, String toReviewGroup, String status,
                                                    String repositoryId, long start, long count) throws URISyntaxException, IOException {
        HttpRequestBuilder requestBuilder = HttpRequestBuilder.get(url).route(API).route(REVIEW_REQUESTS).slash();
        if (toUser != null) requestBuilder.queryString("to-users", toUser);
        if (fromUser != null) requestBuilder.queryString("from-user", fromUser);
        if (toMe != null) requestBuilder.queryString("to-users-directly", toMe);
        if (!toReviewGroup.equals("none")) requestBuilder.queryString("to-groups", toReviewGroup);
        if (repositoryId != null) requestBuilder.queryString("repository", repositoryId);

        RBReviewRequestList result = requestBuilder.queryString("start", String.valueOf(start))
                .queryString("max-results", String.valueOf(count))
                .queryString("status", status)
                .header(AUTHORIZATION, getAuthorizationHeader())
                .asJson(RBReviewRequestList.class);
        return checkSuccess(result);
    }

    public RBDiffList diffListApi(String reviewRequestId) throws URISyntaxException, IOException {
        RBDiffList result = HttpRequestBuilder.get(url).route(API).route(REVIEW_REQUESTS).route(reviewRequestId)
                .route(DIFFS).slash().header(AUTHORIZATION, getAuthorizationHeader()).asJson(RBDiffList.class);
        return checkSuccess(result);
    }

    public RBFileDiff fileDiffApi(String reviewRequestId, String revision) throws URISyntaxException, IOException {
        RBFileDiff result = HttpRequestBuilder.get(url).route(API).route(REVIEW_REQUESTS)
                .route(reviewRequestId).route(DIFFS).route(revision).route(FILES).slash()
                .header(AUTHORIZATION, getAuthorizationHeader())
                .queryString("max-results", MAX_RESULT_HARD_LIMIT)
                .asJson(RBFileDiff.class);
        return checkSuccess(result);
    }

    public RBComments diffCommentListApi(String reviewRequestId, String revision, String fileId) throws URISyntaxException, IOException {
        RBComments result = HttpRequestBuilder.get(url).route(API).route(REVIEW_REQUESTS)
                .route(reviewRequestId).route(DIFFS).route(revision).route(FILES).route(fileId)
                .route(DIFF_COMMENTS).slash()
                .header(AUTHORIZATION, getAuthorizationHeader())
                .asJson(RBComments.class);
        return checkSuccess(result);
    }

    public RBReview createReviewApi(String reviewRequestId, Boolean shipIt) throws URISyntaxException, IOException {
        HttpRequestBuilder requestBuilder = HttpRequestBuilder.post(url).route(API).route(REVIEW_REQUESTS)
                .route(reviewRequestId).route(REVIEWS).slash()
                .header(AUTHORIZATION, getAuthorizationHeader());
        if (shipIt != null) requestBuilder.field("ship_it", shipIt);
        RBReview result = requestBuilder.asJson(RBReview.class);
        return checkSuccess(result);
    }

    public RBRepository repositories(int count) throws URISyntaxException, IOException {
        RBRepository result = HttpRequestBuilder.get(url).route(API).route(REPOSITORIES).slash()
                .header(AUTHORIZATION, getAuthorizationHeader())
                .queryString("max-results", count)
                .asJson(RBRepository.class);
        return checkSuccess(result);
    }

    public void createDiffComment(String reviewRequestId, String reviewId, String filediff_id,
                                  int first_line, int num_lines, String text, boolean issue_opened) throws URISyntaxException, IOException {
        RBModel result = HttpRequestBuilder.post(url).route(API).route(REVIEW_REQUESTS)
                .route(reviewRequestId).route(REVIEWS).route(reviewId).route(DIFF_COMMENTS).slash()
                .field("filediff_id", filediff_id)
                .field("first_line", first_line)
                .field("num_lines", num_lines)
                .field("issue_opened", issue_opened)
                .field("text", text)
                .header(AUTHORIZATION, getAuthorizationHeader())
                .asJson(RBModel.class);
        checkSuccess(result);
    }

    public void updateReviewApi(String reviewRequestId, String reviewId, boolean isPublic,
                                String body_top, String body_bottom) throws URISyntaxException, IOException {
        HttpRequestBuilder put = HttpRequestBuilder.put(url);
        put.route(API).route(REVIEW_REQUESTS)
                .route(reviewRequestId).route(REVIEWS).route(reviewId).slash()
                .header(AUTHORIZATION, getAuthorizationHeader())
                .field("public", isPublic);
        if (!StringUtils.isEmpty(body_bottom)) put.field("body_bottom", body_bottom);
        if (!StringUtils.isEmpty(body_top)) put.field("body_top", body_top);
        RBModel result = put.asJson(RBModel.class);
        checkSuccess(result);
    }

    public String contents(String href) {
        try {
            HttpRequestBase request = HttpRequestBuilder.get(href)
                    .header(AUTHORIZATION, getAuthorizationHeader())
                    .request();
            try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
                CloseableHttpResponse response = client.execute(request);

                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                    return null;
                } else {
                    return CharStreams.toString(new InputStreamReader(response.getEntity().getContent()));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public RBCreateReview createReviewRequestApi(String repositoryId) throws URISyntaxException, IOException {
        RBCreateReview result = HttpRequestBuilder.post(url).route(API).route(REVIEW_REQUESTS).slash()
                .header(AUTHORIZATION, getAuthorizationHeader())
                .field("repository", repositoryId)
                .asJson(RBCreateReview.class);
        return checkSuccess(result);
    }


    public RBModel updateReviewRequestStatus(String reviewRequestId, String status) throws URISyntaxException, IOException {
        RBModel result = HttpRequestBuilder.put(url).route(API).route(REVIEW_REQUESTS)
                .route(reviewRequestId).slash()
                .header(AUTHORIZATION, getAuthorizationHeader())
                .field("status", status)
                .asJson(RBCreateReview.class);
        return checkSuccess(result);
    }

    public RBModel draftDiffUploadApi(String reviewRequestId, String content, String basedir) throws URISyntaxException, IOException {
        RBModel model = HttpRequestBuilder.post(url).route(API).route(REVIEW_REQUESTS)
                .route(reviewRequestId).route(DIFFS).slash()
                .header(AUTHORIZATION, getAuthorizationHeader())
                .field("basedir", basedir)
                .file("path", "git.diff", content.getBytes(CharsetToolkit.UTF8_CHARSET))
                .asJson(RBModel.class);
        return checkSuccess(model);
    }

    public RBGroupList groupsApi(String q, int maxResults) throws URISyntaxException, IOException {
        RBGroupList result = HttpRequestBuilder.get(url).route(API).route(GROUPS).slash()
                .header(AUTHORIZATION, getAuthorizationHeader())
                .queryString("q", q)
                .queryString("max-results", maxResults)
                .asJson(RBGroupList.class);
        return checkSuccess(result);
    }

    public RBGroupList getGroupsApi() throws URISyntaxException, IOException {
        RBGroupList result = HttpRequestBuilder.get(url).route(API).route(GROUPS).slash()
                .header(AUTHORIZATION, getAuthorizationHeader())
                .queryString("max-results", MAX_RESULT_HARD_LIMIT)
                .asJson(RBGroupList.class);
        return checkSuccess(result);
    }

    public RBUserList groupUsersApi(String groupName) throws URISyntaxException, IOException {
        RBUserList result = HttpRequestBuilder.get(url).route(API).route(GROUPS).route(groupName).route(USERS).slash()
                .header(AUTHORIZATION, getAuthorizationHeader())
                .asJson(RBUserList.class);
        return checkSuccess(result);
    }

    public RBUserList usersApi(String q) throws URISyntaxException, IOException {
        RBUserList result = HttpRequestBuilder.get(url).route(API).route(USERS).slash()
                .header(AUTHORIZATION, getAuthorizationHeader())
                .queryString("q", q)
                .asJson(RBUserList.class);
        return checkSuccess(result);
    }


    public RBModel updateReviewApi(String reviewRequestId, String description, String summary, String targetGroups,
                                   String targetPeople, boolean isPublic) throws URISyntaxException, IOException {
        RBModel model = HttpRequestBuilder.post(url).route(API).route(REVIEW_REQUESTS)
                .route(reviewRequestId).route(DRAFT).slash()
                .header(AUTHORIZATION, getAuthorizationHeader())
                .field("summary", summary)
                .field("description", description)
                .field("target_groups", targetGroups)
                .field("target_people", targetPeople)
                .field("public", isPublic)
                .asJson(RBModel.class);
        return checkSuccess(model);
    }

    public RBReply createReplyApi(String reviewRequestId, String reviewId) throws URISyntaxException, IOException {
        HttpRequestBuilder requestBuilder = HttpRequestBuilder.post(url).route(API).route(REVIEW_REQUESTS)
                .route(reviewRequestId).route(REVIEWS).route(reviewId).route(REPLIES).slash()
                .header(AUTHORIZATION, getAuthorizationHeader());
        RBReply result = requestBuilder.asJson(RBReply.class);
        return checkSuccess(result);
    }

    public void updateReplyApi(String reviewRequestId, String reviewId, String replyId, boolean isPublic) throws URISyntaxException, IOException {
        HttpRequestBuilder put = HttpRequestBuilder.put(url);
        put.route(API).route(REVIEW_REQUESTS)
                .route(reviewRequestId).route(REVIEWS).route(reviewId).route(REPLIES).route(replyId).slash()
                .header(AUTHORIZATION, getAuthorizationHeader())
                .field("public", isPublic);
        RBModel result = put.asJson(RBModel.class);
        checkSuccess(result);
    }

    public void createDiffCommentReply(String reviewRequestId, String reviewId, String replyId, String replyToId, String text) throws URISyntaxException, IOException {
        RBModel result = HttpRequestBuilder.post(url).route(API).route(REVIEW_REQUESTS)
                .route(reviewRequestId).route(REVIEWS).route(reviewId).route(REPLIES).route(replyId).route(DIFF_COMMENTS).slash()
                .field("reply_to_id", replyToId)
                .field("text", text)
                .header(AUTHORIZATION, getAuthorizationHeader())
                .asJson(RBModel.class);
        checkSuccess(result);
    }

    // get patch from review board server
    public String getDiffApi(String requestId, String revision) throws URISyntaxException, IOException {
        String obj = HttpRequestBuilder.get(url).route(API).route(REVIEW_REQUESTS).route(requestId).route(DIFFS).route(revision).slash()
                .header(AUTHORIZATION, getAuthorizationHeader())
                .header(ACCEPT, TEXT_XPATCH)
                .asString();
        return obj;
    }

    public RBModel testConnection(String url, String username, String password) throws URISyntaxException, IOException {
        RBModel model = HttpRequestBuilder.get(url).route(API).slash()
                .header(AUTHORIZATION, getAuthorizationHeader(username, password)).asJson(RBModel.class);
        return checkSuccess(model);
    }
}
