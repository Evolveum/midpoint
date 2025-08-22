/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.correlation;

import com.evolveum.midpoint.gui.api.component.button.DropdownButtonDto;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonPanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.tile.TemplateTilePanel;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemsSubCorrelatorType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.TITLE_CSS;

public class SmartCorrelationTilePanel<C extends PrismContainerValueWrapper<ItemsSubCorrelatorType>>
        extends TemplateTilePanel<C, SmartCorrelationTileModel<C>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_SELECT_CHECKBOX = "selectCheckbox";
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

    public SmartCorrelationTilePanel(@NotNull String id,
            @NotNull IModel<SmartCorrelationTileModel<C>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        buildPanel();
    }

    protected void buildPanel() {
        initLabelComponent(ID_TITLE, () -> getModelObject().getName());
        initLabelComponent(ID_DESC, () -> getModelObject().getDescription());
        initLabelComponent(ID_STATS_LABEL, createStringResource("SmartCorrelationTilePanel.stats.label"));
        initLabelComponent(ID_STATE_LABEL, createStringResource("SmartCorrelationTilePanel.state.label"));
        initLabelComponent(ID_STATE_PANEL, () -> getModelObject().getEnabled());

        initCheckBox();
        initActionButton();
        initCorrelationItemPanel();
        initStatsListViewPanel();
        initFooterLinkButton();
    }

    private void initCorrelationItemPanel() {
        CorrelationItemTypePanel correlationItemTypePanel =
                new CorrelationItemTypePanel(ID_CORRELATION_ITEMS_PANEL, () -> getModelObject().getCorrelationItems(), 2);
        correlationItemTypePanel.setOutputMarkupId(true);
        add(correlationItemTypePanel);
    }

    private void initLabelComponent(String id, IModel<?> model) {
        Label label = new Label(id, model);
        label.setOutputMarkupId(true);
        add(label);
    }

    private void initFooterLinkButton() {
        AjaxIconButton viewRuleLink = new AjaxIconButton(ID_VIEW_RULE_LINK, Model.of("fa fa-eye me-1"),
                createStringResource("SmartCorrelationTilePanel.viewRuleLink")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                //TODO  Implement the logic to view the rule
            }
        };
        viewRuleLink.setOutputMarkupId(true);
        viewRuleLink.showTitleAsLabel(true);
        add(viewRuleLink);
    }

    private void initStatsListViewPanel() {
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
        add(stateListView);
    }

    private void initActionButton() {
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
        add(buttonPanel);
    }

    private void initCheckBox() {
        CheckBox selectCheckbox = new CheckBox(ID_SELECT_CHECKBOX, Model.of(getModelObject().isSelected()));
        selectCheckbox.setOutputMarkupId(true);
        add(selectCheckbox);
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
}

