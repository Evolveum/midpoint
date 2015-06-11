package com.evolveum.midpoint.web.component.menu.top;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

/**
 * @author lazyman
 */
public class MenuLinkPanel extends Panel {

    private static String ID_MENU_ITEM_LINK = "menuItemLink";
    private static String ID_MENU_ITEM_LABEL = "menuItemLabel";

    public MenuLinkPanel(String id, IModel<MenuItem> item) {
        super(id);

        initLayout(item);
    }

    private void initLayout(IModel<MenuItem> item) {
        MenuItem menuItem = item.getObject();

        BookmarkablePageLink menuItemLink = new BookmarkablePageLink(ID_MENU_ITEM_LINK,
                menuItem.getPage(), menuItem.getPageParameters());
        add(menuItemLink);

        Label menuItemLabel = new Label(ID_MENU_ITEM_LABEL, menuItem.getName());
        menuItemLink.add(menuItemLabel);
    }
}
