/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.schema.component.wizard.basic;

import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.wizard.TileEnum;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.impl.component.tile.Tile;
import com.evolveum.midpoint.gui.impl.component.wizard.EnumWizardChoicePanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;

public abstract class CreateSchemaChoicePanel extends EnumWizardChoicePanel<CreateSchemaChoicePanel.SchemaEnumType, AssignmentHolderDetailsModel<SchemaType>> {

    public enum SchemaEnumType implements TileEnum {
        ADD_TO_EXISTING_SCHEMA(GuiStyleConstants.CLASS_OBJECT_SCHEMA_TEMPLATE_ICON),
        NEW_SCHEMA_TYPE("fa fa-plus");

        private final String icon;

        SchemaEnumType(String icon) {
            this.icon = icon;
        }

        @Override
        public String getIcon() {
            return icon;
        }
    }

    public CreateSchemaChoicePanel(
            String id, AssignmentHolderDetailsModel<SchemaType> assignmentHolderDetailsModel) {
        super(id, assignmentHolderDetailsModel, SchemaEnumType.class);
    }

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        return getPageBase().createStringResource("CreateSchemaChoicePanel.text");
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("CreateSchemaChoicePanel.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("CreateSchemaChoicePanel.subText");
    }

    @Override
    protected void addDefaultTile(List<Tile<SchemaEnumType>> list) {
    }

    @Override
    protected QName getObjectType() {
        return null;
    }

    @Override
    protected boolean isExitButtonVisible() {
        return false;
    }
}
