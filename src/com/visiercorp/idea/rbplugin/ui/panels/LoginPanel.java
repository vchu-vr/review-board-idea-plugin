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

import javax.swing.*;
import java.awt.event.ActionListener;

/**
 * @author Ritesh
 */
public class LoginPanel {
    private JPanel panel;
    private JTextField url;
    private JPasswordField password;
    private JTextField username;
    private JButton testConnection;
    private JLabel message;

    public LoginPanel() {
    }

    public void addActionListener(ActionListener l) {
        testConnection.addActionListener(l);
    }

    public JComponent getPanel() {
        return panel;
    }

    public String getUrl() {
        return url.getText().trim();
    }

    public String getUsername() {
        return username.getText().trim();
    }

    public String getPassword() {
        return String.valueOf(password.getPassword());
    }

    public void setUrl(String url) {
        this.url.setText(url);
    }

    public void setUsername(String username) {
        this.username.setText(username);
    }

    public void setPassword(String password) {
        this.password.setText(password);
    }

    public void setMessage(String message) {
        this.message.setText(message);
    }
}
