/*
 * Copyright (c) 2012 Evolveum
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
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.users;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.extensions.markup.html.tabs.TabbedPanel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.web.component.orgStruct.OrgStructPanel;
import com.evolveum.midpoint.web.page.admin.PageAdmin;

/**
 * @author mserbak
 *
 */
public class PageOrgStruct extends PageAdmin {

	public PageOrgStruct() {
		initLayout();
	}
	
	private void initLayout() {
		List<ITab> tabs = new ArrayList<ITab>();
		tabs.add(new TabPanel(new Model<String>("First tab"), new Model<String>("Test 1")));
		tabs.add(new TabPanel(new Model<String>("Second tab"), new Model<String>("Test 2")));
		tabs.add(new TabPanel(new Model<String>("Third tab"), new Model<String>("Test 3")));
		
		add(new TabbedPanel("tabPanel", tabs));
	}
	
	private class TabPanel extends AbstractTab {
		IModel<String> context;
		public TabPanel(IModel<String> title, IModel<String> context) {
			super(title);
			this.context = context;
		}
		
		@Override
		public WebMarkupContainer getPanel(String id) {
			return new OrgStructPanel(id, context);
		}
		
	}
}
