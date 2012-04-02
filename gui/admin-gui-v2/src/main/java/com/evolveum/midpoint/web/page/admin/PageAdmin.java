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

package com.evolveum.midpoint.web.page.admin;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.web.component.menu.left.LeftMenuItem;
import com.evolveum.midpoint.web.component.menu.top.TopMenuItem;
import com.evolveum.midpoint.web.component.menu.top2.BottomMenuItem;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.home.PageHome;
import com.evolveum.midpoint.web.page.admin.users.PageUsers;
import com.evolveum.midpoint.web.page.login.PageLogin;

/**
 * @author lazyman
 */
public class PageAdmin extends PageBase {

	@Override
	public List<TopMenuItem> getTopMenuItems() {
		List<TopMenuItem> items = new ArrayList<TopMenuItem>();
		items.add(new TopMenuItem("pageAdmin.home", "pageAdmin.home.description", PageHome.class));
		items.add(new TopMenuItem("pageAdmin.workItems", "pageAdmin.workItems.description", PageHome.class));
		items.add(new TopMenuItem("pageAdmin.users", "pageAdmin.users.description", PageUsers.class));
		items.add(new TopMenuItem("pageAdmin.serverTasks", "pageAdmin.serverTasks.description",
				PageHome.class));
		items.add(new TopMenuItem("pageAdmin.roles", "pageAdmin.roles.description", PageUsers.class));
		items.add(new TopMenuItem("pageAdmin.resources", "pageAdmin.resources.description", PageHome.class));
		items.add(new TopMenuItem("pageAdmin.configuration", "pageAdmin.configuration.description",
				PageLogin.class));

		return items;
	}

	@Override
	public List<BottomMenuItem> getBottomMenuItems() {
		List<BottomMenuItem> items = new ArrayList<BottomMenuItem>();

		items.add(new BottomMenuItem("pageAdmin.users.newUser", PageUsers.class));
		items.add(new BottomMenuItem("pageAdmin.users.listUsers", PageUsers.class));
		items.add(new BottomMenuItem("pageAdmin.users.something", PageUsers.class));
		items.add(new BottomMenuItem("pageAdmin.users.whatever", PageUsers.class));

		return items;
	}

	@Override
	public List<LeftMenuItem> getLeftMenuItems() {
		return new ArrayList<LeftMenuItem>();
	}
}
