// Copyright (c) [2010-2016] Visier Solutions Inc. All rights reserved.

package com.visiercorp.idea.rbplugin.ui.panels;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventListener;

/**
 * Created by jhoang on 04/08/2016.
 */
public class CommentsOptionMenu<T> extends JPopupMenu {

    private DeleteCommentListener listener;

    public CommentsOptionMenu(boolean showDeleteOption, boolean showReplyOption) {

        if (showDeleteOption) {
            JMenuItem deleteOption = new JMenuItem("Delete");
            deleteOption.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    CommentsOptionMenu.this.listener.onDelete();
                }
            });
            add(deleteOption);
        }
        if (showReplyOption) {
            JMenuItem replyOption = new JMenuItem("Add reply to this thread");
            replyOption.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    CommentsOptionMenu.this.listener.onReply();
                }
            });
            add(replyOption);
        }
    }

    public void setListener(DeleteCommentListener<T> listener) {
        this.listener = listener;
    }

    public interface DeleteCommentListener<T> extends EventListener {
        void onDelete();
        void onReply();
    }
}
