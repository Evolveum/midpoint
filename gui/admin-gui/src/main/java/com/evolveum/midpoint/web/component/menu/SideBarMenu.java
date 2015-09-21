package com.evolveum.midpoint.web.component.menu;

import com.evolveum.midpoint.web.component.util.SimplePanel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import java.util.List;

/**
 * @author Viliam Repan (lazyman)
 */
public class SideBarMenu extends SimplePanel<List<MainMenu>> {

    private static final String ID_MENU_ITEMS = "menuItems";
    private static final String ID_NAME = "name";
    private static final String ID_ITEMS = "items";
    private static final String ID_ITEM = "item";

    public SideBarMenu(String id, IModel<List<MainMenu>> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {
        ListView<MainMenu> menuItems = new ListView<MainMenu>(ID_MENU_ITEMS, getModel()) {

            @Override
            protected void populateItem(ListItem<MainMenu> item) {
                Label name = new Label(ID_NAME, item.getModelObject().getName());
                item.add(name);

                ListView<MainMenuItem> items = new ListView<MainMenuItem>(ID_ITEMS,
                        new PropertyModel<List<MainMenuItem>>(item.getModel(), MainMenu.F_ITEMS)) {

                    @Override
                    protected void populateItem(ListItem<MainMenuItem> listItem) {
                        MainMenuPanel item = new MainMenuPanel(ID_ITEM, listItem.getModel());
                        listItem.add(item);
                    }
                };
                item.add(items);
            }
        };
        add(menuItems);
    }
}
