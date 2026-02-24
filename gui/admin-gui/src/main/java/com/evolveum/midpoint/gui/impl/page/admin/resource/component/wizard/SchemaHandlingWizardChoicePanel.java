package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard;

import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.component.wizard.TileEnum;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;
import com.evolveum.midpoint.gui.impl.component.tile.WizardGuideTilePanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.session.ResourceWizardStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

public abstract class SchemaHandlingWizardChoicePanel
        extends ResourceWizardChoicePanel<SchemaHandlingWizardChoicePanel.PreviewTileType> {

    public SchemaHandlingWizardChoicePanel(String id, ResourceDetailsModel resourceModel) {
        super(id, resourceModel, PreviewTileType.class);
    }

    public enum PreviewTileType implements TileEnum {
        PREVIEW_DATA("fa fa-magnifying-glass"),
        CONFIGURE_OBJECT_TYPES("fa fa-object-group"),
        CONFIGURE_ASSOCIATION_TYPES("fa fa-code-compare");

        private final String icon;

        PreviewTileType(String icon) {
            this.icon = icon;
        }

        @Override
        public String getIcon() {
            return icon;
        }
    }

    @Override
    protected Component createTilePanel(String id, IModel<Tile<PreviewTileType>> tileModel) {
        return new WizardGuideTilePanel<>(id, tileModel) {

            private @NotNull Boolean hasDescription() {
                return StringUtils.isNotEmpty(tileModel.getObject().getDescription());
            }

            @Override
            protected void onClick(AjaxRequestTarget target) {
                if (isLocked()) {
                    return;
                }
                Tile<PreviewTileType> tile = tileModel.getObject();
                onTileClick(tile.getValue(), target);
            }

            @Override
            protected IModel<Badge> getBadgeModel() {
                ResourceType resource = getAssignmentHolderDetailsModel().getObjectType();
                ResourceWizardStorage storage = SchemaHandlingWizardChoicePanel.this.getPageBase()
                        .getSessionStorage().getResourceWizardStorage();

                PreviewTileType tile = tileModel.getObject().getValue();
                ResourceGuideSchemaHandlingWizardTileState state =
                        ResourceGuideSchemaHandlingWizardTileState.computeState(tile, resource, storage);

                return state.badgeModel(SchemaHandlingWizardChoicePanel.this);
            }

            @Override
            protected IModel<String> getDescriptionTooltipModel() {
                PreviewTileType value = tileModel.getObject().getValue();

                if(isLocked() &&  value == PreviewTileType.CONFIGURE_ASSOCIATION_TYPES) {
                    return SchemaHandlingWizardChoicePanel.this.getPageBase()
                            .createStringResource("ResourceWizardPreviewPanel.noObjectTypes");
                }
                return null;
            }

            @Override
            protected boolean isLocked() {
                ResourceType resource = getAssignmentHolderDetailsModel().getObjectType();
                ResourceWizardStorage storage = SchemaHandlingWizardChoicePanel.this.getPageBase()
                        .getSessionStorage().getResourceWizardStorage();

                PreviewTileType tile = tileModel.getObject().getValue();
                return ResourceGuideSchemaHandlingWizardTileState
                        .computeState(tile, resource, storage)
                        .isLocked();
            }

            @Override
            protected VisibleEnableBehaviour getDescriptionBehaviour() {
                return new VisibleBehaviour(this::hasDescription);
            }
        };
    }

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        return Model.of(getResourceName());
    }

    private String getResourceName() {
        return WebComponentUtil.getDisplayNameOrName(
                getAssignmentHolderDetailsModel().getObjectWrapper().getObject());
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("ResourceWizardPreviewPanel.subText", getResourceName());
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("ResourceWizardPreviewPanel.text", getResourceName());
    }

    // keep your existing abstract handler
    protected abstract void onTileClickPerformed(@NotNull PreviewTileType value, AjaxRequestTarget target);
}
