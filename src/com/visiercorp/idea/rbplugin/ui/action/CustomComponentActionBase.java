// Copyright (c) [2010-2016] Visier Solutions Inc. All rights reserved.

//
// Copyright © [2010-2016] Visier Solutions Inc. All rights reserved.
//
package com.visiercorp.idea.rbplugin.ui.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author ritesh
 */
public class CustomComponentActionBase extends DumbAwareAction implements CustomComponentAction {
    private JComponent component;

    public CustomComponentActionBase(JComponent component) {
        this.component = component;
    }

    @Override
    public JComponent createCustomComponent(Presentation presentation) {
        return component;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
    }
}
