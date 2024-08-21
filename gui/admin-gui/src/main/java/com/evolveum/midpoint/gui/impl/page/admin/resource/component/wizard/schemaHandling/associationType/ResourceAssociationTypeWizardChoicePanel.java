/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType;

import com.evolveum.midpoint.gui.impl.util.AssociationChildWrapperUtil;
import com.evolveum.midpoint.schema.processor.CompleteResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;

import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeIdentificationType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.wizard.TileEnum;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.ResourceWizardChoicePanel;
import com.evolveum.midpoint.gui.impl.util.GuiDisplayNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationTypeDefinitionType;

import java.util.List;

public abstract class ResourceAssociationTypeWizardChoicePanel
        extends ResourceWizardChoicePanel<ResourceAssociationTypeWizardChoicePanel.ResourceAssociationTypePreviewTileType> {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceAssociationTypeWizardChoicePanel.class);

    private final WizardPanelHelper<ShadowAssociationTypeDefinitionType, ResourceDetailsModel> helper;

    public ResourceAssociationTypeWizardChoicePanel(
            String id,
            WizardPanelHelper<ShadowAssociationTypeDefinitionType, ResourceDetailsModel> helper) {
        super(id, helper.getDetailsModel(), ResourceAssociationTypePreviewTileType.class);
        this.helper = helper;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        add(AttributeAppender.append("class", "col-xxl-8 col-10 gap-3 m-auto"));
    }

    public enum ResourceAssociationTypePreviewTileType implements TileEnum {

        BASIC("fa fa-circle"),
        SUBJECT("fa fa-square"),
        OBJECT("fa-regular fa-square");

        private final String icon;

        ResourceAssociationTypePreviewTileType(String icon) {
            this.icon = icon;
        }

        @Override
        public String getIcon() {
            return icon;
        }
    }

    @Override
    protected boolean addDefaultTile() {
        return false;
    }

    @Override
    protected IModel<String> getExitLabel() {
        return getPageBase().createStringResource("ResourceAssociationTypeWizardChoicePanel.exit");
    }

    @Override
    protected boolean isExitButtonVisible() {
        return false;
    }

    @Override
    protected boolean isBackButtonVisible() {
        return true;
    }

    protected IModel<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> getValueModel() {
        return helper.getValueModel();
    }

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        return new LoadableDetachableModel<>() {
            @Override
            protected String load() {
                return GuiDisplayNameUtil.getDisplayName(getValueModel().getObject().getRealValue());
            }
        };
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("ResourceAssociationTypeWizardChoicePanel.subText");
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("ResourceAssociationTypeWizardChoicePanel.text");
    }

    @Override
    protected String getDescriptionForTile(ResourceAssociationTypePreviewTileType type) {
        List<ResourceObjectTypeIdentificationType> objectTypes = List.of();
        if (ResourceAssociationTypePreviewTileType.SUBJECT == type) {
            objectTypes =
                    AssociationChildWrapperUtil.getObjectTypesOfSubject(getValueModel().getObject());
        } else if (ResourceAssociationTypePreviewTileType.OBJECT == type) {
            objectTypes =
                    AssociationChildWrapperUtil.getObjectTypesOfObject(getValueModel().getObject());
        }

        if (!objectTypes.isEmpty()) {
            try {
                CompleteResourceSchema schema = getAssignmentHolderDetailsModel().getRefinedSchema();
                return StringUtils.join(
                        objectTypes.stream()
                                .map(objectType ->
                                        GuiDisplayNameUtil.getDisplayName(
                                                schema.getObjectTypeDefinition(
                                                                ResourceObjectTypeIdentification.of(objectType))
                                                        .getDefinitionBean()))
                                .toList(),
                        ", ");
            } catch (CommonException e) {
                LOGGER.error("Couldn't load resource schema");
            }
        }

        return super.getDescriptionForTile(type);
    }
}
