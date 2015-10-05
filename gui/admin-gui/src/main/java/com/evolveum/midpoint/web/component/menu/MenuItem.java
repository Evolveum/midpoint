package com.evolveum.midpoint.web.component.menu;

import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.io.Serializable;

/**
 * @author Viliam Repan (lazyman)
 */
public class MenuItem implements Serializable {

    private IModel<String> name;
    private Class<? extends WebPage> page;
    private PageParameters params;

    private VisibleEnableBehaviour visibleEnable;

    public MenuItem(IModel<String> name, Class<? extends WebPage> page) {
        this(name, page, null, null);
    }

    public MenuItem(IModel<String> name, Class<? extends WebPage> page,
                    PageParameters params, VisibleEnableBehaviour visibleEnable) {
        this.name = name;
        this.page = page;
        this.params = params;
        this.visibleEnable = visibleEnable;
    }

    public IModel<String> getName() {
        return name;
    }

    public Class<? extends WebPage> getPage() {
        return page;
    }

    public PageParameters getParams() {
        return params;
    }

    public VisibleEnableBehaviour getVisibleEnable() {
        return visibleEnable;
    }
}
