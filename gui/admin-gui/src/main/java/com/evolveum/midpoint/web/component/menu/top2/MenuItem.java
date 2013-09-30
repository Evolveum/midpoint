package com.evolveum.midpoint.web.component.menu.top2;

import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.IModel;

/**
 * @author lazyman
 */
public class MenuItem {

    private IModel<String> name;
    private VisibleEnableBehaviour visibleEnable;
    private Class<? extends WebPage> page;
    private boolean menuHeader;

    public MenuItem(IModel<String> name) {
        this(name, false, null, null);
    }

    public MenuItem(IModel<String> name, Class<? extends WebPage> page) {
        this(name, false, page, null);
    }

    public MenuItem(IModel<String> name, boolean menuHeader, Class<? extends WebPage> page,
                    VisibleEnableBehaviour visibleEnable) {
        this.name = name;
        this.menuHeader = menuHeader;
        this.page = page;
        this.visibleEnable = visibleEnable;
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

    public boolean isMenuHeader() {
        return menuHeader;
    }

    public boolean isDivider() {
        return getPage() == null;
    }
}
