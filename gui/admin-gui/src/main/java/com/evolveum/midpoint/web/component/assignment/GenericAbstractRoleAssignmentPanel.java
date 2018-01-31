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
package com.evolveum.midpoint.web.component.assignment;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

public class GenericAbstractRoleAssignmentPanel extends AbstractRoleAssignmentPanel {

	private static final long serialVersionUID = 1L;

	public GenericAbstractRoleAssignmentPanel(String id, IModel<ContainerWrapper<AssignmentType>> assignmentContainerWrapperModel) {
		super(id, assignmentContainerWrapperModel);
	}

//	@Override
//	protected boolean isRelationVisible() {
//		return true;
//	}

//	@Override
//	protected <T extends ObjectType> void addSelectedAssignmentsPerformed(AjaxRequestTarget target, List<T> assignmentsList,
//			QName relation) {
//		super.addSelectedAssignmentsPerformed(target, assignmentsList, SchemaConstants.ORG_DEFAULT);
//	}
	
	@Override
	protected List<ContainerValueWrapper<AssignmentType>> postSearch(List<ContainerValueWrapper<AssignmentType>> assignments) {
		
		List<ContainerValueWrapper<AssignmentType>> resultList = new ArrayList<>();
		Task task = getPageBase().createSimpleTask("load assignment targets");
		Iterator<ContainerValueWrapper<AssignmentType>> assignmentIterator = assignments.iterator();
		while (assignmentIterator.hasNext()) {
			ContainerValueWrapper<AssignmentType> ass = assignmentIterator.next();
			AssignmentType assignment = ass.getContainerValue().asContainerable();
			if (QNameUtil.match(assignment.getTargetRef().getType(), OrgType.COMPLEX_TYPE)) {
				PrismObject<OrgType> org = WebModelServiceUtils.loadObject(assignment.getTargetRef(), getPageBase(), task, task.getResult());
				if (org != null) {
					if (org.asObjectable().getOrgType().contains("access")) {
						resultList.add(ass);
					}
				}
			}
			
		}
		
		return resultList;
	}

//	protected ObjectQuery createObjectQuery() {
//		
//		List<ContainerValueWrapper<AssignmentType>> assignments = getModelObject().getValues();
//		
//		Task task = getPageBase().createSimpleTask("load assignment targets");
//		
//		List<String> oids = new ArrayList<>(); 
//		Iterator<ContainerValueWrapper<AssignmentType>> assignmentIterator = assignments.iterator();
//		while (assignmentIterator.hasNext()) {
//			ContainerValueWrapper<AssignmentType> ass = assignmentIterator.next();
//			AssignmentType assignment = ass.getContainerValue().asContainerable();
//			PrismObject<OrgType> org = WebModelServiceUtils.loadObject(assignment.getTargetRef(), getPageBase(), task, task.getResult());
//			if (org != null) {
//				assignment.setTarget(org.asObjectable());
//			}
//			
//		}
//		ObjectQuery query =  QueryBuilder.queryFor(AssignmentType.class, getParentPage().getPrismContext())
//				.item(new ItemPath(AssignmentType.F_TARGET, OrgType.F_ORG_TYPE)).eq("access").build();
//				
//		return query;
//	}
}
