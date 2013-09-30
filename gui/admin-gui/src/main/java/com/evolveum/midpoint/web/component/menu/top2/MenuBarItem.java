package com.evolveum.midpoint.web.component.menu.top2;

import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.IModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class MenuBarItem implements Serializable {

    private IModel<String> name;
    private Class<? extends WebPage> page;
    private VisibleEnableBehaviour visibleEnable;
    private List<MenuItem> items;

    public MenuBarItem(IModel<String> name, Class<? extends WebPage> page) {
        this(name, page, null);
    }

    public MenuBarItem(IModel<String> name, Class<? extends WebPage> page, VisibleEnableBehaviour visibleEnable) {
        this.name = name;
        this.page = page;
        this.visibleEnable = visibleEnable;
    }

    public void addMenuItem(MenuItem item) {
        getItems().add(item);
    }

    public List<MenuItem> getItems() {
        if (items == null) {
            items = new ArrayList<MenuItem>();
        }
        return items;
    }

    public IModel<String> getName() {
        return name;
    }

    public VisibleEnableBehaviour getVisibleEnable() {
        return visibleEnable;
    }

    public Class<? extends WebPage> getPage() {
        return page;
    }
}
