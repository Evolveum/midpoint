package com.evolveum.midpoint.web.component.menu.top;

import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

/**
 * @author lazyman
 */
public class MenuDividerPanel extends Panel {

    private static String ID_MENU_ITEM_LABEL = "menuItemLabel";

    public MenuDividerPanel(String id, IModel<MenuItem> item) {
        super(id);

        initLayout(item);
    }

    private void initLayout(IModel<MenuItem> item) {
        final MenuItem menuItem = item.getObject();

        Label menuItemLabel = new Label(ID_MENU_ITEM_LABEL, menuItem.getName());
        menuItemLabel.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return menuItem.isMenuHeader();
            }
        });

        add(menuItemLabel);
    }
}
