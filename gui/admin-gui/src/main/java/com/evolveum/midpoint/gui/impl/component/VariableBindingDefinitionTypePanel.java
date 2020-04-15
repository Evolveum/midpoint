/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.path.ItemPathDto;
import com.evolveum.midpoint.gui.api.component.path.ItemPathPanel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.VariableBindingDefinitionType;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

public class VariableBindingDefinitionTypePanel extends BasePanel<VariableBindingDefinitionType> {

    private static final String ID_PATH_LABEL = "pathLabel";
    private static final String ID_PATH_PANEL = "pathPanel";

    public VariableBindingDefinitionTypePanel(String id, IModel<VariableBindingDefinitionType> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        ItemPathPanel pathPanel = new ItemPathPanel(ID_PATH_PANEL, createPathModel()) {

            @Override
            protected void onUpdate(ItemPathDto itemPathDto) {
                ItemPath newPath = getModelObject().toItemPath();
                ItemPathType newPathtype = null;
                if (newPath != null) {
                    newPathtype = new ItemPathType(newPath);
                }

                VariableBindingDefinitionTypePanel.this.getModelObject().setPath(newPathtype);
            }
        };
        pathPanel.setOutputMarkupId(true);
        add(pathPanel);

    }

    private ItemPathDto createPathModel() {
        VariableBindingDefinitionType variable = getModelObject();
        if (variable == null) {
            return new ItemPathDto();
        }
        return new ItemPathDto(variable.getPath());
    }
}
