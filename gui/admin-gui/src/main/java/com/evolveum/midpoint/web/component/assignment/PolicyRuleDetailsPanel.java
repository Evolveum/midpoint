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
import java.util.List;

import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

/**
 * Created by honchar.
 */
public class PolicyRuleDetailsPanel<F extends FocusType> extends AbstractAssignmentDetailsPanel<F> {
    private static final long serialVersionUID = 1L;


    
    public PolicyRuleDetailsPanel(String id, Form<?> form, IModel<ContainerValueWrapper<AssignmentType>> model){
        super(id, form, model);
    }

	@Override
	protected List<ItemPath> collectContainersToShow() {
		List<ItemPath> containersToShow = new ArrayList<>();
		containersToShow.add(getAssignmentPath().append(AssignmentType.F_POLICY_RULE));
		
		containersToShow.add(getAssignmentPath().append(AssignmentType.F_POLICY_EXCEPTION));
		
		return containersToShow;
	}


}
