/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.menu;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.IPageFactory;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.flow.RedirectToUrlException;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.breadcrumbs.Breadcrumb;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

/**
 * @author Viliam Repan (lazyman)
 */
public class MainMenuPanel extends BasePanel<MainMenuItem> {
    private static final long serialVersionUID = 1L;

    private static final String ID_ITEM = "item";
    private static final String ID_LINK = "link";
    private static final String ID_SR_CURRENT_MESSAGE = "srCurrentMessage";
    private static final String ID_LABEL = "label";
    private static final String ID_ICON = "icon";
    private static final String ID_SUBMENU = "submenu";
    private static final String ID_ARROW = "arrow";
    private static final String ID_BUBBLE = "bubble";
    private static final String ID_SUB_ITEM = "subItem";
    private static final String ID_SUB_LINK = "subLink";

    private static final String ID_SR_CURRENT_MESSAGE_SUB_ITEM = "srCurrentMessageSubItem";
    private static final String ID_SUB_LABEL = "subLabel";
    private static final String ID_SUB_LINK_ICON = "subLinkIcon";
    private static final String ID_ITEM_STATUS = "itemStatus";

    private static final Trace LOGGER = TraceManager.getTrace(MainMenuPanel.class);

    public MainMenuPanel(String id, IModel<MainMenuItem> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        WebMarkupContainer item = new WebMarkupContainer(ID_ITEM);
        item.setOutputMarkupId(true);
        item.add(AttributeModifier.append("class", () -> {
            MainMenuItem mmi = getModelObject();

            return mmi.hasActiveSubmenu(getPageBase()) ? "menu-is-opening menu-open" : null;
        }));
        add(item);

        item.add(AttributeModifier.append("style", () -> isMenuExpanded() ? "" : "display: none;"));

        Label itemStatus = new Label(ID_ITEM_STATUS, Model.of(""));
        itemStatus.add(new VisibleBehaviour(() -> getModelObject().containsSubMenu()));
        itemStatus.setOutputMarkupId(true);
        item.add(itemStatus);

        StringResourceModel labelModel = new StringResourceModel(
                "${nameModel}",
                getModel()).setDefaultValue(new PropertyModel<>(getModel(), "nameModel"));
        AjaxLink<Void> link = new AjaxLink<>(ID_LINK) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                mainMenuPerformed();
                target.appendJavaScript(String.format("MidPointTheme.updateStatusMessageForMenu('%s', %d, '%s', %d);",
                        item.getMarkupId(), 300, itemStatus.getMarkupId(), 100));
            }
        };
        link.add(AttributeModifier.append("class", () -> {
            MainMenuItem mmi = getModelObject();

            return mmi.hasActiveSubmenu(getPageBase()) || mmi.isMenuActive(getPageBase()) ? "active" : null;
        }));

        if (getModelObject().containsSubMenu()) {
            item.add(AttributeAppender.append(
                    "aria-expanded",
                    () -> getModelObject().hasActiveSubmenu(getPageBase())));
        }

        link.add(AttributeModifier.append(
                "aria-current",
                () -> {
                    if (getModelObject().isMenuActive(getPageBase())) {
                        return "page";
                    }
                    return null;
                }));

        item.add(link);

        WebMarkupContainer icon = new WebMarkupContainer(ID_ICON);
        icon.add(AttributeModifier.append("class", new PropertyModel<>(getModel(), MainMenuItem.F_ICON_CLASS)));
        link.add(icon);

        Label srCurrentMessage = new Label(ID_SR_CURRENT_MESSAGE, () -> {
            String key = "MainMenuPanel.srCurrentMessage";
            if (getModelObject().hasActiveSubmenu(getPageBase())) {
                key = "MainMenuPanel.srActiveSubItemMessage";
            }
            return getPageBase().createStringResource(key).getString();
        });
        srCurrentMessage.add(new VisibleBehaviour(() -> {
            MainMenuItem mmi = getModelObject();
            return mmi.hasActiveSubmenu(getPageBase()) || mmi.isMenuActive(getPageBase());
        }));
        link.add(srCurrentMessage);

        Label label = new Label(ID_LABEL, labelModel);
        label.setRenderBodyOnly(true);
        link.add(label);

        final PropertyModel<String> bubbleModel = new PropertyModel<>(getModel(), MainMenuItem.F_BUBBLE_LABEL);

        Label bubble = new Label(ID_BUBBLE, bubbleModel);
        bubble.add(new VisibleBehaviour(() -> bubbleModel.getObject() != null));
        link.add(bubble);

        WebMarkupContainer arrow = new WebMarkupContainer(ID_ARROW);
        arrow.add(new VisibleBehaviour(() -> getModelObject().containsSubMenu() && bubbleModel.getObject() == null));
        link.add(arrow);

        item.add(AttributeAppender.append(
                "aria-haspopup",
                () -> getModelObject().containsSubMenu() && bubbleModel.getObject() == null));

        WebMarkupContainer submenu = new WebMarkupContainer(ID_SUBMENU);
        item.add(submenu);

        ListView<MenuItem> subItem = new ListView<>(ID_SUB_ITEM, new PropertyModel<>(getModel(), MainMenuItem.F_ITEMS)) {

            @Override
            protected void populateItem(ListItem<MenuItem> listItem) {
                createSubmenu(listItem);
            }
        };
        subItem.setOutputMarkupId(true);
        submenu.add(new VisibleBehaviour(() -> getModelObject().containsSubMenu()));
        submenu.add(subItem);
    }

    private void createSubmenu(final ListItem<MenuItem> listItem) {
        IModel<MenuItem> menuItem = listItem.getModel();

        StringResourceModel labelModel = new StringResourceModel("${nameModel}", menuItem);
        labelModel.setDefaultValue(new PropertyModel<>(menuItem, "nameModel"));

        Link<String> subLink = new Link<>(ID_SUB_LINK) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick() {
                menuItemPerformed(menuItem.getObject());
            }
        };
        subLink.setEnabled(!menuItem.getObject().isDynamic());
        subLink.add(AttributeModifier.append("class", () -> menuItem.getObject().isMenuActive(getPageBase()) ? "active" : null));

        listItem.add(subLink);

        subLink.add(AttributeModifier.append(
                "aria-current",
                () -> {
                    if (menuItem.getObject().isMenuActive(getPageBase())) {
                        return "page";
                    }
                    return null;
                }));

        WebMarkupContainer subLinkIcon = new WebMarkupContainer(ID_SUB_LINK_ICON);
        subLinkIcon.add(AttributeAppender.append("class", new PropertyModel<>(menuItem, MainMenuItem.F_ICON_CLASS)));
        subLink.add(subLinkIcon);

        Label srCurrentMessage = new Label(
                ID_SR_CURRENT_MESSAGE_SUB_ITEM,
                getPageBase().createStringResource("MainMenuPanel.srCurrentMessage"));
        srCurrentMessage.add(new VisibleBehaviour(() -> listItem.getModelObject().isMenuActive(getPageBase())));
        subLink.add(srCurrentMessage);

        Label subLabel = new Label(ID_SUB_LABEL, labelModel);
        subLink.add(subLabel);
    }

    private void menuItemPerformed(MenuItem menu) {
        LOGGER.trace("menuItemPerformed: {}", menu);

        IPageFactory pFactory = Session.get().getPageFactory();
        WebPage page;
        if (menu.getParams() == null) {
            page = pFactory.newPage(menu.getPageClass());
        } else {
            page = pFactory.newPage(menu.getPageClass(), menu.getParams());
        }
        if (!(page instanceof PageBase)) {
            setResponsePage(page);
            return;
        }

        PageBase pageBase = (PageBase) page;

        // IMPORTANT: we need to re-bundle the name to a new models
        // that will not be connected to the old page reference
        // otherwise the old page will somehow remain in the memory
        // I have no idea how it could do that and especially how
        // several old pages can remain in memory. But if the model
        // is not re-bundled here then the page size grows and never
        // falls.
        MainMenuItem mainMenuItem = getModelObject();
        String name = mainMenuItem.getNameModel();
        Breadcrumb bc = new Breadcrumb(createStringResource(name));
        bc.setIcon(new Model<>(mainMenuItem.getIconClass()));
        pageBase.addBreadcrumb(bc);

        if (mainMenuItem.containsSubMenu() && mainMenuItem.isInsertDefaultBackBreadcrumb()) {
            MenuItem first = mainMenuItem.getFirstMenuItem();

            Breadcrumb invisibleBc = new Breadcrumb(createStringResource(first.getNameModel()), first.getPageClass(), first.getParams());
            invisibleBc.setVisible(false);
            pageBase.addBreadcrumb(invisibleBc);
        }

        setResponsePage(page);
    }

    private void mainMenuPerformed() {
        MainMenuItem menuItem = getModelObject();
        Class<? extends WebPage> page = menuItem.getPageClass();
        if (page != null) {
            setResponsePage(page, menuItem.getParams());
            return;
        }

        if (menuItem instanceof AdditionalMenuItem) {
            throw new RedirectToUrlException(((AdditionalMenuItem) menuItem).getTargetUrl());
        }
    }

    protected boolean isMenuExpanded() {
        return true;
    }
}
