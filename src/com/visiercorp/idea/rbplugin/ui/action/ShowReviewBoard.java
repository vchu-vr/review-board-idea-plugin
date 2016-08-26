// Copyright (c) [2010-2016] Visier Solutions Inc. All rights reserved.

package com.visiercorp.idea.rbplugin.ui.action;
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


import com.visiercorp.idea.rbplugin.reviewboard.ReviewDataProvider;
import com.visiercorp.idea.rbplugin.ui.ExceptionHandler;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.diff.impl.patch.UnifiedDiffWriter;
import com.intellij.openapi.diff.impl.patch.FilePatch;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.diff.ItemLatestState;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vfs.VirtualFile;
import com.visiercorp.idea.rbplugin.state.DefaultState;
import com.visiercorp.idea.rbplugin.ui.panels.DraftReviewPanel;
import com.intellij.openapi.vcs.VcsException;

import org.jetbrains.annotations.NotNull;

import org.zmlx.hg4idea.provider.HgDiffProvider;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.io.StringWriter;

/**
 * @author Ritesh
 */
public class ShowReviewBoard extends AnAction {
    final Logger LOG = Logger.getInstance(ShowReviewBoard.class);


    @Override
    public void actionPerformed(AnActionEvent e) {
        try {
            final Project project = e.getProject();
            final VirtualFile[] vFiles = e.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);
            StringBuffer output = new StringBuffer();
            if (vFiles == null || vFiles.length == 0) {
                Messages.showMessageDialog("No file to be review", "Alert", null);
                return;
            }
            String diffContent = "";
            HgDiffProvider hgDiffProvider = new HgDiffProvider(project);


            for (VirtualFile vf: vFiles) {
                VcsRevisionNumber vcsRevisionNumber = hgDiffProvider.getCurrentRevision(vf);
                ContentRevision contentRevision = hgDiffProvider.createFileContent(vcsRevisionNumber, vf);
                ItemLatestState itemLatestState = hgDiffProvider.getLastRevision(vf);
                ContentRevision cR = hgDiffProvider.createFileContent(itemLatestState.getNumber(), vf);
                Messages.showWarningDialog(cR.getContent(), "DIFF CONTENT CR");
                diffContent = contentRevision.getContent();
            }
            Messages.showWarningDialog(diffContent, "DIFF CONTENT");
            showCreateReviewPanel(project, diffContent);
        } catch (Exception ex) {
            ExceptionHandler.handleException(ex);
        }
    }

    private void showCreateReviewPanel(final Project project, final String diffContent) {
        DefaultState state = ReviewDataProvider.getDefaultState(project);
        final DraftReviewPanel draftReviewPanel =
                new DraftReviewPanel(project, "Create Review Request", null, null, state.targetPeople, state.targetGroup, state.repository);
        if (draftReviewPanel.showAndGet()) {
            ReviewDataProvider.saveDefaultState(project,
                    new DefaultState(draftReviewPanel.getRepository(), draftReviewPanel.getTargetPeople(),
                            draftReviewPanel.getTargetGroup()));

            createReviewRequest(project, diffContent, draftReviewPanel);
        }
    }

    private void createReviewRequest(final Project project, final String diffContent, final DraftReviewPanel draftReviewPanel) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Uploading Review") {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                try {
                    ReviewDataProvider.getInstance(project).
                            createReviewRequest(draftReviewPanel.getSummary(), draftReviewPanel.getDescription(),
                                    draftReviewPanel.getTargetPeople(), draftReviewPanel.getTargetGroup(),
                                    draftReviewPanel.getRepositoryId(), diffContent);
                } catch (Exception ex) {
                    ExceptionHandler.handleException(ex);
                }
            }
        });
    }

    private List<Change> getChanges(Project project, VirtualFile[] vFiles) {
        List<Change> changes = new ArrayList<Change>();
        final ChangeListManager changeListManager = ChangeListManager.getInstance(project);
        for (VirtualFile vf : vFiles) {
            if (vf != null) {
                vf.refresh(false, true);
                Change change = changeListManager.getChange(vf);
                if (change != null && change.getType().equals(Change.Type.NEW)) {
                    final ContentRevision afterRevision = change.getAfterRevision();
                    change = new Change(null, new ContentRevision() {
                        @Override
                        public String getContent() throws VcsException {
                            return afterRevision.getContent();
                        }

                        @NotNull
                        @Override
                        public FilePath getFile() {
                            return afterRevision.getFile();
                        }

                        @NotNull
                        @Override
                        public VcsRevisionNumber getRevisionNumber() {
                            return new VcsRevisionNumber.Int(0);
                        }
                    }, change.getFileStatus()
                    );
                }
                changes.add(change);
            }
        }
        return changes;
    }

    private List<FilePatch> buildPatch(Project project, List<Change> changes, String localRootDir, boolean b) {
        //      List<FilePatch> filePatches = IdeaTextPatchBuilder.buildPatch(project, changes, localRootDir, false);
//    List<FilePatch> filePatches = TextPatchBuilder.buildPatch(changes, localRootDir, false);
        Object result = null;
        try {//invoke the api in 10.x
            Messages.showWarningDialog("HELLO", "Hello");
            Class c = Class.forName("com.intellij.openapi.diff.impl.patch.IdeaTextPatchBuilder");
            Messages.showWarningDialog("HELLO1", "Hello1");
            Method buildPatchMethod = c.getMethod("buildPatch", Project.class, Collection.class, String.class, boolean.class);
            Messages.showWarningDialog("HELLO2", "Hello2");
            result = buildPatchMethod.invoke(null, project, changes, localRootDir, b);
            Messages.showWarningDialog(result.toString(), "Diff CONTENT");
        } catch (ClassNotFoundException e) {
            try {//API in 9.0x
                Class c = Class.forName("com.intellij.openapi.diff.impl.patch.TextPatchBuilder");
                Method buildPatchMethod = c.getMethod("buildPatch", Collection.class, String.class, boolean.class);
                result = buildPatchMethod.invoke(null, changes, localRootDir, b);
            } catch (Exception e1) {
                Messages.showErrorDialog("The current version doesn't support the review", "Not support 1");
                return null;
            }
        } catch (Exception e) {
            Messages.showErrorDialog("The current version doesn't support the review", "Not support 2");
        }
        if (result != null && result instanceof List) {
            return (List<FilePatch>) result;
        }
        return null;
    }

    private String generateDiff(Project project, VirtualFile[] vFiles) {
        try {
            List<Change> changes = getChanges(project, vFiles);
            List<FilePatch> filePatches = buildPatch(project, changes, "C:\\Users\\ali\\Workspace\\com.visiercorp.vserver", false);
            if (filePatches == null) {
                Messages.showWarningDialog("Create diff error", "Alter");
                return null;
            }
            StringWriter w = new StringWriter();
            UnifiedDiffWriter.write(project, filePatches, w, "\r\n", null);
            w.close();
            return w.toString();
        } catch (Exception e) {
            Messages.showWarningDialog("Svn is still in refresh. Please try again later.", "Alter");
            return null;
        }
    }

}
