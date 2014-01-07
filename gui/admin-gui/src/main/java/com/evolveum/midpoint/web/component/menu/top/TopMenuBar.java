package com.evolveum.midpoint.web.component.menu.top;

import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;

import java.util.List;

/**
 * @author lazyman
 */
public class TopMenuBar extends Panel {

    private static String ID_TOP_MENU_BAR = "topMenuBar";
    private static String ID_BAR_ITEM_LINK = "barItemLink";
    private static String ID_CARET = "caret";
    private static String ID_LABEL = "label";
    private static String ID_MENU_ITEM = "menuItem";
    private static String ID_MENU_ITEM_BODY = "menuItemBody";
    public static String ID_RIGHT_PANEL = "rightPanel";

    public TopMenuBar(String id, List<MenuBarItem> items) {
        super(id);
        setRenderBodyOnly(true);

        initLayout(items);
    }

    private void initLayout(final List<MenuBarItem> items) {
        ListView<MenuBarItem> topMenuBar = new ListView<MenuBarItem>(ID_TOP_MENU_BAR, items) {

            @Override
            protected void populateItem(ListItem<MenuBarItem> menuBar) {
                initMenuBarItem(menuBar);
            }
        };
        topMenuBar.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return !items.isEmpty();
            }
        });
        add(topMenuBar);

        WebMarkupContainer rightPanel = new WebMarkupContainer(ID_RIGHT_PANEL);
        rightPanel.setVisible(false);
        add(rightPanel);
    }

    private void initMenuBarItem(ListItem<MenuBarItem> menuBar) {
        final MenuBarItem menuBarItem = menuBar.getModelObject();

        WebMarkupContainer barItemLink;
        if (menuBarItem.getPage() != null) {
            barItemLink = new BookmarkablePageLink(ID_BAR_ITEM_LINK, menuBarItem.getPage());
        } else {
            barItemLink = new WebMarkupContainer(ID_BAR_ITEM_LINK);
        }
        menuBar.add(barItemLink);

        if (!menuBarItem.getItems().isEmpty()) {
            menuBar.add(new AttributeModifier("class", "dropdown"));

            barItemLink.add(new AttributeModifier("data-toggle", "dropdown"));
            barItemLink.add(new AttributeModifier("class", "dropdown-toggle"));
        }

        Label label = new Label(ID_LABEL, menuBarItem.getName());
        label.setRenderBodyOnly(true);
        barItemLink.add(label);

        Label caret = new Label(ID_CARET);
        caret.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return !menuBarItem.getItems().isEmpty();
            }
        });
        barItemLink.add(caret);

        menuBar.add(new ListView<MenuItem>(ID_MENU_ITEM, menuBarItem.getItems()) {

            @Override
            protected void populateItem(ListItem<MenuItem> menuItem) {
                initMenuItem(menuItem);
            }
        });
    }

    private void initMenuItem(ListItem<MenuItem> menuItem) {
        final MenuItem item = menuItem.getModelObject();
        if (item.isMenuHeader()) {
            menuItem.add(new AttributeModifier("class", "dropdown-header"));
        } else if (item.isDivider()) {
            menuItem.add(new AttributeModifier("class", "divider"));
        }

        WebMarkupContainer menuItemBody;
        if (item.isMenuHeader() || item.isDivider()) {
            menuItemBody = new MenuDividerPanel(ID_MENU_ITEM_BODY, menuItem.getModel());
        } else {
            menuItemBody = new MenuLinkPanel(ID_MENU_ITEM_BODY, menuItem.getModel());
        }
        menuItemBody.setRenderBodyOnly(true);
        menuItem.add(menuItemBody);
    }
}
