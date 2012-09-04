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

package com.evolveum.midpoint.web.component.orgStruct;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.LabeledWebMarkupContainer;
import org.apache.wicket.model.IModel;

import wickettree.AbstractTree;
import wickettree.AbstractTree.State;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.page.admin.users.dto.OrgStructDto;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.UserType;

/**
 * @author mserbak
 */
public class BookmarkableFolderContent extends Content {
	private static final String DOT_CLASS = BookmarkableFolderContent.class.getName() + ".";
	private static final String OPERATION_LOAD_ORGUNIT = DOT_CLASS + "load org unit";

	public BookmarkableFolderContent(final AbstractTree<NodeDto> tree) {
	}

	@Override
	public Component newContentComponent(String id, final AbstractTree<NodeDto> tree, IModel<NodeDto> model) {
		return new Node<NodeDto>(id, model) {

			@Override
			protected MarkupContainer newLinkComponent(String id, IModel<NodeDto> model) {
				final NodeDto node = model.getObject();
				if (tree.getProvider().hasChildren(node)) {
					return super.newLinkComponent(id, model);
					
				} else {
					return new LabeledWebMarkupContainer(id, model) {
					};
				}
			}

			@Override
			protected void onClick(AjaxRequestTarget target) {
				NodeDto t = getModelObject();
				if (tree.getState(t) == State.EXPANDED) {
					tree.collapse(t);
				} else {
					t.setNodes(getNodes(t));
					tree.expand(t);
				}
				target.appendJavaScript("initMenuButtons()");
			}

			@Override
			protected String getStyleClass() {
				NodeDto t = getModelObject();
				String styleClass;
				if (tree.getProvider().hasChildren(t)) {
					if (tree.getState(t) == State.EXPANDED) {
						styleClass = getOpenStyleClass();
					} else {
						styleClass = getClosedStyleClass();
					}
				} else {
					styleClass = getOtherStyleClass(t);
				}

				if (isSelected()) {
					styleClass += " " + getSelectedStyleClass();
				}

				return styleClass;
			}
		};
	}
	
	private List<NodeDto> getNodes(NodeDto parent) {
		OrgStructDto orgUnit = loadOrgUnit(parent.getOid());
		List<NodeDto> listNodes = new ArrayList<NodeDto>();

		if (orgUnit.getOrgUnitList() != null && !orgUnit.getOrgUnitList().isEmpty()) {
			for (OrgType org : orgUnit.getOrgUnitList()) {
				listNodes.add(createOrgUnit(parent, org));
			}
		}

		if (orgUnit.getUserList() != null && !orgUnit.getUserList().isEmpty()) {
			for (UserType org : orgUnit.getUserList()) {
				listNodes.add(createUserUnit(parent, org));
			}
		}
		return listNodes;
	}

	private OrgStructDto loadOrgUnit(String oid) {
		Task task = createSimpleTask(OPERATION_LOAD_ORGUNIT);
		OperationResult result = new OperationResult(OPERATION_LOAD_ORGUNIT);

		OrgStructDto newOrgModel = null;
		List<PrismObject<ObjectType>> orgUnitList;

		OrgFilter orgFilter = OrgFilter.createOrg(oid, null, "2");
		ObjectQuery query = ObjectQuery.createObjectQuery(orgFilter);

		try {
			orgUnitList = getModelService().searchObjects(ObjectType.class, query, null, task, result);
			orgUnitList = orgUnitList.subList(1, orgUnitList.size());
			//TODO: hack
			newOrgModel = new OrgStructDto(orgUnitList);
			result.recordSuccess();
		} catch (Exception ex) {
			result.recordFatalError("Unable to load org unit", ex);
		}

		if (!result.isSuccess()) {
			showResult(result);
		}

		if (newOrgModel.getOrgUnitList() == null) {
			result.recordFatalError("pageOrgStruct.message.noOrgStructDefined");
			showResult(result);
		}
		return newOrgModel;
	}

	private NodeDto createOrgUnit(NodeDto parent, OrgType unit) {
		return new NodeDto(parent, unit.getDisplayName().toString(), unit.getOid(), NodeType.FOLDER);
	}

	private NodeDto createUserUnit(NodeDto parent, UserType unit) {
		NodeType type = getRelation(parent, unit.getOrgRef());
		return new NodeDto(parent, unit.getFullName().toString(), unit.getOid(), type);
	}

	private NodeType getRelation(NodeDto parent, List<ObjectReferenceType> orgRefList) {
		ObjectReferenceType orgRef = null;

		for (ObjectReferenceType orgRefType : orgRefList) {
			if (orgRefType.getOid().equals(parent.getOid())) {
				orgRef = orgRefType;
				break;
			}
		}

		if (orgRef.getRelation() == null) {
			return null;
		}
		String relation = orgRef.getRelation().getLocalPart();

		if (relation.equals("manager")) {
			return NodeType.BOSS;
		} else if (relation.equals("member")) {
			return NodeType.MANAGER;
		} else {
			return NodeType.USER;
		}
	}
}