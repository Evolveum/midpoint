/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.operation;

import java.io.Serial;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;

public class DebugLabel extends BasePanel<String> implements Popupable {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_TEXT = "label";

    public DebugLabel(String id, IModel<String> model) {
        super(id, model);
        initLayout();
    }

    private void initLayout() {
        Label label = new Label(ID_TEXT, getModel());
        label.setOutputMarkupId(true);
        add(label);
    }

    @Override
    public int getWidth() {
        return 50;
    }

    @Override
    public int getHeight() {
        return 50;
    }

    @Override
    public String getWidthUnit() {
        return "%";
    }

    @Override
    public String getHeightUnit() {
        return "%";
    }

    @Override
    public IModel<String> getTitle() {
        return null;
    }

    @Override
    public Component getContent() {
        return this;
    }
}
