/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.component.otp;

import java.io.Serial;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;

import com.evolveum.midpoint.gui.api.component.BasePanel;

public class ActionPanel extends BasePanel<Void> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_EDIT = "edit";
    private static final String ID_DELETE = "delete";

    public ActionPanel(String id) {
        super(id);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initLayout();
    }

    private void initLayout() {
        AjaxLink<?> editButton = new AjaxLink<>(ID_EDIT) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                onEditPerformed(target);
            }
        };
        add(editButton);

        AjaxLink<?> deleteButton = new AjaxLink<>(ID_DELETE) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                onDeletePerformed(target);
            }
        };
        add(deleteButton);
    }

    protected void onEditPerformed(AjaxRequestTarget target) {
        // intentionally left blank, to be overridden by the caller
    }

    protected void onDeletePerformed(AjaxRequestTarget target) {
        // intentionally left blank, to be overridden by the caller
    }
}
