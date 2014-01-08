package com.evolveum.midpoint.web.component.menu.cog;

import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

/**
 * @author lazyman
 */
public class MenuDividerPanel extends Panel {

    private static String ID_MENU_ITEM_LABEL = "menuItemLabel";

    public MenuDividerPanel(String id, IModel<InlineMenuItem> item) {
        super(id);

        initLayout(item);
    }

    private void initLayout(IModel<InlineMenuItem> item) {
        final InlineMenuItem menuItem = item.getObject();

        Label menuItemLabel = new Label(ID_MENU_ITEM_LABEL, menuItem.getLabel());
        menuItemLabel.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return menuItem.isMenuHeader();
            }
        });

        add(menuItemLabel);
    }
}
