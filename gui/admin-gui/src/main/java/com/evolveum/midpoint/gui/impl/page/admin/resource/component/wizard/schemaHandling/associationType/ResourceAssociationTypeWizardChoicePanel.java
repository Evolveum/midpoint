/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType;

import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.component.wizard.TileEnum;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;
import com.evolveum.midpoint.gui.impl.component.tile.WizardGuideTilePanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.ResourceWizardChoicePanel;
import com.evolveum.midpoint.gui.impl.util.GuiDisplayNameUtil;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationTypeDefinitionType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.jetbrains.annotations.NotNull;

public abstract class ResourceAssociationTypeWizardChoicePanel
        extends ResourceWizardChoicePanel<ResourceAssociationTypeWizardChoicePanel.ResourceAssociationTypePreviewTileType> {

    private final WizardPanelHelper<ShadowAssociationTypeDefinitionType, ResourceDetailsModel> helper;

    public ResourceAssociationTypeWizardChoicePanel(
            String id,
            @NotNull WizardPanelHelper<ShadowAssociationTypeDefinitionType, ResourceDetailsModel> helper) {
        super(id, helper.getDetailsModel(), ResourceAssociationTypePreviewTileType.class);
        this.helper = helper;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        add(AttributeAppender.append("class", "col-xxl-8 col-10 choice-tiles-container-8 gap-3 m-auto"));
    }

    public enum ResourceAssociationTypePreviewTileType implements TileEnum {

        BASIC_ATTRIBUTES("fa fa-file-alt"),
        OBJECT_AND_SUBJECT("fa fa-arrows-left-right"),
        MAPPINGS("fa fa-random"),
        CORRELATION("fa fa-code-branch"),
        SYNCHRONIZATION("fa fa-sync");

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
    protected Component createTilePanel(String id, IModel<Tile<ResourceAssociationTypePreviewTileType>> tileModel) {
        return new WizardGuideTilePanel<>(id, tileModel) {

            private @NotNull Boolean getDescription() {
                return StringUtils.isNotEmpty(tileModel.getObject().getDescription());
            }

            @Override
            protected void onClick(AjaxRequestTarget target) {
                if (isLocked()) {
                    return;
                }

                Tile<ResourceAssociationTypePreviewTileType> tile = tileModel.getObject();
                onTileClick(tile.getValue(), target);
            }

            @Override
            protected IModel<Badge> getBadgeModel() {
                ResourceAssociationTypePreviewTileType tile = tileModel.getObject().getValue();
                ResourceGuideAssociationTypeTileState state =
                        ResourceGuideAssociationTypeTileState.computeState(tile, getValueModel());
                return state.badgeModel(ResourceAssociationTypeWizardChoicePanel.this);
            }

            @Override
            protected IModel<String> getDescriptionTooltipModel() {
                ResourceAssociationTypePreviewTileType tile = tileModel.getObject().getValue();

                PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType> wrapper = getValueModel().getObject();
                ShadowAssociationTypeDefinitionType real = wrapper != null ? wrapper.getRealValue() : null;
                if (real == null) {
                    return null;
                }

                String key = ResourceGuideAssociationTypeTileState.getTooltipKey(tile, real);
                return key != null
                        ? ResourceAssociationTypeWizardChoicePanel.this.getPageBase().createStringResource(key)
                        : null;
            }

            @Override
            protected boolean isLocked() {
                ResourceAssociationTypePreviewTileType tile = tileModel.getObject().getValue();
                return ResourceGuideAssociationTypeTileState.computeState(tile, getValueModel()).isLocked();
            }

            @Override
            protected VisibleEnableBehaviour getDescriptionBehaviour() {
                return new VisibleBehaviour(this::getDescription);
            }
        };
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
                if (getValueModel() == null
                        || getValueModel().getObject() == null
                        || getValueModel().getObject().getRealValue() == null) {
                    return getPageBase()
                            .createStringResource("ResourceAssociationTypeWizardChoicePanel.breadcrumb")
                            .getString();
                }

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
}
