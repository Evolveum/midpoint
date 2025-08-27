/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.correlation;

import com.evolveum.midpoint.gui.api.component.BadgePanel;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonDto;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.tile.MultiSelectContainerActionTileTablePanel;
import com.evolveum.midpoint.gui.impl.component.tile.TemplateTilePanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.component.SmartGeneratingPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.dto.SmartGeneratingDto;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils.getAiCustomTextBadgeModel;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.TITLE_CSS;

public class SmartCorrelationTilePanel<C extends PrismContainerValueWrapper<ItemsSubCorrelatorType>>
        extends TemplateTilePanel<C, SmartCorrelationTileModel<C>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_TILE_CONTENT = "tileContent";

    private static final String ID_SELECT_CHECKBOX = "selectCheckbox";
    private static final String ID_BADGE_PANEL = "badgePanel";
    private static final String ID_MORE_ACTION = "moreAction";

    private static final String ID_TITLE = "title";
    private static final String ID_DESC = "description";

    private static final String ID_CORRELATION_ITEMS_PANEL = "correlationItemsPanel";

    private static final String ID_STATS_LABEL = "statsLabel";
    private static final String ID_STATS_PANEL = "statsPanel";
    private static final String ID_STATS_PANEL_VALUE = "statsPanelValue";
    private static final String ID_STATS_PANEL_LABEL = "statsPanelLabel";

    private static final String ID_STATE_LABEL = "stateLabel";
    private static final String ID_STATE_PANEL = "statePanel";

    private static final String ID_VIEW_RULE_LINK = "viewRuleLink";

    LoadableModel<StatusInfo<?>> statusModel;

    public SmartCorrelationTilePanel(@NotNull String id, @NotNull IModel<SmartCorrelationTileModel<C>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        this.setOutputMarkupId(true);


        statusModel = new LoadableModel<>(true) {
            @Override
            protected StatusInfo<CorrelationSuggestionType> load() {
                Task task = getPageBase().createSimpleTask("Load generation status");
                OperationResult result = task.getResult();
                return getModelObject().getStatusInfo(getPageBase(), task, result);
            }
        };

        StatusInfo<?> statusInfo = statusModel.getObject();
        if (statusInfo != null && statusInfo.isExecuting()) {
            add(createGeneratingFragment());
        } else {
            add(createBasicFragment());
        }
    }

    private @NotNull Fragment createGeneratingFragment() {
        Fragment fragment = new Fragment(SmartCorrelationTilePanel.ID_TILE_CONTENT, "generatingTileFragment", this);
        Component generatingPanelComponent = createGeneratingPanelComponent();
        fragment.add(generatingPanelComponent);
        return fragment;
    }

    private @NotNull Fragment createBasicFragment() {
        Fragment fragment = new Fragment(SmartCorrelationTilePanel.ID_TILE_CONTENT, "basicTileFragment", this);
        buildBasicPanel(fragment);
        return fragment;
    }

    protected void buildBasicPanel(Fragment fragment) {
        initLabelComponent(ID_TITLE, () -> getModelObject().getName(), fragment);
        initLabelComponent(ID_DESC, () -> getModelObject().getDescription(), fragment);
        initLabelComponent(ID_STATS_LABEL, createStringResource("SmartCorrelationTilePanel.stats.label"), fragment);
        if (statusModel.getObject() != null) {
            initLabelComponent(ID_STATE_LABEL, createStringResource("SmartCorrelationTilePanel.action.label"), fragment);
            initActionSuggestionButton(fragment);
        } else {
            initLabelComponent(ID_STATE_LABEL, createStringResource("SmartCorrelationTilePanel.state.label"), fragment);
            initLabelComponent(ID_STATE_PANEL, () -> getModelObject().getEnabled(), fragment);
        }
        initCheckBox(fragment);
        initBadgePanel(fragment);
        initActionButton(fragment);
        initCorrelationItemPanel(fragment);
        initStatsListViewPanel(fragment);
        initFooterLinkButton(fragment);
    }

    private void initCorrelationItemPanel(@NotNull Fragment fragment) {
        CorrelationItemTypePanel correlationItemTypePanel =
                new CorrelationItemTypePanel(ID_CORRELATION_ITEMS_PANEL, () -> getModelObject().getCorrelationItems(), 2);
        correlationItemTypePanel.setOutputMarkupId(true);
        fragment.add(correlationItemTypePanel);
    }

    private void initLabelComponent(String id, IModel<?> model, @NotNull Fragment fragment) {
        Label label = new Label(id, model);
        label.setOutputMarkupId(true);
        label.add(AttributeModifier.append("class", model.getObject().equals(Boolean.TRUE)
                ? "success-light" : ""));
        fragment.add(label);
    }

    private void initFooterLinkButton(@NotNull Fragment fragment) {
        AjaxIconButton viewRuleLink = new AjaxIconButton(ID_VIEW_RULE_LINK, Model.of("fa fa-eye me-1"),
                createStringResource("SmartCorrelationTilePanel.viewRuleLink")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onFooterButtonClick(target);
            }
        };
        viewRuleLink.setOutputMarkupId(true);
        viewRuleLink.showTitleAsLabel(true);
        fragment.add(viewRuleLink);
    }

    private void initStatsListViewPanel(@NotNull Fragment fragment) {
        ListView<SmartCorrelationTileModel.StateRecord> stateListView = new ListView<>(ID_STATS_PANEL,
                getModelObject().getStatesRecordList()) {

            @Override
            protected void populateItem(@NotNull ListItem<SmartCorrelationTileModel.StateRecord> listItem) {
                SmartCorrelationTileModel.StateRecord stateRecord = listItem.getModelObject();
                Label stateValue = new Label(ID_STATS_PANEL_VALUE, stateRecord.getValue());
                stateValue.setOutputMarkupId(true);
                listItem.add(stateValue);

                Label stateLabel = new Label(ID_STATS_PANEL_LABEL, stateRecord.getLabel());
                stateLabel.setOutputMarkupId(true);
                listItem.add(stateLabel);
            }
        };
        stateListView.setOutputMarkupId(true);
        fragment.add(stateListView);
    }

    private void initActionSuggestionButton(@NotNull Fragment fragment) {
        RepeatingView buttonsView = new RepeatingView(ID_STATE_PANEL);

        AjaxIconButton discardButton = new AjaxIconButton(buttonsView.newChildId(), Model.of("fa fa-solid fa-x"),
                createStringResource("SmartCorrelationTilePanel.discardButton")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onDiscardButtonClick(target);
            }
        };
        discardButton.setOutputMarkupId(true);
        discardButton.add(new TooltipBehavior());
        discardButton.add(AttributeModifier.replace("class", "col p-2 btn btn-default rounded"));
        discardButton.showTitleAsLabel(true);
        buttonsView.add(discardButton);

        AjaxIconButton acceptButton = new AjaxIconButton(buttonsView.newChildId(), Model.of("fa fa-check"),
                createStringResource("SmartCorrelationTilePanel.acceptButton")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onAcceptButtonClick(target);
            }
        };
        acceptButton.setOutputMarkupId(true);
        acceptButton.add(new TooltipBehavior());
        acceptButton.add(AttributeModifier.replace("class", "col p-2 btn btn-success rounded"));
        acceptButton.showTitleAsLabel(true);
        buttonsView.add(acceptButton);
        fragment.add(buttonsView);
    }

    private void initActionButton(@NotNull Fragment fragment) {
        DropdownButtonPanel buttonPanel = new DropdownButtonPanel(ID_MORE_ACTION, new DropdownButtonDto(
                null, "fa-ellipsis-h ml-1", null, createMenuItems())) {
            @Override
            protected boolean hasToggleIcon() {
                return false;
            }

            @Contract(pure = true)
            @Override
            protected @NotNull String getSpecialButtonClass() {
                return " px-1 py-0 ";
            }
        };
        buttonPanel.setOutputMarkupId(true);
        buttonPanel.add(AttributeModifier.replace(TITLE_CSS, createStringResource("RoleAnalysis.menu.moreOptions")));
        buttonPanel.add(new TooltipBehavior());
        fragment.add(buttonPanel);
    }

    private void initCheckBox(@NotNull Fragment fragment) {
        IModel<Boolean> selectedModel = new IModel<>() {
            @Override
            public @NotNull Boolean getObject() {
                return getModelObject().getValue().isSelected();
            }

            @Override
            public void setObject(Boolean value) {
                getModelObject().getValue().setSelected(Boolean.TRUE.equals(value));
            }
        };

        AjaxCheckBox selectCheckbox = new AjaxCheckBox(ID_SELECT_CHECKBOX, selectedModel) {
            @Override
            protected void onUpdate(@NotNull AjaxRequestTarget target) {
                Component component = SmartCorrelationTilePanel.this.findParent(MultiSelectContainerActionTileTablePanel.class);
                target.add(Objects.requireNonNullElse(component, SmartCorrelationTilePanel.this));

                component = SmartCorrelationTilePanel.this.findParent(MultiSelectContainerActionTileTablePanel.class);
                target.add(Objects.requireNonNullElse(component, SmartCorrelationTilePanel.this));
            }
        };
        selectCheckbox.setOutputMarkupId(true);
        fragment.addOrReplace(selectCheckbox);
    }

    private void initBadgePanel(@NotNull Fragment fragment) {
        BadgePanel badge = new BadgePanel(ID_BADGE_PANEL, getAiCustomTextBadgeModel("Suggestion"));
        badge.setOutputMarkupId(true);
        badge.add(new VisibleBehaviour(() -> statusModel.getObject() != null));
        fragment.add(badge);
    }

    @Override
    protected void initLayout() {
        // No additional layout initialization needed
    }

    public List<InlineMenuItem> createMenuItems() {
        List<InlineMenuItem> items = new ArrayList<>();
        items.add(new InlineMenuItem(createStringResource("abstractRoleMemberPanel.menu.delete")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        // Implement the logic to delete the correlation
                    }
                };
            }

        });
        return items;
    }

    protected Component createGeneratingPanelComponent() {
        return new SmartGeneratingPanel("generationPanel", () -> {
            Task task = getPageBase().createSimpleTask("Load generation status");
            OperationResult result = task.getResult();

            String token = statusModel.getObject().getToken();
            PrismObject<TaskType> taskTypePrismObject = WebModelServiceUtils.loadObject(
                    TaskType.class, token, getPageBase(), task, result);
            return new SmartGeneratingDto(statusModel, () -> taskTypePrismObject);
        }, false) {
            @Override
            protected void createButtons(@NotNull RepeatingView buttonsView) {
                initActionButton(buttonsView);
            }

            @Override
            protected boolean isListViewVisible() {
                return false;
            }

            @Override
            protected IModel<String> getSubTitleModel() {
                return createStringResource(
                        "SmartGeneratingSuggestionStep.generating.correlation.suggestion.action.subText");
            }

            @Override
            protected void onFinishActionPerform(AjaxRequestTarget target) {
                onFinishGeneration(target);
            }
        };
    }

    protected void onFooterButtonClick(AjaxRequestTarget target) {
        // Override to implement footer button click behavior
    }

    protected void onAcceptButtonClick(AjaxRequestTarget target) {
        // Override to implement accept button click behavior
    }

    protected void onDiscardButtonClick(AjaxRequestTarget target) {
        // Override to implement discard button click behavior
    }

    protected void onRefresh(AjaxRequestTarget target) {
        // Override to implement refresh behavior
    }

    protected void onFinishGeneration(AjaxRequestTarget target) {
        // Override to implement finish generation behavior
    }
}

