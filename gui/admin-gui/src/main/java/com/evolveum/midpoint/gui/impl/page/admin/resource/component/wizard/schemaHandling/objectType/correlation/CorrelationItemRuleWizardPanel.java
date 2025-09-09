/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.correlation;

import com.evolveum.midpoint.gui.api.component.BadgePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;

import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormCorrelationItemPanel;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceWizardBasicPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.util.GuiDisplayNameUtil;

import static com.evolveum.midpoint.gui.api.util.WebPrismUtil.setReadOnlyRecursively;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationStatusInfoUtils.extractEfficiencyFromSuggestedCorrelationItemWrapper;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils.*;

/**
 * @author lskublik
 */

@PanelType(name = "rw-correlators")
@PanelInstance(identifier = "rw-correlators",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "CorrelationItemRefsTableWizardPanel.headerLabel", icon = "fa fa-bars-progress"))
public class CorrelationItemRuleWizardPanel extends AbstractResourceWizardBasicPanel<ItemsSubCorrelatorType> {

    private static final String PANEL_TYPE = "rw-correlators";

    private static final String ID_PANEL = "panel";
    private static final String ID_TABLE = "table";

    private static final String ID_ALERT_CONTAINER = "containerAlert";
    private static final String ID_ALERT_ICON = "iconAlert";
    private static final String ID_ALERT_TITLE = "titleAlert";
    private static final String ID_ALERT_DESCRIPTION = "descriptionAlert";
    private static final String ID_ALERT_BADGE = "badgeAlert";

    IModel<StatusInfo<CorrelationSuggestionsType>> statusInfoModel;

    public CorrelationItemRuleWizardPanel(
            String id,
            WizardPanelHelper<ItemsSubCorrelatorType, ResourceDetailsModel> superHelper,
            IModel<StatusInfo<CorrelationSuggestionsType>> statusInfoModel) {
        super(id, superHelper);
        this.statusInfoModel = statusInfoModel;
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

        Double efficiency = extractEfficiencyFromSuggestedCorrelationItemWrapper(getValueModel().getObject());

        BadgePanel badge = new BadgePanel(ID_ALERT_BADGE,
                getAiEfficiencyBadgeModel(
                        createStringResource("SmartCorrelationTilePanel.unconfirmed.suggestion.efficiency",
                                efficiency != null ? efficiency : "-").getString()));
        badge.setOutputMarkupId(true);
        infoPanel.add(badge);
        add(infoPanel);
    }

    @Override
    public boolean isEnabledInHierarchy() {
        return super.isEnabledInHierarchy();
    }

    private void initLayout() {
        IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> valueModel = getValueModel();

        ItemPanelSettings settings = new ItemPanelSettingsBuilder()
                .visibilityHandler((wrapper) -> {
                    ItemName itemName = wrapper.getPath().lastName();
                    return itemName.equivalent(ItemsSubCorrelatorType.F_DESCRIPTION)
//                            || itemName.equivalent(ItemsSubCorrelatorType.F_DISPLAY_NAME)
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
                        return !isSuggestionApplied();
                    }
                };
        panel.setOutputMarkupId(true);
        add(panel);
        valueModel.getObject().getRealValue().asPrismContainerValue();

        CorrelationItemRefsTable table = new CorrelationItemRefsTable(ID_TABLE, getValueModel(), getConfiguration()) {
            @Override
            boolean isReadOnlyTable() {
                return isSuggestionApplied();
            }
        };
        table.setOutputMarkupId(true);
        add(table);
    }

    @Override
    protected void onSubmitPerformed(AjaxRequestTarget target) {
        if (isSuggestionApplied()) {
            acceptSuggestionPerformed(target, getValueModel());
            return;
        }

        onExitPerformed(target);
    }

    protected boolean isShowEmptyField() {
        return false;
    }

    protected void acceptSuggestionPerformed(
            @NotNull AjaxRequestTarget target,
            @NotNull IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> valueModel) {
    }

    @Override
    protected IModel<String> getSubmitLabelModel() {
        return isSuggestionApplied()
                ? getPageBase().createStringResource("CorrelationItemRefsTableWizardPanel.accept")
                : getPageBase().createStringResource("CorrelationItemRefsTableWizardPanel.confirm");
    }

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        String name = GuiDisplayNameUtil.getDisplayName(getValueModel().getObject().getRealValue());
        if (StringUtils.isNotBlank(name)) {
            return Model.of(name);
        }
        return getPageBase().createStringResource("CorrelationItemRefsTableWizardPanel.breadcrumb");
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("CorrelationItemRefsTableWizardPanel.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("CorrelationItemRefsTableWizardPanel.subText");
    }

    protected CorrelationItemRefsTable getTable() {
        return (CorrelationItemRefsTable) get(ID_TABLE);
    }

    @Override
    protected boolean isValid(AjaxRequestTarget target) {
        return getTable().isValidFormComponents(target);
    }

    @Override
    protected boolean isExitButtonVisible() {
        return isSuggestionApplied();
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    protected StatusInfo<?> getStatusInfo() {
        return statusInfoModel.getObject();
    }

    protected boolean isSuggestionApplied() {
        return getStatusInfo() != null;
    }

    @Override
    protected void addCustomButtons(@NotNull RepeatingView buttons) {
        AjaxIconButton discardButton = new AjaxIconButton(
                buttons.newChildId(),
                Model.of("fa fa-trash"),
                Model.of("Discard")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onDiscardButtonClick(getPageBase(), target, getValueModel(), getStatusInfo());
                onExitPerformed(target);
            }
        };
        discardButton.showTitleAsLabel(true);
        discardButton.add(new VisibleBehaviour(this::isDiscardButtonVisible));
        discardButton.add(AttributeAppender.append("class", "btn-link text-danger"));
        buttons.add(discardButton);
    }

    protected boolean isDiscardButtonVisible() {
        return isSuggestionApplied();
    }

    protected void onDiscardButtonClick(
            @NotNull PageBase pageBase,
            @NotNull AjaxRequestTarget target,
            @NotNull IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> valueModel,
            @NotNull StatusInfo<?> statusInfo) {
    }

}
