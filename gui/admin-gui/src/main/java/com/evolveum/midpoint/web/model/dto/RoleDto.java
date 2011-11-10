/*
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.web.model.dto;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.web.bean.AssignmentBean;
import com.evolveum.midpoint.web.controller.util.ContainsAssignment;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.RoleType;

/**
 * 
 * @author lazyman
 * 
 */
public class RoleDto extends ExtensibleObjectDto<RoleType> implements ContainsAssignment {

	private static final long serialVersionUID = -6609121465638251441L;
	private List<AssignmentBean> assignments;
	private ModelService model;

	public RoleDto(RoleType role) {
		super(role);
		if (role != null) {
			createAssignments(role);
		}
	}

	public RoleDto(RoleType role, ModelService model) {
		super(role);
		this.model = model;
		if (role != null) {
			createAssignments(role);
		}
	}

	@Override
	public void setXmlObject(RoleType xmlObject) {
		super.setXmlObject(xmlObject);

		if (xmlObject != null) {
			createAssignments(xmlObject);
		}
	}

	@Override
	public List<AssignmentBean> getAssignments() {
		if (assignments == null) {
			assignments = new ArrayList<AssignmentBean>();
		}

		return assignments;
	}

	private void createAssignments(RoleType role) {
		getAssignments().clear();
		int id = 0;
		for (AssignmentType assignment : role.getAssignment()) {
			getAssignments().add(new AssignmentBean(id, assignment, model));
			id++;
		}
	}
	
	@Override
	public void normalizeAssignments() {
		List<AssignmentType> assignmentTypes = getXmlObject().getAssignment();
		assignmentTypes.clear();
		for (AssignmentBean bean : getAssignments()) {
			assignmentTypes.add(bean.getAssignment());
		}
	}
}