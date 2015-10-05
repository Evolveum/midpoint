package com.evolveum.midpoint.web.component.menu;

import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import org.apache.wicket.model.IModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Viliam Repan (lazyman)
 */
public class MainMenuItem implements Serializable {

    public static final String F_ITEMS = "items";
    public static final String F_ICON_CLASS = "iconClass";

    private String iconClass;
    private IModel<String> name;
    private Class<? extends PageBase> page;
    private VisibleEnableBehaviour visibleEnable;
    private List<MenuItem> items;

    public MainMenuItem(String iconClass, IModel<String> name) {
        this(iconClass, name, null, null);
    }

    public MainMenuItem(String iconClass, IModel<String> name, Class<? extends PageBase> page) {
        this(iconClass, name, page, null);
    }

    public MainMenuItem(String iconClass, IModel<String> name, Class<? extends PageBase> page, List<MenuItem> items) {
        this(iconClass, name, page, items, null);
    }

    public MainMenuItem(String iconClass, IModel<String> name, Class<? extends PageBase> page,
                        List<MenuItem> items, VisibleEnableBehaviour visibleEnable) {
        this.iconClass = iconClass;
        this.items = items;
        this.name = name;
        this.page = page;
        this.visibleEnable = visibleEnable;
    }

    public String getIconClass() {
        return iconClass;
    }

    public List<MenuItem> getItems() {
        if (items == null) {
            items = new ArrayList<>();
        }
        return items;
    }

    public IModel<String> getName() {
        return name;
    }

    public VisibleEnableBehaviour getVisibleEnable() {
        return visibleEnable;
    }

    public Class<? extends PageBase> getPage() {
        return page;
    }
}
