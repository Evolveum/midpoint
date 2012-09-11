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

package com.evolveum.midpoint.web.page.admin.users.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jvnet.jaxb2_commons.lang.Validate;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.orgStruct.NodeDto;
import com.evolveum.midpoint.web.component.orgStruct.NodeType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.UserType;

/**
 * @author mserbak
 */
public class OrgStructDto<T extends ObjectType> implements Serializable {
	private List<NodeDto> orgUnitList;
	private List<NodeDto> userList;

	public OrgStructDto(List<PrismObject<T>> orgUnitList) {
		Validate.notNull(orgUnitList);
		initNodes(orgUnitList);
	}

	private void initNodes(List<PrismObject<T>> list) {
		for (PrismObject<T> node : list) {
			ObjectType nodeObject = node.asObjectable();

			if (nodeObject instanceof OrgType) {
				if (orgUnitList == null) {
					orgUnitList = new ArrayList<NodeDto>();
				}
				OrgType org = (OrgType) nodeObject;
				orgUnitList.add(new NodeDto(null, org.getDisplayName().toString(), org.getOid(),
						NodeType.FOLDER));

			} else if (nodeObject instanceof UserType) {
				if (userList == null) {
					userList = new ArrayList<NodeDto>();
				}
				UserType user = (UserType) nodeObject;
				userList.add(new NodeDto(null, user.getFullName().toString(), user.getOid(), user.getParentOrgRef()));
			}
		}
	}

	public IModel<String> getTitle() {
		if (orgUnitList != null && !orgUnitList.isEmpty()) {
			String title = orgUnitList.get(0).getDisplayName().toString();
			return new Model<String>(title);
		}
		return new Model<String>("");
	}

	public List<NodeDto> getOrgUnitDtoList() {
		return orgUnitList;
	}

	public List<NodeDto> getUserDtoList() {
		return userList;
	}

	public static NodeType getRelation(NodeDto parent, List<ObjectReferenceType> orgRefList) {
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
