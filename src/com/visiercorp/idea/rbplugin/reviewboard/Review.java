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

import org.jetbrains.annotations.NotNull;

import java.util.Date;

/**
 * @author Ritesh
 */
public class Review {
    public String id;
    public String summary;
    public String branch;
    public Date lastUpdated;
    public String status;
    public String[] targetPeople;
    public String submitter;
    public String repository;
    public String[] targetGroups;
    public String description;
    public int issueOpenCount;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Review review = (Review) o;

        return id.equals(review.id);

    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public static class File {
        public String fileId;
        public String srcFileName;
        public String dstFileName;
        public String srcFileContents;
        public String dstFileContents;
        public String sourceRevision;
        public String revision;
        public boolean hasComments;
        public boolean hasOpenComments;



        public static class Comment implements Comparable<Comment>{
            public boolean issueOpened;
            public int numberOfLines;
            public Date timestamp;
            public String id;
            public String issueStatus; // one of "" (issueOpened is false), "dropped", "open", "resolved"
            public String text;
            public int firstLine;
            public String user;
            public File file;
            public String reviewId; // id of the review, not review request
            public String replyToCommentId;

            @Override
            public int compareTo(@NotNull Comment o) {
                return Integer.compare(numberOfLines, o.numberOfLines);
//                if (i != 0) return i;
//
//                if (timestamp != null && o.timestamp != null) {
//                    return timestamp.compareTo(o.timestamp);
//                } else if (timestamp == null && o.timestamp == null) {
//                    return 0;
//                } else if (timestamp == null) {
//                    return new Date().compareTo(o.timestamp);
//                } else {
//                    return timestamp.compareTo(new Date());
//                }

            }
        }
    }

    public static final class Builder {
        private Review review = new Review();

        public Builder() {
        }

        public Builder id(String id) {
            review.id = id;
            return this;
        }

        public Builder summary(String summary) {
            review.summary = summary;
            return this;
        }

        public Builder branch(String branch) {
            review.branch = branch;
            return this;
        }

        public Builder lastUpdated(Date lastUpdated) {
            review.lastUpdated = lastUpdated;
            return this;
        }

        public Builder status(String status) {
            review.status = status;
            return this;
        }

        public Builder targetPeople(String[] targetPeople) {
            review.targetPeople = targetPeople;
            return this;
        }

        public Builder submitter(String submitter) {
            review.submitter = submitter;
            return this;
        }

        public Builder repository(String repository) {
            review.repository = repository;
            return this;
        }

        public Builder issueOpenCount(int openIssueCount) {
            review.issueOpenCount = openIssueCount;
            return this;
        }

        public Review build() {
            return review;
        }


        public Builder targetGroups(String[] targetGroups) {
            review.targetGroups = targetGroups;
            return this;
        }

        public Builder description(String description) {
            review.description = description;
            return this;
        }
    }
}
