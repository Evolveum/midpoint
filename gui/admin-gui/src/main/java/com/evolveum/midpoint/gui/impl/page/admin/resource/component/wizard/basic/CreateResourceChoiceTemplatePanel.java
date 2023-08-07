/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic;

import com.evolveum.midpoint.gui.impl.component.tile.Tile;
import com.evolveum.midpoint.gui.impl.component.wizard.EnumWizardChoicePanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.List;

public class CreateResourceChoiceTemplatePanel extends EnumWizardChoicePanel<ResourceTemplate.TemplateType, ResourceDetailsModel> {

    private Model<ResourceTemplate.TemplateType> templateType;

    public CreateResourceChoiceTemplatePanel(String id, ResourceDetailsModel resourceModel, Model<ResourceTemplate.TemplateType> templateType) {
        super(id, resourceModel, ResourceTemplate.TemplateType.class);
        this.templateType = templateType;
    }

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        return getPageBase().createStringResource("CreateResourceChoiceTemplatePanel.text");
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("CreateResourceChoiceTemplatePanel.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("CreateResourceChoiceTemplatePanel.subText");
    }

    @Override
    protected void addDefaultTile(List<Tile<ResourceTemplate.TemplateType>> list) {
    }

    @Override
    protected QName getObjectType() {
        return null;
    }

    @Override
    protected final void onTileClickPerformed(ResourceTemplate.TemplateType value, AjaxRequestTarget target) {
        templateType.setObject(value);
        onClickTile(target);
    }

    protected void onClickTile(AjaxRequestTarget target) {
    }

    @Override
    protected boolean isExitButtonVisible() {
        return false;
    }
}
