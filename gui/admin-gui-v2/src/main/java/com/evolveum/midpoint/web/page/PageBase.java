/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.web.component.login.LocalePanel;
import com.evolveum.midpoint.web.component.login.LoginPanel;
import com.evolveum.midpoint.web.component.menu.left.LeftMenu;
import com.evolveum.midpoint.web.component.menu.left.LeftMenuItem;
import com.evolveum.midpoint.web.component.menu.top.BottomMenuItem;
import com.evolveum.midpoint.web.component.menu.top.TopMenu;
import com.evolveum.midpoint.web.component.menu.top.TopMenuItem;
import com.evolveum.midpoint.web.security.MidPointApplication;
import org.apache.commons.lang.Validate;
import org.apache.wicket.injection.Injector;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import java.util.List;

/**
 * @author lazyman
 */
public abstract class PageBase extends WebPage {

    @SpringBean(name = "modelController")
    private ModelService modelService;
    @SpringBean(name = "cacheRepositoryService")
    private RepositoryService cacheRepositoryService;

    public PageBase() {
        Injector.get().inject(this);
        validateInjection(modelService, "Model service was not injected.");
        validateInjection(cacheRepositoryService, "Cache repository service was not injected.");

        List<TopMenuItem> topMenuItems = getTopMenuItems();
        Validate.notNull(topMenuItems, "Top menu item list must not be null.");

        List<BottomMenuItem> bottomMenuItems = getBottomMenuItems();
        Validate.notNull(bottomMenuItems, "Bottom menu item list must not be null.");

        add(new TopMenu("topMenu2", topMenuItems, bottomMenuItems));
        add(new LeftMenu("leftMenu", getLeftMenuItems()));

        LoginPanel loginPanel = new LoginPanel("loginPanel");
        
        /*if(loginPanel.getIsAdminLoggedIn()){
        	add(loginPanel);
        }*/
        add(loginPanel);
    }

    private void validateInjection(Object object, String message) {
        if (object == null) {
            throw new IllegalStateException(message);
        }
    }

    protected MidPointApplication getMidpointApplication() {
        return (MidPointApplication) getApplication();
    }

    public abstract List<TopMenuItem> getTopMenuItems();

    public abstract List<BottomMenuItem> getBottomMenuItems();

    public abstract List<LeftMenuItem> getLeftMenuItems();

    protected RepositoryService getCacheRepositoryService() {
        return cacheRepositoryService;
    }

    protected ModelService getModelService() {
        return modelService;
    }

    protected StringResourceModel createStringResource(String resourceKey) {
        return new StringResourceModel(resourceKey, this, null);
    }
}
