/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.correlation;

import com.evolveum.midpoint.gui.api.component.BadgePanel;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormCorrelationItemPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.List;

import static com.evolveum.midpoint.gui.api.util.LocalizationUtil.translate;
import static com.evolveum.midpoint.gui.api.util.WebPrismUtil.setReadOnlyRecursively;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationStatusInfoUtils.extractEfficiencyFromSuggestedCorrelationItemWrapper;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils.getAiEfficiencyBadgeModel;

/**
 * Panel for viewing and editing a correlation item rule.
 */
public class CorrelationItemRulePanel<C extends Containerable> extends BasePanel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> implements Popupable {

    private static final String ID_PANEL = "panel";
    private static final String ID_TABLE = "table";

    private static final String ID_ALERT_CONTAINER = "containerAlert";
    private static final String ID_ALERT_ICON = "iconAlert";
    private static final String ID_ALERT_TITLE = "titleAlert";
    private static final String ID_ALERT_DESCRIPTION = "descriptionAlert";
    private static final String ID_ALERT_BADGE = "badgeAlert";

    IModel<StatusInfo<CorrelationSuggestionsType>> statusInfoModel = Model.of();
    IModel<PrismContainerValueWrapper<C>> parentContainerDefWrapperModel;

    public CorrelationItemRulePanel(String id,
            IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> valueWrapperIModel,
            IModel<StatusInfo<CorrelationSuggestionsType>> statusInfoModel,
            IModel<PrismContainerValueWrapper<C>> resourceObjectTypeDefinition) {
        super(id, valueWrapperIModel);
        this.statusInfoModel = statusInfoModel;
        this.parentContainerDefWrapperModel = resourceObjectTypeDefinition;
    }

    public CorrelationItemRulePanel(String id,
            IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> valueWrapperIModel,
            IModel<PrismContainerValueWrapper<C>> resourceObjectTypeDefinition) {
        super(id, valueWrapperIModel);
        this.parentContainerDefWrapperModel = resourceObjectTypeDefinition;
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

        infoPanel.add(createEfficiencyBadge());
        add(infoPanel);
    }

    private BadgePanel createEfficiencyBadge() {
        String efficiency = extractEfficiencyFromSuggestedCorrelationItemWrapper(getModelObject());
        if (efficiency == null) {
            efficiency = translate("SmartCorrelation.unknown");
        }

        String tooltip = translate("SmartIntegration.badge.tooltip.ai");
        BadgePanel badge = new BadgePanel(ID_ALERT_BADGE,
                getAiEfficiencyBadgeModel(
                        translate("SmartCorrelationTilePanel.unconfirmed.suggestion.efficiency", efficiency),
                        tooltip));
        badge.setOutputMarkupId(true);
        return badge;
    }

    private void initLayout() {
        prepareRuleWrapper();
        add(createVerticalFormCorrelationPanel(getModel(), createRuleSettings()));
        add(createCorrelationItemRefsTable());
    }

    private void prepareRuleWrapper() {
        if (isSuggestionApplied()) {
            setReadOnlyRecursively(getModelObject());
        }
        getModelObject().setShowEmpty(isShowEmptyField());
    }

    private ItemPanelSettings createRuleSettings() {
        return new ItemPanelSettingsBuilder()
                .visibilityHandler(this::getRuleItemVisibility)
                .isRemoveButtonVisible(false)
                .build();
    }

    private ItemVisibility getRuleItemVisibility(ItemWrapper<?, ?> wrapper) {
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
    }

    private VerticalFormCorrelationItemPanel createVerticalFormCorrelationPanel(
            IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> valueModel,
            ItemPanelSettings settings) {
        VerticalFormCorrelationItemPanel panel =
                new VerticalFormCorrelationItemPanel(ID_PANEL, valueModel, settings) {
                    @Override
                    protected boolean isSubContainerEnabled(PrismContainerWrapper<?> wrapper) {
                        // Keep metadata accessible; property wrappers enforce read-only values.
                        return true;
                    }

                    @Override
                    protected boolean isShowEmptyButtonVisible() {
                        return isShowEmptyField();
                    }

                    @Override
                    protected boolean isNoContainerFormVisible(IModel<List<ItemWrapper<?, ?>>> nonContainerWrappers) {
                        return true;
                    }

                    @Override
                    protected boolean isShowMoreButtonVisible(IModel<List<ItemWrapper<?, ?>>> nonContainerWrappers) {
                        return false;
                    }
                };
        panel.setOutputMarkupId(true);
        return panel;
    }

    private CorrelationItemRefsTable<C> createCorrelationItemRefsTable() {
        CorrelationItemRefsTable<C> table = new CorrelationItemRefsTable<>(ID_TABLE, getModel(), getConfiguration()) {
            @Override
            boolean isReadOnlyTable() {
                return isSuggestionApplied() || isReadOnly();
            }

            @Override
            public IModel<PrismContainerValueWrapper<C>> getMappingContainerParent() {
                return CorrelationItemRulePanel.this.getParentContainerWrapper();
            }
        };
        table.setOutputMarkupId(true);
        return table;
    }

    public boolean validateCorrelationItems(AjaxRequestTarget target) {
        return ((CorrelationItemRefsTable<?>) get(ID_TABLE)).validateCorrelationItems(target);
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

    protected boolean isReadOnly() {
        return false;
    }

    private IModel<PrismContainerValueWrapper<C>> getParentContainerWrapper() {
        return parentContainerDefWrapperModel;
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
