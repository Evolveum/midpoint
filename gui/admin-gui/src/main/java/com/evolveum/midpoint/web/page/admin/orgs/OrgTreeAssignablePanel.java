/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.web.page.admin.orgs;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

public class OrgTreeAssignablePanel extends BasePanel<OrgType> implements Popupable{

	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(OrgTreeAssignablePanel.class);

	public static final String PARAM_ORG_RETURN = "org";

	private static final String DOT_CLASS = OrgTreeAssignablePanel.class.getName() + ".";

	private static final String ID_ORG_TABS = "orgTabs";
	private static final String ID_ASSIGN = "assign";
	private boolean selectable;

	public OrgTreeAssignablePanel(String id, boolean selectable, PageBase parentPage) {
		super(id);
		this.selectable = selectable;
		setParent(parentPage);
		initLayout();
	}

	private void initLayout() {

		AbstractOrgTabPanel tabbedPanel = new AbstractOrgTabPanel(ID_ORG_TABS, getPageBase()) {

			private static final long serialVersionUID = 1L;

			@Override
			protected Panel createTreePanel(String id, Model<String> model, PageBase pageBase) {
				OrgTreePanel panel = new OrgTreePanel(id, model, selectable, pageBase) {
					private static final long serialVersionUID = 1L;

					@Override
					protected void selectTreeItemPerformed(SelectableBean<OrgType> selected,
							AjaxRequestTarget target) {
						onItemSelect(selected, target);
					}
				};

				panel.setOutputMarkupId(true);
				return panel;
			}

			@Override
			protected boolean isWarnMessageVisible(){
				return false;
			}

		};

		tabbedPanel.setOutputMarkupId(true);
		add(tabbedPanel);

		AjaxButton assignButton = new AjaxButton(ID_ASSIGN,
				getPageBase().createStringResource("userBrowserDialog.button.addButton")) {

			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				AbstractOrgTabPanel orgPanel = (AbstractOrgTabPanel) getParent().get(ID_ORG_TABS);
				OrgTreePanel treePanel = (OrgTreePanel) orgPanel.getPanel();
				List<OrgType> selectedOrgs = treePanel.getSelectedOrgs();
				assignSelectedOrgPerformed(selectedOrgs, target);

			}
		};
		assignButton.setOutputMarkupId(true);
		assignButton.add(new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return selectable;
			}
		});
		add(assignButton);

	}

	protected void assignSelectedOrgPerformed(List<OrgType> selectedOrgs, AjaxRequestTarget target) {

	}

	protected void onItemSelect(SelectableBean<OrgType> selected, AjaxRequestTarget target) {

	}

	@Override
	public int getWidth() {
		return 900;
	}

	@Override
	public int getHeight() {
		return 500;
	}

	@Override
	public StringResourceModel getTitle() {
		return new StringResourceModel("OrgTreeAssignablePanel.selectOrg");
	}

	@Override
	public Component getComponent() {
		return this;
	}

}
