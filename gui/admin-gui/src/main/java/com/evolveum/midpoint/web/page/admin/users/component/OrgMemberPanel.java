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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.DropDownFormGroup;
import com.evolveum.midpoint.web.page.admin.roles.AbstractRoleMemberPanel;
import com.evolveum.midpoint.web.page.admin.roles.MemberOperationsHelper;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AreaCategoryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class OrgMemberPanel extends AbstractRoleMemberPanel<OrgType> {

	private static final Trace LOGGER = TraceManager.getTrace(OrgMemberPanel.class);

	
	protected static final String ID_SEARCH_BY_TYPE = "searchByType";

	protected static final ObjectTypes OBJECT_TYPES_DEFAULT = ObjectTypes.USER;

	

	protected static final String DOT_CLASS = OrgMemberPanel.class.getName() + ".";
	
	private static final long serialVersionUID = 1L;

	public OrgMemberPanel(String id, IModel<OrgType> model, TableId tableId, Map<String, String> authorizations) {
		super(id, model, tableId, authorizations);
		setOutputMarkupId(true);
	}

	@Override
	protected void initLayout() {
		super.initLayout();
	}

	@Override
	protected ObjectQuery createMemberQuery(boolean indirect, Collection<QName> relations) {
		ObjectTypes searchType = getSearchType();
		if (SEARCH_SCOPE_ONE.equals(getOrgSearchScope())) {
			if (FocusType.class.isAssignableFrom(searchType.getClassDefinition())) {
				return super.createMemberQuery(indirect, relations);
			}
			else {
				ObjectReferenceType ref = MemberOperationsHelper.createReference(getModelObject(), getSelectedRelation());
				return QueryBuilder.queryFor(searchType.getClassDefinition(), getPageBase().getPrismContext())
						.isDirectChildOf(ref.asReferenceValue()).build();
			}
		}
		
		String oid = getModelObject().getOid();

		ObjectReferenceType ref = MemberOperationsHelper.createReference(getModelObject(), getSelectedRelation());
		ObjectQuery query = QueryBuilder.queryFor(searchType.getClassDefinition(), getPageBase().getPrismContext())
				.isChildOf(ref.asReferenceValue()).build();

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Searching members of org {} with query:\n{}", oid, query.debugDump());
		}
		return query;
	
	}
	
	protected String getOrgSearchScope() {
		DropDownFormGroup<String> searchorgScope = (DropDownFormGroup<String>) get(
				createComponentPath(ID_FORM, ID_SEARCH_SCOPE));
		return searchorgScope.getModelObject();
	}
	
	@Override
	protected <O extends ObjectType> void assignMembers(AjaxRequestTarget target, List<QName> availableRelationList) {
		MemberOperationsHelper.assignOrgMembers(getPageBase(), getModelObject(), target, availableRelationList);
	}

	@Override
	protected void unassignMembersPerformed(QName objectType, QueryScope scope, Collection<QName> relations, AjaxRequestTarget target) {
		super.unassignMembersPerformed(objectType, scope, relations, target);
		MemberOperationsHelper.unassignOtherOrgMembersPerformed(getPageBase(), getModelObject(), scope, getActionQuery(scope, relations), relations, target);
	}
	
	@Override
	protected List<QName> getSupportedObjectTypes() {
		List<QName> objectTypes = WebComponentUtil.createObjectTypeList();
		objectTypes.remove(ShadowType.COMPLEX_TYPE);
		objectTypes.remove(ObjectType.COMPLEX_TYPE);
		return objectTypes;
	}
	
	@Override
	protected <O extends ObjectType> Class<O> getDefaultObjectType() {
		return (Class) UserType.class;
	}

	@Override
	protected List<QName> getSupportedRelations() {
		return WebComponentUtil.getCategoryRelationChoices(AreaCategoryType.ORGANIZATION, getPageBase());
	}
	
	

}
