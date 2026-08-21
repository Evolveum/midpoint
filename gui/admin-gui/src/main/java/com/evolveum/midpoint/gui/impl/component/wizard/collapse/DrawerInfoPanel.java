/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.collapse;

import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.component.BadgePanel;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

public class DrawerInfoPanel<M extends DrawerDescriptor<M>>
        extends BasePanel<Void> {

    private static final String ID_FAKE_PANEL = "fakePanel";
    private static final String ID_COLLAPSED_MENU = "collapsedMenu";
    private static final String ID_MENU_ITEM = "menuItem";
    private static final String ID_ICON_BUTTON = "iconButton";
    private static final String ID_BADGE = "badge";
    private static final String ID_DETAILS = "details";
    private static final String ID_DETAILS_LABEL = "detailsLabel";
    private static final String ID_CLOSE_BUTTON = "closeButton";
    private static final String ID_DETAILS_ITEM = "detailsItem";
    protected static final String ID_FOOTER_PANEL = "footerPanel";

    private M drawerModel;

    public DrawerInfoPanel(
            @NotNull String id,
            @NotNull M drawerModel) {
        super(id);
        this.drawerModel = Objects.requireNonNull(drawerModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        setOutputMarkupId(true);
        setOutputMarkupPlaceholderTag(true);

        initLayout();
    }

    private void initLayout() {
        add(new VisibleBehaviour(() -> drawerModel.isCollapsedItemsVisible()));

        WebMarkupContainer fakePanel =
                new WebMarkupContainer(ID_FAKE_PANEL);
        fakePanel.setOutputMarkupId(true);
        fakePanel.setOutputMarkupPlaceholderTag(true);
        fakePanel.add(new VisibleBehaviour(this::isShowedDetails));
        add(fakePanel);

        WebMarkupContainer collapsedMenu =
                new WebMarkupContainer(ID_COLLAPSED_MENU);
        collapsedMenu.setOutputMarkupId(true);
        collapsedMenu.setOutputMarkupPlaceholderTag(true);
        collapsedMenu.add(createItemListView());
        collapsedMenu.add(new VisibleBehaviour(this::isShowedCollapsedMenu));
        add(collapsedMenu);

        WebMarkupContainer detailsPanel =
                new WebMarkupContainer(ID_DETAILS);
        detailsPanel.setOutputMarkupId(true);
        detailsPanel.setOutputMarkupPlaceholderTag(true);
        detailsPanel.add(new VisibleBehaviour(this::isShowedDetails));

        customizeDetailsPanelWidth(detailsPanel);
        add(detailsPanel);

        IconWithLabel detailLabel = new IconWithLabel(
                ID_DETAILS_LABEL,
                this::getTitleOfSelectedItem) {
            @Override
            protected String getIconCssClass() {
                return getTitleIconOfSelectedItem();
            }
        };
        detailLabel.setOutputMarkupId(true);
        detailsPanel.add(detailLabel);

        AjaxLink<Void> closeButton = new AjaxLink<>(ID_CLOSE_BUTTON) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                onClose(target);
            }
        };
        closeButton.setOutputMarkupId(true);
        detailsPanel.add(closeButton);

        detailsPanel.add(createDetailsPlaceholder());

        detailsPanel.add(createFooter());
    }

    protected void onClose(AjaxRequestTarget target) {
        drawerModel.clearSelection();
        replaceDetailsPlaceholder();

        refreshDrawerContent(target);
    }

    private @NotNull Component createFooter() {
        Component footer = drawerModel.getFooter(
                ID_FOOTER_PANEL,
                drawerModel);

        footer.setOutputMarkupId(true);
        footer.setOutputMarkupPlaceholderTag(true);
        footer.add(new VisibleBehaviour(
                () -> drawerModel.isFooterVisible()));

        return footer;
    }

    private void customizeDetailsPanelWidth(WebMarkupContainer detailsPanel) {
        if (getMinWidth() != null) {
            detailsPanel.add(AttributeModifier.append("style", "min-width: " + getMinWidth()));
        }
    }

    protected String getMinWidth() {
        return null;
    }

    private @NotNull ListView<CollapsedItem<M>> createItemListView() {
        IModel<List<CollapsedItem<M>>> itemsModel =
                () -> drawerModel.getCollapsedItems().getObject();

        ListView<CollapsedItem<M>> menuItems =
                new ListView<>(ID_MENU_ITEM, itemsModel) {

                    @Override
                    protected void populateItem(
                            ListItem<CollapsedItem<M>> item) {

                        CollapsedItem<M> collapsedItem =
                                item.getModelObject();

                        AjaxIconButton iconButton = new AjaxIconButton(ID_ICON_BUTTON,
                                collapsedItem.getIcon(), collapsedItem.getTitle()) {

                            @Override
                            public void onClick(
                                    AjaxRequestTarget target) {

                                boolean selected = !collapsedItem.isSelected();

                                if (selected) {
                                    drawerModel.select(collapsedItem);
                                    replaceDetailsContent(collapsedItem);
                                } else {
                                    drawerModel.clearSelection();
                                    replaceDetailsPlaceholder();
                                }

                                refreshDrawerContent(target);
                            }
                        };

                        iconButton.setOutputMarkupId(true);
                        iconButton.add(AttributeAppender.append(
                                "class",
                                () -> collapsedItem.isSelected()
                                        ? "selected"
                                        : ""));
                        item.add(iconButton);

                        Badge badge = new Badge() {

                            @Override
                            public String getText() {
                                return String.valueOf(collapsedItem.countOfObject());
                            }
                        };
                        badge.setCssClass(Badge.State.DANGER);

                        BadgePanel badgePanel =
                                new BadgePanel(ID_BADGE, () -> badge);
                        badgePanel.setOutputMarkupId(true);
                        badgePanel.setOutputMarkupPlaceholderTag(true);
                        badgePanel.add(new VisibleBehaviour(
                                () -> collapsedItem.countOfObject() > 0));
                        item.add(badgePanel);
                    }
                };

        menuItems.setOutputMarkupId(true);
        return menuItems;
    }

    /**
     * Replaces the drawer model and refreshes the drawer.
     */
    public void replaceModel(
            @NotNull M drawerModel,
            @NotNull AjaxRequestTarget target) {

        this.drawerModel = Objects.requireNonNull(drawerModel);

        replaceSelectedDetailsContent();
        getDetailsPanel().addOrReplace(createFooter());

        target.add(this);
    }

    private void replaceSelectedDetailsContent() {
        drawerModel.getSelectedCollapsedItem()
                .ifPresentOrElse(
                        this::replaceDetailsContent,
                        this::replaceDetailsPlaceholder);
    }

    private void replaceDetailsContent(
            @NotNull CollapsedItem<M> collapsedItem) {

        Component panel = collapsedItem.getPanel(
                ID_DETAILS_ITEM,
                drawerModel);

        panel.setOutputMarkupId(true);
        panel.setOutputMarkupPlaceholderTag(true);

        getDetailsPanel().addOrReplace(panel);
    }

    private void replaceDetailsPlaceholder() {
        getDetailsPanel().addOrReplace(
                createDetailsPlaceholder());
    }

    private @NotNull WebMarkupContainer createDetailsPlaceholder() {
        WebMarkupContainer placeholder =
                new WebMarkupContainer(ID_DETAILS_ITEM);

        placeholder.setOutputMarkupId(true);
        placeholder.setOutputMarkupPlaceholderTag(true);

        return placeholder;
    }

    private @NotNull WebMarkupContainer getDetailsPanel() {
        return (WebMarkupContainer) get(ID_DETAILS);
    }

    private String getTitleOfSelectedItem() {
        return drawerModel.getSelectedCollapsedItem()
                .map(CollapsedItem::getTitle)
                .orElse(Model.of(""))
                .getObject();
    }

    private String getTitleIconOfSelectedItem() {
        return drawerModel.getSelectedCollapsedItem()
                .map(CollapsedItem::getTitleIconCss)
                .orElse(Model.of(""))
                .getObject();
    }

    public boolean isShowedDetails() {
        return drawerModel.getSelectedCollapsedItem().isPresent();
    }

    public boolean isShowedCollapsedMenu() {
        return drawerModel.isShowedCollapsedMenu();
    }

    public M getDrawerModel() {
        return drawerModel;
    }

    private void refreshDrawerContent(AjaxRequestTarget target) {
        target.add(get(ID_FAKE_PANEL));
        target.add(get(ID_COLLAPSED_MENU));
        target.add(getDetailsPanel());
    }
}
