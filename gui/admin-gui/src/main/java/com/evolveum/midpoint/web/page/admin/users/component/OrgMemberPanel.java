/**
 * Copyright (c) 2015-2018 Evolveum
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

import javax.xml.namespace.QName;

import com.evolveum.midpoint.web.session.MemberPanelStorage;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.DropDownFormGroup;
import com.evolveum.midpoint.web.page.admin.roles.AbstractRoleMemberPanel;
import com.evolveum.midpoint.web.page.admin.roles.MemberOperationsHelper;

public class OrgMemberPanel extends AbstractRoleMemberPanel<OrgType> {

	private static final Trace LOGGER = TraceManager.getTrace(OrgMemberPanel.class);


	protected static final String ID_SEARCH_BY_TYPE = "searchByType";

	protected static final ObjectTypes OBJECT_TYPES_DEFAULT = ObjectTypes.USER;



	protected static final String DOT_CLASS = OrgMemberPanel.class.getName() + ".";

	private static final long serialVersionUID = 1L;

	public OrgMemberPanel(String id, IModel<OrgType> model) {
		super(id, model);
		setOutputMarkupId(true);
	}

	@Override
	protected void initLayout() {
		super.initLayout();
	}

	@Override
	protected ObjectQuery createMemberQuery(boolean indirect, Collection<QName> relations) {
		ObjectTypes searchType = getSearchType();
		if (SearchBoxScopeType.ONE_LEVEL.equals(getOrgSearchScope())) {
			if (FocusType.class.isAssignableFrom(searchType.getClassDefinition())) {
				return super.createMemberQuery(indirect, relations);
			}
			else {
				ObjectReferenceType ref = MemberOperationsHelper.createReference(getModelObject(), getSelectedRelation());
				return getPageBase().getPrismContext().queryFor(searchType.getClassDefinition())
						.type(searchType.getClassDefinition())
						.isDirectChildOf(ref.asReferenceValue()).build();
			}
		}

		String oid = getModelObject().getOid();

		ObjectReferenceType ref = MemberOperationsHelper.createReference(getModelObject(), getSelectedRelation());
		ObjectQuery query = getPageBase().getPrismContext().queryFor(searchType.getClassDefinition())
				.type(searchType.getClassDefinition())
				.isChildOf(ref.asReferenceValue()).build();

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Searching members of org {} with query:\n{}", oid, query.debugDump());
		}
		return query;

	}

	protected SearchBoxScopeType getOrgSearchScope() {
		DropDownFormGroup<SearchBoxScopeType> searchorgScope = (DropDownFormGroup<SearchBoxScopeType>) get(
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
		if (relations != null && relations.size() > 0) {
			MemberOperationsHelper.unassignOtherOrgMembersPerformed(getPageBase(), getModelObject(), scope, getActionQuery(scope, relations), relations, target);
		}
	}

	@Override
	protected List<QName> getSupportedObjectTypes(boolean includeAbstractTypes) {
		List<QName> objectTypes = WebComponentUtil.createObjectTypeList();
		objectTypes.remove(ShadowType.COMPLEX_TYPE);
		objectTypes.remove(ObjectType.COMPLEX_TYPE);
		return objectTypes;
	}

	@Override
	protected <O extends ObjectType> Class<O> getDefaultObjectType() {
		return getMemberPanelStorage().getType() != null ? (Class) WebComponentUtil.qnameToClass(getPageBase().getPrismContext(),
				getMemberPanelStorage().getType().getTypeQName()) : (Class) UserType.class;
	}

	@Override
	protected List<QName> getSupportedRelations() {
		return WebComponentUtil.getCategoryRelationChoices(AreaCategoryType.ORGANIZATION, getPageBase());
	}

	@Override
	protected MemberPanelStorage getMemberPanelStorage(){
		String storageKey = WebComponentUtil.getStorageKeyForTableId(getTableId(getComplexTypeQName()));
		PageStorage storage = null;
		if (StringUtils.isNotEmpty(storageKey)) {
			storage = getPageBase().getSessionStorage().getPageStorageMap().get(storageKey);
			if (storage == null) {
				storage = getPageBase().getSessionStorage().initPageStorage(storageKey);
			}
		}
		return (MemberPanelStorage) storage;
	}

	@Override
	protected GuiObjectListPanelConfigurationType getAdditionalPanelConfig(){
		CompiledObjectCollectionView orgViewType = getPageBase().getCompiledUserProfile().findObjectCollectionView(OrgType.COMPLEX_TYPE, null);
		if (orgViewType == null || orgViewType.getAdditionalPanels() == null){
			return null;
		}
		return orgViewType.getAdditionalPanels().getMemberPanel();
	}
}
