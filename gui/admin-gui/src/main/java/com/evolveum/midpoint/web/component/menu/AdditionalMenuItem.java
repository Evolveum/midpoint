package com.evolveum.midpoint.web.component.menu;

import com.evolveum.midpoint.gui.api.page.PageBase;
import org.apache.wicket.model.IModel;

/**
 * Created by Kate on 19.09.2016.
 */
public class AdditionalMenuItem extends MainMenuItem {
    private String targetUrl;

    public AdditionalMenuItem(String iconClass, IModel<String> name, String targetUrl, Class<? extends PageBase> page) {
        super(iconClass, name, page, null);
        this.targetUrl = targetUrl;
    }

    public String getTargetUrl() {
        return targetUrl;
    }
}
