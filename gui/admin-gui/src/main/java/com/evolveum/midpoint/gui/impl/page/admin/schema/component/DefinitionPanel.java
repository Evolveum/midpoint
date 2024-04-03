/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.schema.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.page.admin.schema.dto.ComplexTypeDefinitionDto;
import com.evolveum.midpoint.gui.impl.page.admin.schema.dto.DefinitionDto;

import com.evolveum.midpoint.gui.impl.page.admin.schema.dto.EnumerationTypeDefinitionDto;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

public class DefinitionPanel<T extends DefinitionDto> extends BasePanel<T> {

    public DefinitionPanel(String id, IModel<T> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    protected void initLayout() {
       Label displayName = new Label("displayName", getModelObject().getDisplayName());
       add(displayName);

        Label displayOrder = new Label("displayOrder", getModelObject().getDisplayOrder());
        add(displayOrder);

    }
}
