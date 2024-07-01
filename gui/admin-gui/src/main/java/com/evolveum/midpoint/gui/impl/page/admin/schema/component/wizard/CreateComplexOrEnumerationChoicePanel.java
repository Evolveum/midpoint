/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.schema.component.wizard;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.wizard.TileEnum;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;
import com.evolveum.midpoint.gui.impl.component.wizard.EnumWizardChoicePanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaType;

import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.List;

public abstract class CreateComplexOrEnumerationChoicePanel extends EnumWizardChoicePanel<CreateComplexOrEnumerationChoicePanel.TypeEnum, AssignmentHolderDetailsModel<SchemaType>> {

    public enum TypeEnum implements TileEnum {
        COMPLEX_TYPE(
                GuiStyleConstants.CLASS_SCHEMA_COMPLEX_TYPE_ICON,
                LocalizationUtil.translate("CreateComplexOrEnumerationChoicePanel.complexType.help")),
        ENUMERATION_TYPE(
                GuiStyleConstants.CLASS_SCHEMA_ENUM_TYPE_ICON,
                LocalizationUtil.translate("CreateComplexOrEnumerationChoicePanel.EnumerationType.help"));

        private final String icon;
        private final String description;

        TypeEnum(String icon, String description) {
            this.icon = icon;
            this.description = description;
        }

        @Override
        public String getIcon() {
            return icon;
        }

        @Override
        public String getDescription() {
            return description;
        }
    }

    public CreateComplexOrEnumerationChoicePanel(
            String id, AssignmentHolderDetailsModel<SchemaType> assignmentHolderDetailsModel) {
        super(id, assignmentHolderDetailsModel, TypeEnum.class);
    }

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        return getPageBase().createStringResource("CreateComplexOrEnumerationChoicePanel.text");
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("CreateComplexOrEnumerationChoicePanel.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("CreateComplexOrEnumerationChoicePanel.subText");
    }

    @Override
    protected void addDefaultTile(List<Tile<TypeEnum>> list) {
    }

    @Override
    protected QName getObjectType() {
        return null;
    }
}
