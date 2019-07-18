/*
 * Copyright (c) 2010-2019 Evolveum
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
package com.evolveum.midpoint.gui.impl.component.data.column;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import com.ctc.wstx.util.StringUtil;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn.ColumnType;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismPropertyValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismPropertyWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.schema.util.PolicyRuleTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.assignment.AssignmentsUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LifecycleStateModelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LifecycleStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyActionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectAssociationType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

/**
 * @author katka
 *
 */
public class PrismContainerWrapperColumnPanel<C extends Containerable> extends AbstractItemWrapperColumnPanel<PrismContainerWrapper<C>, PrismContainerValueWrapper<C>> {

	private static final long serialVersionUID = 1L;
	private static final transient Trace LOGGER = TraceManager.getTrace(PrismContainerWrapperColumn.class);
	
	PrismContainerWrapperColumnPanel(String id, IModel<PrismContainerWrapper<C>> model, ColumnType columnType) {
		super(id, model, columnType);
	}
	
	@Override
	protected String createLabel(PrismContainerValueWrapper<C> object) {
		C realValue = object.getRealValue();
		if(realValue == null) {
			return "";
		}
		
		if (PolicyConstraintsType.class.isAssignableFrom(realValue.getClass())) {
			return PolicyRuleTypeUtil.toShortString((PolicyConstraintsType) realValue);
		}
		
		if (PolicyActionsType.class.isAssignableFrom(realValue.getClass())) {
			return PolicyRuleTypeUtil.toShortString((PolicyActionsType) realValue, new ArrayList<>());
		}
		
		if (ActivationType.class.isAssignableFrom(realValue.getClass())) {
			return getActivationLabelLabel((ActivationType) realValue);
		}
		
		
		//TODO what to show?
		if (LifecycleStateModelType.class.isAssignableFrom(realValue.getClass())) {
			return realValue.toString();
		}
		
		if (LifecycleStateType.class.isAssignableFrom(realValue.getClass())) {
			LifecycleStateType state = (LifecycleStateType) realValue;
			if(StringUtils.isBlank(state.getDisplayName())) {
				return state.getName();
			}
			return state.getDisplayName();
		}
		
		if (ResourceObjectAssociationType.class.isAssignableFrom(realValue.getClass())) {
			return getAssociationLabel((ResourceObjectAssociationType) realValue);
		}
		
		if(PendingOperationType.class.isAssignableFrom(realValue.getClass())) {
			return WebComponentUtil.getPendingOperationLabel((PendingOperationType) realValue, this);
		}
		
		return realValue.toString();
		
		
	}

	private String getActivationLabelLabel(ActivationType activation){
		if (activation.getAdministrativeStatus() != null) {
			return activation.getAdministrativeStatus().value();
		}
		
		PrismContainerWrapper<AssignmentType> assignmentModel =  (PrismContainerWrapper<AssignmentType>) getModel().getObject();
		PrismPropertyWrapper<String> lifecycle = null;
		try {
			lifecycle = assignmentModel.findProperty(AssignmentType.F_LIFECYCLE_STATE);
		} catch (SchemaException e) {
			LOGGER.error("Cannot find lifecycle property: ", e.getMessage(), e);
		}
		
		String lifecycleState = getLifecycleState(lifecycle);
		
		ActivationStatusType status = WebModelServiceUtils.getAssignmentEffectiveStatus(lifecycleState, activation, getPageBase());
		return AssignmentsUtil.createActivationTitleModel(status, activation.getValidFrom(), activation.getValidTo(), getPageBase());
	
	}
	

    private String getAssociationLabel(ResourceObjectAssociationType association){
        if (association == null){
            return "";
        }
        return association != null ?
                (StringUtils.isNotEmpty(association.getDisplayName()) ? association.getDisplayName() : association.getRef().toString())
                : null;

    }

	
	private String getLifecycleState(PrismPropertyWrapper<String> lifecycle) {
		if (lifecycle == null) {
			return null;
		}
		
		List<PrismPropertyValueWrapper<String>> values = lifecycle.getValues();
		if (CollectionUtils.isEmpty(values)) {
			return null;
		}
		
		return values.iterator().next().getRealValue();
	}
	
	
	@Override
	protected Panel createValuePanel(String id, IModel<PrismContainerWrapper<C>> headerModel, PrismContainerValueWrapper<C> object) {
		throw new UnsupportedOperationException("Panels not supported for container values.");
	}

	@Override
	protected Panel createLink(String id, IModel<PrismContainerValueWrapper<C>> object) {
		throw new UnsupportedOperationException("Links not supported for container values.");
	}
}
