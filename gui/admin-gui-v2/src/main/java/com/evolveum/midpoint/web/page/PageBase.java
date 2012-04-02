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

import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.component.login.LoginPanel;
import com.evolveum.midpoint.web.component.menu.left.LeftMenu;
import com.evolveum.midpoint.web.component.menu.left.LeftMenuItem;
import com.evolveum.midpoint.web.component.menu.top.TopMenu;
import com.evolveum.midpoint.web.component.menu.top.TopMenuItem;
import com.evolveum.midpoint.web.component.menu.top2.BottomMenuItem;
import com.evolveum.midpoint.web.component.menu.top2.TopMenu2;

import org.apache.commons.lang.Validate;
import org.apache.wicket.devutils.debugbar.DebugBar;
import org.apache.wicket.markup.html.WebPage;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public abstract class PageBase extends WebPage {

	public PageBase() {
        List<TopMenuItem> topMenuItems = getTopMenuItems();
        Validate.notNull(topMenuItems, "Top menu item list must not be null.");

        List<BottomMenuItem> bottomMenuItems = getBottomMenuItems();
        Validate.notNull(bottomMenuItems, "Bottom menu item list must not be null.");

//        add(new TopMenu("topMenu", topMenuItems));
        add(new TopMenu2("topMenu2", topMenuItems, bottomMenuItems));
        add(new LeftMenu("leftMenu", getLeftMenuItems()));
        
        LoginPanel loginPanel = new LoginPanel("loginPanel");
        /*if(loginPanel.getIsAdminLoggedIn()){
        	add(loginPanel);
        }*/
        add(loginPanel);
    }

    protected MidPointApplication getMidpointApplication() {
        return (MidPointApplication) getApplication();
    }

    public abstract List<TopMenuItem> getTopMenuItems();

    public abstract List<BottomMenuItem> getBottomMenuItems();

    public abstract List<LeftMenuItem> getLeftMenuItems();
}
