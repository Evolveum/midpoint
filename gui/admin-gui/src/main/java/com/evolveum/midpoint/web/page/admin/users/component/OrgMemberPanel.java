/**
 * Copyright (c) 2015-2017 Evolveum
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
package com.evolveum.midpoint.web.page.admin.users.component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.component.ChooseMemberPopup;
import com.evolveum.midpoint.gui.api.component.ChooseOrgMemberPopup;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntryOrEmpty;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.input.QNameObjectTypeChoiceRenderer;
import com.evolveum.midpoint.web.component.input.RelationDropDownChoicePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.web.page.self.PageAssignmentShoppingCart;
import com.evolveum.midpoint.web.security.GuiAuthorizationConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.Validate;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.InOidFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrgFilter.Scope;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.RelationTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.FocusSummaryPanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenu;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.util.ObjectWrapperUtil;
import com.evolveum.midpoint.web.page.admin.configuration.component.HeaderMenuAction;
import com.evolveum.midpoint.web.page.admin.roles.AbstractRoleMemberPanel;
import com.evolveum.midpoint.web.page.admin.roles.MemberOperationsHelper;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;
import com.evolveum.midpoint.web.util.StringResourceChoiceRenderer;

public class OrgMemberPanel extends AbstractRoleMemberPanel<OrgType> {

	private static final Trace LOGGER = TraceManager.getTrace(OrgMemberPanel.class);

	
	protected static final String ID_SEARCH_BY_TYPE = "searchByType";
//	protected static final String ID_SEARCH_BY_RELATION = "searchByRelation";

//	protected static final String ID_MANAGER_MENU = "managerMenu";
//	protected static final String ID_MANAGER_MENU_BODY = "managerMenuBody";

//	protected static final String ID_SEARCH_SCOPE = "searchScope";
//	protected static final String SEARCH_SCOPE_SUBTREE = "subtree";
//	protected static final String SEARCH_SCOPE_ONE = "one";
//	protected static final List<String> SEARCH_SCOPE_VALUES = Arrays.asList(SEARCH_SCOPE_SUBTREE,
//			SEARCH_SCOPE_ONE);

	protected static final ObjectTypes OBJECT_TYPES_DEFAULT = ObjectTypes.USER;

	

	protected static final String DOT_CLASS = OrgMemberPanel.class.getName() + ".";
//	protected static final String OPERATION_SEARCH_MANAGERS = DOT_CLASS + "searchManagers";
	
//	private static final String OPERATION_UNASSIGN_MANAGERS = DOT_CLASS + "unassignManagers";
	
	
	
	private static final long serialVersionUID = 1L;

	public OrgMemberPanel(String id, IModel<OrgType> model) {
		super(id, model, TableId.ORG_MEMEBER_PANEL, GuiAuthorizationConstants.ORG_MEMBERS_AUTHORIZATIONS);
		setOutputMarkupId(true);
	}

	@Override
	protected void initSearch(Form form) {

//		/// TODO: move to utils class??
		List<QName> objectTypes = WebComponentUtil.createObjectTypeList();
//		//fix for MID-3629 (we don't know the resource to search shadows on)
		objectTypes.remove(ObjectTypes.SHADOW);


	}

	@Override
	protected void initLayout() {
		super.initLayout();
	}


	@Override
	protected void removeMembersPerformed(QName objectType, QueryScope scope, Collection<QName> relations, AjaxRequestTarget target) {
		super.removeMembersPerformed(objectType, scope, relations, target);
		MemberOperationsHelper.removeOtherOrgMembersPerformed(getPageBase(), getModelObject(), scope, getActionQuery(scope, relations), relations, target);
	}

	@Override
	protected List<QName> getSupportedRelations() {
		// TODO Auto-generated method stub
		return null;
	}

}
