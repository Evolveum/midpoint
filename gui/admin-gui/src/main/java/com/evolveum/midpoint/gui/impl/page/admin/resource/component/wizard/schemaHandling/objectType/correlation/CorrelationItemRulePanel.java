/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.correlation;

import com.evolveum.midpoint.gui.api.component.BadgePanel;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormCorrelationItemPanel;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.gui.api.util.WebPrismUtil.setReadOnlyRecursively;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationStatusInfoUtils.extractEfficiencyFromSuggestedCorrelationItemWrapper;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils.getAiEfficiencyBadgeModel;

public class CorrelationItemRulePanel extends BasePanel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> implements Popupable {

    private static final String ID_PANEL = "panel";
    private static final String ID_TABLE = "table";

    private static final String ID_ALERT_CONTAINER = "containerAlert";
    private static final String ID_ALERT_ICON = "iconAlert";
    private static final String ID_ALERT_TITLE = "titleAlert";
    private static final String ID_ALERT_DESCRIPTION = "descriptionAlert";
    private static final String ID_ALERT_BADGE = "badgeAlert";

    IModel<StatusInfo<CorrelationSuggestionsType>> statusInfoModel = Model.of();
    IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> resourceObjectTypeDefinition;

    public CorrelationItemRulePanel(String id,
            IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> valueWrapperIModel,
            IModel<StatusInfo<CorrelationSuggestionsType>> statusInfoModel,
            IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> resourceObjectTypeDefinition) {
        super(id, valueWrapperIModel);
        this.statusInfoModel = statusInfoModel;
        this.resourceObjectTypeDefinition = resourceObjectTypeDefinition;
    }

    public CorrelationItemRulePanel(String id,
            IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> valueWrapperIModel,
            IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> resourceObjectTypeDefinition) {
        super(id, valueWrapperIModel);
        this.resourceObjectTypeDefinition = resourceObjectTypeDefinition;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initAlertInfoPanel();
        initLayout();
    }


    private void initAlertInfoPanel() {
        WebMarkupContainer infoPanel = new WebMarkupContainer(ID_ALERT_CONTAINER);
        infoPanel.setOutputMarkupId(true);
        infoPanel.add(new VisibleBehaviour(this::isSuggestionApplied));

        WebMarkupContainer icon = new WebMarkupContainer(ID_ALERT_ICON);
        icon.add(AttributeAppender.append("class", "fa fa-solid fa-wand-magic-sparkles text-purple"));
        infoPanel.add(icon);
        infoPanel.add(new Label(ID_ALERT_TITLE,
                createStringResource("CorrelationItemRefsTableWizardPanel.unconfirmed.suggestion")));
        infoPanel.add(new Label(ID_ALERT_DESCRIPTION,
                createStringResource("SmartCorrelationTilePanel.unconfirmed.suggestion.description")));

        String efficiency = extractEfficiencyFromSuggestedCorrelationItemWrapper(getModelObject());

        BadgePanel badge = new BadgePanel(ID_ALERT_BADGE,
                getAiEfficiencyBadgeModel(
                        createStringResource("SmartCorrelationTilePanel.unconfirmed.suggestion.efficiency",
                                efficiency).getString()));
        badge.setOutputMarkupId(true);
        infoPanel.add(badge);
        add(infoPanel);
    }

    private void initLayout() {
        IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> valueModel = getModel();

        ItemPanelSettings settings = new ItemPanelSettingsBuilder()
                .visibilityHandler((wrapper) -> {
                    ItemName itemName = wrapper.getPath().lastName();
                    return itemName.equivalent(ItemsSubCorrelatorType.F_DESCRIPTION)
                            || itemName.equivalent(ItemsSubCorrelatorType.F_NAME)
                            || itemName.equivalent(ItemsSubCorrelatorType.F_ENABLED)
                            || itemName.equivalent(ItemsSubCorrelatorType.F_COMPOSITION)
                            || itemName.equivalent(CorrelatorCompositionDefinitionType.F_IGNORE_IF_MATCHED_BY)
                            || itemName.equivalent(CorrelatorCompositionDefinitionType.F_TIER)
                            || itemName.equivalent(CorrelatorCompositionDefinitionType.F_WEIGHT)
                            ? ItemVisibility.AUTO
                            : ItemVisibility.HIDDEN;
                })
                .isRemoveButtonVisible(false)
                .build();

        if (isSuggestionApplied()) {
            setReadOnlyRecursively(valueModel.getObject());
        }

        valueModel.getObject().setShowEmpty(isShowEmptyField());
        VerticalFormCorrelationItemPanel panel =
                new VerticalFormCorrelationItemPanel(ID_PANEL, valueModel, settings) {
                    @Override
                    protected boolean isShowEmptyButtonVisible() {
                        return isShowEmptyField();
                    }
                };
        panel.setOutputMarkupId(true);
        add(panel);
        valueModel.getObject().getRealValue().asPrismContainerValue();

        CorrelationItemRefsTable table = buildCorrelationitemRefsTable();
        add(table);
    }

    private @NotNull CorrelationItemRefsTable buildCorrelationitemRefsTable() {
        CorrelationItemRefsTable table = new CorrelationItemRefsTable(ID_TABLE, getModel(), getConfiguration()) {
            @Override
            boolean isReadOnlyTable() {
                return isSuggestionApplied() || isReadOnly();
            }

            @Override
            public @NotNull IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> getResourceObjectTypeDefModel() {
                return CorrelationItemRulePanel.this.getResourceObjectTypeDefinitionModel();
            }
        };
        table.setOutputMarkupId(true);
        return table;
    }

    protected boolean isShowEmptyField() {
        return false;
    }

    protected ContainerPanelConfigurationType getConfiguration() {
        return null;
    }

    private boolean isSuggestionApplied() {
        return getStatusInfo() != null;
    }

    protected boolean isReadOnly(){
        return false;
    }

    private IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> getResourceObjectTypeDefinitionModel() {
        return resourceObjectTypeDefinition;
    }

    private StatusInfo<CorrelationSuggestionsType> getStatusInfo() {
        return statusInfoModel.getObject();
    }

    @Override
    public int getWidth() {
        return 80;
    }

    @Override
    public int getHeight() {
        return 70;
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
        return createStringResource("CorrelationItemRulePanel.title");
    }

    @Override
    public Component getContent() {
        return this;
    }
}
