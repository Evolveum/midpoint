package com.evolveum.midpoint.web.component.menu.top;

import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.commons.lang.Validate;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class MenuItem implements Serializable {

    private IModel<String> name;
    private VisibleEnableBehaviour visibleEnable;
    private Class<? extends WebPage> page;
    private boolean menuHeader;
    private PageParameters pageParameters;

    private List<MenuItem> dependsOn;

    public MenuItem(IModel<String> name) {
        this(name, false, null, null);
    }

    public MenuItem(IModel<String> name, Class<? extends WebPage> page) {
        this(name, false, page, null, null);
    }

    public MenuItem(IModel<String> name, Class<? extends WebPage> page, PageParameters pageParameters) {
        this(name, false, page, null, pageParameters);
    }

    public MenuItem(IModel<String> name, boolean menuHeader, Class<? extends WebPage> page,
                    VisibleEnableBehaviour visibleEnable) {
        this(name, menuHeader, page, visibleEnable, null);
    }

    public MenuItem(IModel<String> name, boolean menuHeader, Class<? extends WebPage> page,
                    VisibleEnableBehaviour visibleEnable, PageParameters pageParameters) {
        this.name = name;
        this.menuHeader = menuHeader;
        this.page = page;
        this.visibleEnable = visibleEnable;
        this.pageParameters = pageParameters;
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

    public PageParameters getPageParameters() {
        return pageParameters;
    }

    public List<MenuItem> getDependsOn() {
        if (dependsOn == null) {
            dependsOn = new ArrayList<>();
        }
        return dependsOn;
    }

    public void setDependsOn(List<MenuItem> dependsOn) {
        this.dependsOn = dependsOn;
    }

    public void addDependsOn(MenuItem item) {
        Validate.notNull(item, "Menu item must not be null.");
        getDependsOn().add(item);
    }
}
