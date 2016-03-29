/*
 * Copyright (c) 2010-2016 Evolveum
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
package com.evolveum.midpoint.gui.api.component;

import java.util.List;

import javax.xml.namespace.QName;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.util.ListModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.input.QNameChoiceRenderer;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class TypedAssignablePanel<T extends ObjectType> extends BasePanel<T> {

	private static final String ID_TYPE = "type";
	private static final String ID_ROLE_TABLE = "roleTable";
	private static final String ID_RESOURCE_TABLE = "resourceTable";
	private static final String ID_ORG_TABLE = "orgTable";
	
	private static final String ID_SELECTED_ROLES = "rolesSelected";
	private static final String ID_SELECTED_RESOURCES = "resourcesSelected";
	private static final String ID_SELECTED_ORGS = "orgSelected";

	private static final String ID_BUTTON_ASSIGN = "assignButton";

	private IModel<QName> typeModel;

	private PageBase parentPage;

	public TypedAssignablePanel(String id, final Class<T> type, boolean multiselect, PageBase parentPage) {
		super(id);
		this.parentPage = parentPage;
		typeModel = new LoadableModel<QName>(false) {

			@Override
			protected QName load() {
				return compileTimeClassToQName(type);
			}

		};

		initLayout(type, multiselect);
	}
	
	private List<T> getSelectedData(String id){
		return ((ObjectListPanel) get(id)).getSelectedObjects();
	}
	
	private Label  createCountLabel(String id, ObjectListPanel panel){
		Label label = new Label(id, panel.getSelectedObjects().size());
		label.setOutputMarkupId(true);
		return label;
	}

	private void initLayout(Class<T> type, final boolean multiselect) {
		DropDownChoice<QName> typeSelect = new DropDownChoice(ID_TYPE, typeModel,
				new ListModel(WebComponentUtil.createAssignableTypesList()), new QNameChoiceRenderer());
		typeSelect.add(new OnChangeAjaxBehavior() {

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				ObjectListPanel<T> listRolePanel = (ObjectListPanel<T>) get(ID_ROLE_TABLE);
//				listRolePanel.setVisible(false);
				ObjectListPanel<T> listResourcePanel = (ObjectListPanel<T>) get(ID_RESOURCE_TABLE);
				ObjectListPanel<T> listOrgPanel = (ObjectListPanel<T>) get(ID_ORG_TABLE);
				
//				listRolePanel.setVisible(true);
				
//				addOrReplace(listRolePanel);
//				addOrReplace(listResourcePanel);
				target.add(addOrReplace(listRolePanel));
				target.add(addOrReplace(listResourcePanel));
				target.add(addOrReplace(listOrgPanel));
				target.add(addOrReplace(createCountLabel(ID_SELECTED_ORGS, listOrgPanel)));
				target.add(addOrReplace(createCountLabel(ID_SELECTED_RESOURCES, listResourcePanel)));
				target.add(addOrReplace(createCountLabel(ID_SELECTED_ROLES, listRolePanel)));
			}
		});
		typeSelect.setOutputMarkupId(true);
		add(typeSelect);

		ObjectListPanel<T> listRolePanel = createObjectListPanel(ID_ROLE_TABLE, ID_SELECTED_ROLES, RoleType.COMPLEX_TYPE);
		add(listRolePanel);
		ObjectListPanel<T> listResourcePanel = createObjectListPanel(ID_RESOURCE_TABLE, ID_SELECTED_RESOURCES, ResourceType.COMPLEX_TYPE);
		add(listResourcePanel);
		ObjectListPanel<T> listOrgPanel = createObjectListPanel(ID_ORG_TABLE, ID_SELECTED_ORGS, OrgType.COMPLEX_TYPE);
		add(listOrgPanel);
		
		
		add(createCountLabel(ID_SELECTED_ORGS, listOrgPanel));
		add(createCountLabel(ID_SELECTED_RESOURCES, listResourcePanel));
		add(createCountLabel(ID_SELECTED_ROLES, listRolePanel));
		
		AjaxButton addButton = new AjaxButton(ID_BUTTON_ASSIGN,
				createStringResource("userBrowserDialog.button.addButton")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				List<T> selected = getSelectedData(ID_ROLE_TABLE);
				selected.addAll(getSelectedData(ID_RESOURCE_TABLE));
				selected.addAll(getSelectedData(ID_ORG_TABLE));
				TypedAssignablePanel.this.addPerformed(target, selected);
			}
		};
		
		addButton.add(new VisibleEnableBehaviour() {
			
			@Override
			public boolean isVisible() {
				return multiselect;
			}
		});

		add(addButton);
	}

	protected void onClick(AjaxRequestTarget target, T focus) {
		parentPage.hideMainPopup(target);
	}

	private ObjectListPanel<T> createObjectListPanel(String id, final String countId, final QName type) {
		ObjectListPanel<T> listPanel = new ObjectListPanel<T>(id, qnameToCompileTimeClass(type), parentPage) {

			@Override
			protected void onCheckboxUpdate(AjaxRequestTarget target) {
				target.add(getParent().addOrReplace(createCountLabel(countId, this)));
			}

//			@Override
//			public void addPerformed(AjaxRequestTarget target, List<T> selected) {
//				super.addPerformed(target, selected);
//				TypedAssignablePanel.this.addPerformed(target, selected);
//			}
			
			@Override
			public boolean isEditable() {
				// TODO Auto-generated method stub
				return false;
			}
			
		};
//		listPanel.setMultiSelect(multiselect);
		listPanel.setOutputMarkupId(true);
		listPanel.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return type.equals(typeModel.getObject());
			}
		});
		return listPanel;
	}

	protected void addPerformed(AjaxRequestTarget target, List<T> selected) {
		parentPage.hideMainPopup(target);
	}

	private Class qnameToCompileTimeClass(QName typeName) {
		return parentPage.getPrismContext().getSchemaRegistry().getCompileTimeClassForObjectType(typeName);
	}

	private QName compileTimeClassToQName(Class<T> type) {
		PrismObjectDefinition def = parentPage.getPrismContext().getSchemaRegistry()
				.findObjectDefinitionByCompileTimeClass(type);
		if (def == null) {
			return UserType.COMPLEX_TYPE;
		}

		return def.getTypeName();
	}

}
