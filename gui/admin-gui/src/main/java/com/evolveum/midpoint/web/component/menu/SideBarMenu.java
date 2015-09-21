package com.evolveum.midpoint.web.component.menu;

import com.evolveum.midpoint.web.component.util.SimplePanel;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import java.util.List;

/**
 * @author Viliam Repan (lazyman)
 */
public class SideBarMenu extends SimplePanel<List<MainMenuItem>> {

    private static final String ID_MENU_ITEMS = "menuItems";
    private static final String ID_MENU_ITEM = "menuItem";

    public SideBarMenu(String id, IModel<List<MainMenuItem>> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {
        ListView<MainMenuItem> menuItems = new ListView<MainMenuItem>(ID_MENU_ITEMS, getModel()) {

            @Override
            protected void populateItem(ListItem<MainMenuItem> listItem) {
                MainMenuPanel menuItem = new MainMenuPanel(ID_MENU_ITEM, listItem.getModel());
                listItem.add(menuItem);
            }
        };
        add(menuItems);
    }
}
