package com.evolveum.midpoint.gui.impl.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import java.util.List;

public class DetailsNavigationPanel extends BasePanel<List<DetailsNavigationMainItem>> {

    private static final String ID_MAIN_MENU = "mainMenu";
    private static final String ID_MENU_ITEM = "menuItem";

    public DetailsNavigationPanel(String id, IModel<List<DetailsNavigationMainItem>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        ListView<DetailsNavigationMainItem> mainMenu = new ListView<DetailsNavigationMainItem>(ID_MAIN_MENU, getModel()) {

            @Override
            protected void populateItem(ListItem<DetailsNavigationMainItem> item) {
                item.add(new Label(ID_MENU_ITEM, new StringResourceModel("${labelKey}", item.getModel())));
            }
        };
        add(mainMenu);
    }
}
