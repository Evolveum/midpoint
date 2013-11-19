/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.web.page.admin.users.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jvnet.jaxb2_commons.lang.Validate;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.orgStruct.NodeDto;
import com.evolveum.midpoint.web.component.orgStruct.NodeType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * @author mserbak
 */
public class OrgStructDto<T extends ObjectType> implements Serializable {
	private List<NodeDto> orgUnitList;
	private List<NodeDto> userList;
	private OperationResult result = new OperationResult(OrgStructDto.class.getName());

	public OrgStructDto(List<PrismObject<T>> orgUnitList, NodeDto parent) {
		this(orgUnitList, parent, null);
	}
	
	public OrgStructDto(List<PrismObject<T>> orgUnitList, NodeDto parent, OperationResult result) {
		Validate.notNull(orgUnitList);
		if(result != null) {
			this.result = result;
		}
		initNodes(orgUnitList, parent);
	}

	private void initNodes(List<PrismObject<T>> list, NodeDto parent) {
		listNodes:
		for (PrismObject<T> node : list) {
			ObjectType nodeObject = node.asObjectable();

			if (nodeObject instanceof OrgType) {
				if (orgUnitList == null) {
					orgUnitList = new ArrayList<NodeDto>();
				}
				OrgType org = (OrgType) nodeObject;
				orgUnitList.add(new NodeDto(parent, getName(org.getDisplayName(), org.getName()), org.getOid(),
						NodeType.FOLDER));

			} else if (nodeObject instanceof UserType) {
				if (userList == null) {
					userList = new ArrayList<NodeDto>();
				}
				UserType user = (UserType) nodeObject;
				if(!userList.isEmpty()) {
					for (NodeDto userDto : userList) {
						if(userDto.getOid().equals(user.getOid())) {
							userDto.addTypeToListTypes(getRelation(parent, user));
							continue listNodes;
						}
					}
				}
				userList.add(new NodeDto(parent, getName(user.getFullName(), user.getName()), user.getOid(),
                        getRelation(parent, user)));
			}
		}

        if (orgUnitList != null) {
            Collections.sort(orgUnitList);
        }
        if (userList != null) {
            Collections.sort(userList);
        }
	}

    private String getName(PolyStringType name, PolyStringType defaultValue) {
        if (name != null && StringUtils.isNotEmpty(name.getOrig())) {
            return name.getOrig();
        }

        return defaultValue.getOrig();
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

	public NodeType getRelation(NodeDto parent, UserType user) {
		List<ObjectReferenceType> orgRefList = user.getParentOrgRef();
		ObjectReferenceType orgRef = null;

		for (ObjectReferenceType orgRefType : orgRefList) {
			if (orgRefType.getOid()!= null && orgRefType.getOid().equals(parent.getOid())) {
				orgRef = orgRefType;
				break;
			}
		}
		
		if(orgRef == null) {
			OperationResult subresult = result.createSubresult("getRelation - Getting NodeType for node: " + user.getName());
			subresult.recordFatalError("ObjectReferenceType is undefined/incorrect");
			return null;
		}

		if (orgRef.getRelation() == null) {
			return NodeType.MEMBER;
		}
		QName relation = orgRef.getRelation();

		if (SchemaConstants.ORG_MANAGER.equals(relation)) {
			return NodeType.MANAGER;
		}
		return null;
	}

}
