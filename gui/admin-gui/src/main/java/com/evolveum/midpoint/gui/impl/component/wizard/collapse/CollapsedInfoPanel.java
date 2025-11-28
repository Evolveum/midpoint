/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.collapse;

import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.component.BadgePanel;
import com.evolveum.midpoint.gui.api.component.BasePanel;

import com.evolveum.midpoint.gui.impl.component.wizard.withnavigation.WizardModelWithParentSteps;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

public class CollapsedInfoPanel extends BasePanel {

    private static final String ID_FAKE_PANEL = "fakePanel";
    private static final String ID_COLLAPSED_MENU = "collapsedMenu";
    private static final String ID_MENU_ITEM = "menuItem";
    private static final String ID_ICON_BUTTON = "iconButton";
    private static final String ID_BADGE = "badge";
    private static final String ID_DETAILS = "details";
    private static final String ID_DETAILS_LABEL = "detailsLabel";
    private static final String ID_CLOSE_BUTTON = "closeButton";
    private static final String ID_DETAILS_ITEM = "detailsItem";

    private final WizardModelWithParentSteps wizardModel;

    public CollapsedInfoPanel(String id, WizardModelWithParentSteps wizardModel) {
        super(id);
        this.wizardModel = wizardModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayer();
    }

    private void initLayer() {
        add(new VisibleBehaviour(wizardModel::isCollapsedItemsVisible));

        WebMarkupContainer fakePanel = new WebMarkupContainer(ID_FAKE_PANEL);
        fakePanel.setOutputMarkupId(true);
        fakePanel.add(new VisibleBehaviour(CollapsedInfoPanel.this::isShowedDetails));
        add(fakePanel);

        WebMarkupContainer collapsedMenu = new WebMarkupContainer(ID_COLLAPSED_MENU);
        collapsedMenu.setOutputMarkupId(true);
        add(collapsedMenu);

        ListView<CollapsedItem> menuItems = getItemListView();
        collapsedMenu.add(menuItems);

        WebMarkupContainer detailsPanel = new WebMarkupContainer(ID_DETAILS);
        detailsPanel.setOutputMarkupId(true);
        detailsPanel.add(new VisibleBehaviour(CollapsedInfoPanel.this::isShowedDetails));
        add(detailsPanel);

        Label detailLabel = new Label(ID_DETAILS_LABEL, this::getTitleOfSelectedItem);
        detailLabel.setOutputMarkupId(true);
        detailsPanel.add(detailLabel);

        AjaxLink<Void> closeButton = new AjaxLink<>(ID_CLOSE_BUTTON) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                wizardModel.getSelectedCollapsedItem().ifPresent(item -> item.setSelected(false));

                target.add(CollapsedInfoPanel.this);
            }
        };
        closeButton.setOutputMarkupId(true);
        detailsPanel.add(closeButton);

        WebMarkupContainer detailsItemPanel = new WebMarkupContainer(ID_DETAILS_ITEM);
        detailsItemPanel.setOutputMarkupId(true);
        detailsPanel.add(detailsItemPanel);
    }

    private @NotNull ListView<CollapsedItem> getItemListView() {
        ListView<CollapsedItem> menuItems = new ListView<>(ID_MENU_ITEM, wizardModel.getCollapsedItems()) {
            @Override
            protected void populateItem(ListItem<CollapsedItem> item) {
                AjaxIconButton iconButton = new AjaxIconButton(ID_ICON_BUTTON, item.getModelObject().getIcon(), item.getModelObject().getTitle()) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        target.add(CollapsedInfoPanel.this);
                        boolean selected = !item.getModelObject().isSelected();
                        wizardModel.getCollapsedItems().getObject().forEach(item -> item.setSelected(false));
                        item.getModelObject().setSelected(selected);
                        if (selected) {
                            Component panel = item.getModelObject().getPanel(ID_DETAILS_ITEM, wizardModel);
                            panel.setOutputMarkupId(true);
                            getDetailsPanel().addOrReplace(panel);
                        }
                    }
                };
                iconButton.setOutputMarkupId(true);
                item.add(iconButton);
                iconButton.add(AttributeAppender.append("class", item.getModelObject().isSelected() ? "selected" : ""));

                Badge badge = new Badge() {
                    @Override
                    public String getText() {
                        return String.valueOf(item.getModelObject().countOfObject());
                    }
                };
                badge.setCssClass(Badge.State.DANGER);

                BadgePanel badgePanel = new BadgePanel(ID_BADGE, () -> badge);
                badgePanel.setOutputMarkupId(true);
                badgePanel.add(new VisibleBehaviour(() -> item.getModelObject().countOfObject() > 0));
                item.add(badgePanel);
            }
        };
        menuItems.setOutputMarkupId(true);
        return menuItems;
    }

    private WebMarkupContainer getDetailsPanel() {
        return (WebMarkupContainer) get(ID_DETAILS);
    }

    private String getTitleOfSelectedItem() {
        return wizardModel.getSelectedCollapsedItem()
                .map(CollapsedItem::getTitle)
                .orElse(Model.of("")).getObject();
    }

    public boolean isShowedDetails() {
        return wizardModel.getSelectedCollapsedItem().isPresent();
    }
}
