/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;
import com.evolveum.midpoint.gui.impl.component.tile.WizardGuideTilePanel;
import com.evolveum.midpoint.gui.impl.component.wizard.EnumWizardChoicePanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.List;

public class CreateResourceChoiceTemplatePanel extends EnumWizardChoicePanel<ResourceTemplate.TemplateType, ResourceDetailsModel> {

    private static final Trace LOGGER = TraceManager.getTrace(CreateResourceChoiceTemplatePanel.class);

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
    protected Component createTilePanel(String id, IModel<Tile<ResourceTemplate.TemplateType>> tileModel) {
        return new WizardGuideTilePanel<>(id, tileModel) {

            @Override
            protected boolean isLocked() {
                ResourceTemplate.TemplateType value = tileModel.getObject().getValue();
                if (value == null) {
                    return false;
                }

                if (value != ResourceTemplate.TemplateType.INHERIT_TEMPLATE
                        && value != ResourceTemplate.TemplateType.COPY_FROM_TEMPLATE) {
                    return false;
                }
                return !hasAnyResourceTemplates();
            }

            @Override
            protected IModel<String> getDescriptionTooltipModel() {
                PageBase pageBase = CreateResourceChoiceTemplatePanel.this.getPageBase();
                return isLocked()
                        ? pageBase.createStringResource("CreateResourceChoiceTemplatePanel.noTemplates")
                        : null;
            }

            @Override
            protected void onClick(AjaxRequestTarget target) {
                if (isLocked()) {
                    return;
                }
                onTileClick(tileModel.getObject().getValue(), target);
            }
        };
    }

    private boolean hasAnyResourceTemplates() {
        try {
            PageBase pageBase = CreateResourceChoiceTemplatePanel.this.getPageBase();
            ObjectQuery query = PrismContext.get().queryFor(ResourceType.class)
                    .item(ResourceType.F_TEMPLATE)
                    .eq(true)
                    .build();

            Task task = pageBase.createSimpleTask("checkResourceTemplates");
            OperationResult result = task.getResult();
            Integer count = pageBase.getModelService()
                    .countObjects(ResourceType.class, query, null, task, result);

            return count != null && count > 0;
        } catch (Exception e) {
            LOGGER.error("Failed to check if there are any resource templates: {}", e.getMessage(), e);
            return false;
        }
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
