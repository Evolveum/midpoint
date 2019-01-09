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

import java.util.*;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.session.OrgTreeStateStorage;
import com.evolveum.midpoint.web.session.UsersStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_4.FocusType;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
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
import com.evolveum.midpoint.xml.ns._public.common.common_4.OrgType;

public class OrgTreeAssignablePanel  extends BasePanel<OrgType> implements Popupable{

	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(OrgTreeAssignablePanel.class);

	public static final String PARAM_ORG_RETURN = "org";

	private static final String DOT_CLASS = OrgTreeAssignablePanel.class.getName() + ".";
	private static final String OPERATION_LOAD_ASSIGNABLE_ITEMS = DOT_CLASS + "loadAssignableOrgs";

	private static final String ID_ORG_TABS = "orgTabs";
	private static final String ID_ASSIGN = "assign";
	private boolean selectable;
	List<OrgType> allTabsSelectedOrgs = new ArrayList<>();

	public OrgTreeAssignablePanel(String id, boolean selectable, PageBase parentPage) {
		super(id);
		this.selectable = selectable;
		setParent(parentPage);
		initLayout();
	}

	private void initLayout() {
		if (getPreselectedOrgsList() != null) {
			allTabsSelectedOrgs.addAll(getPreselectedOrgsList());
		}
		AbstractOrgTabPanel tabbedPanel = new AbstractOrgTabPanel(ID_ORG_TABS, getPageBase()) {

			private static final long serialVersionUID = 1L;

			@Override
			protected Panel createTreePanel(String id, Model<String> model, PageBase pageBase) {
				OrgTreePanel panel = new OrgTreePanel(id, model, selectable, pageBase, "", allTabsSelectedOrgs) {
					private static final long serialVersionUID = 1L;

					@Override
					protected IModel<Boolean> getCheckBoxValueModel(IModel<SelectableBean<OrgType>> rowModel){
						return new LoadableModel<Boolean>(true) {

							@Override
							public Boolean load() {
								for (OrgType org : allTabsSelectedOrgs){
									if (rowModel.getObject().getValue().getOid().equals(org.getOid())) {
										return true;
									}
								}
								return false;
							}
						};
					}

					@Override
					protected void onOrgTreeCheckBoxSelectionPerformed(AjaxRequestTarget target, IModel<SelectableBean<OrgType>> rowModel){
							if (rowModel != null && rowModel.getObject() != null) {
								boolean isAlreadyInList = false;
								Iterator<OrgType> it = allTabsSelectedOrgs.iterator();
								while (it.hasNext()){
									OrgType org = it.next();
									if (org.getOid().equals(rowModel.getObject().getValue().getOid())) {
										isAlreadyInList = true;
										it.remove();
									}
								}
								if (!isAlreadyInList){
									allTabsSelectedOrgs.add(rowModel.getObject().getValue());
								}
							}
						OrgTreeAssignablePanel.this.onOrgTreeCheckBoxSelectionPerformed(target, rowModel);
					}

					@Override
					protected void selectTreeItemPerformed(SelectableBean<OrgType> selected,
														   AjaxRequestTarget target) {
						onItemSelect(selected, target);
					}

					@Override
					protected OrgTreeStateStorage getOrgTreeStateStorage(){
						return null;
					}

					@Override
					protected ObjectFilter getCustomFilter(){
						return getAssignableItemsFilter();
					}
				};

				panel.setOutputMarkupId(true);
				return panel;
			}

			@Override
			protected boolean isWarnMessageVisible(){
				return false;
			}

			@Override
			protected ObjectFilter getAssignableItemsFilter(){
				return OrgTreeAssignablePanel.this.getAssignableItemsFilter();
			}

			@Override
			protected UsersStorage getUsersSessionStorage(){
				return null;
			}

		};

		tabbedPanel.setOutputMarkupId(true);
		add(tabbedPanel);

		AjaxButton assignButton = new AjaxButton(ID_ASSIGN,
				getPageBase().createStringResource("userBrowserDialog.button.addButton")) {

			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				assignSelectedOrgPerformed(getAllTabPanelsSelectedOrgs(), target);
			}
		};
		assignButton.setOutputMarkupId(true);
		assignButton.add(new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return isAssignButtonVisible();
			}
		});
		add(assignButton);

	}

	protected boolean isAssignButtonVisible(){
		return selectable;
	}

	protected void assignSelectedOrgPerformed(List<OrgType> selectedOrgs, AjaxRequestTarget target) {

	}

	public List<OrgType> getAllTabPanelsSelectedOrgs(){
		return allTabsSelectedOrgs;
	}

	protected void onItemSelect(SelectableBean<OrgType> selected, AjaxRequestTarget target) {

	}

	private ObjectFilter getAssignableItemsFilter(){
		if (getAssignmentOwnerObject() == null){
			return null;
		}
		Task task = getPageBase().createSimpleTask(OPERATION_LOAD_ASSIGNABLE_ITEMS);
		OperationResult result = task.getResult();
		return WebComponentUtil.getAssignableRolesFilter(getAssignmentOwnerObject().asPrismObject(), OrgType.class,
				isInducement() ? WebComponentUtil.AssignmentOrder.INDUCEMENT : WebComponentUtil.AssignmentOrder.ASSIGNMENT,
				result, task, getPageBase());
	}

	protected boolean isInducement(){
		return false;
	}

	protected <F extends FocusType> F getAssignmentOwnerObject(){
		return null;
	}

	protected List<OrgType> getPreselectedOrgsList(){
		return null;
	}

	protected void onOrgTreeCheckBoxSelectionPerformed(AjaxRequestTarget target, IModel<SelectableBean<OrgType>> rowModel){}

	@Override
	public int getWidth() {
		return 900;
	}

	@Override
	public int getHeight() {
		return 500;
	}

	@Override
	public String getWidthUnit(){
		return "px";
	}

	@Override
	public String getHeightUnit(){
		return "px";
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
