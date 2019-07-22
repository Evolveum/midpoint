/*
 * Copyright (c) 2018 Evolveum
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

import javax.xml.namespace.QName;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

public class GenericAbstractRoleAssignmentPanel extends AbstractRoleAssignmentPanel {

	private static final long serialVersionUID = 1L;

	public GenericAbstractRoleAssignmentPanel(String id, IModel<PrismContainerWrapper<AssignmentType>> assignmentContainerWrapperModel) {
		super(id, assignmentContainerWrapperModel);
	}

	@Override
	protected List<PrismContainerValueWrapper<AssignmentType>> customPostSearch(List<PrismContainerValueWrapper<AssignmentType>> assignments) {
		
		if(assignments == null) {
			return null;
		}
		
		List<PrismContainerValueWrapper<AssignmentType>> resultList = new ArrayList<>();
		Task task = getPageBase().createSimpleTask("load assignment targets");
		Iterator<PrismContainerValueWrapper<AssignmentType>> assignmentIterator = assignments.iterator();
		while (assignmentIterator.hasNext()) {
			PrismContainerValueWrapper<AssignmentType> ass = assignmentIterator.next();
			AssignmentType assignment = ass.getRealValue();
			if (assignment == null || assignment.getTargetRef() == null) {
				continue;
			}
			if (QNameUtil.match(assignment.getTargetRef().getType(), OrgType.COMPLEX_TYPE)) {
				PrismObject<OrgType> org = WebModelServiceUtils.loadObject(assignment.getTargetRef(), getPageBase(), task, task.getResult());
				if (org != null) {
					if (FocusTypeUtil.determineSubTypes(org).contains("access")) {
						resultList.add(ass);
					}
				}
			}
			
		}
		
		return resultList;
	}

	protected ObjectFilter getSubtypeFilter(){
		ObjectFilter filter = getPageBase().getPrismContext().queryFor(OrgType.class)
				.block()
				.item(OrgType.F_SUBTYPE)
				.contains("access")
				.or()
				.item(OrgType.F_ORG_TYPE)
				.contains("access")
				.endBlock()
				.buildFilter();
		return filter;
	}

	@Override
	protected QName getAssignmentType() {
		return OrgType.COMPLEX_TYPE;
	}

}
