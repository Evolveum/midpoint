package com.evolveum.midpoint.web.component.menu;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.wicket.model.IModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Viliam Repan (lazyman)
 */
public class SideBarMenuItem implements Serializable {

    public static final String F_NAME = "name";
    public static final String F_ITEMS = "items";

    private IModel<String> name;
    private List<MainMenuItem> items;

    public SideBarMenuItem(IModel<String> name) {
        this.name = name;
    }

    public List<MainMenuItem> getItems() {
        if (items == null) {
            items = new ArrayList<>();
        }
        return items;
    }

    public IModel<String> getName() {
        return name;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("items", items)
                .append("name", name)
                .toString();
    }
}
