/**
 * Copyright (c) 2017 Evolveum
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
package com.evolveum.midpoint.model.impl.lens.projector.focus;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.repo.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.repo.common.expression.Source;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPropertiesSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AutoassignMappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateMappingEvaluationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateMappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author semancik
 *
 */
public class FocalMappingSpec implements ShortDumpable {
	
	private ObjectTemplateMappingType objectTemplateMappingType;
	private ObjectTemplateType objectTemaplate;
	
	private AutoassignMappingType autoassignMappingType;
	private AbstractRoleType role;

	public FocalMappingSpec(ObjectTemplateMappingType objectTemplateMappingType, ObjectTemplateType objectTemaplate) {
		super();
		this.objectTemplateMappingType = objectTemplateMappingType;
		this.objectTemaplate = objectTemaplate;
	}
	
	public FocalMappingSpec(AutoassignMappingType autoassignMappingType, AbstractRoleType role) {
		super();
		this.autoassignMappingType = autoassignMappingType;
		this.role = role;
	}

	public ObjectTemplateMappingType getObjectTemplateMappingType() {
		return objectTemplateMappingType;
	}

	public AutoassignMappingType getAutoassignMappingType() {
		return autoassignMappingType;
	}
	
	public MappingType getMappingType() {
		if (objectTemplateMappingType != null) {
			return objectTemplateMappingType;
		} else {
			return autoassignMappingType;
		}
	}
	
	public <V extends PrismValue, D extends ItemDefinition, F extends FocusType> Source<V,D> getDefaultSource(ObjectDeltaObject<F> focusOdo) throws SchemaException {
		if (objectTemplateMappingType != null) {
			return null;
		}
		PrismObject<F> focus = focusOdo.getAnyObject();
		PrismContainerDefinition<AssignmentType> assignmentDef = focus.getDefinition().findContainerDefinition(FocusType.F_ASSIGNMENT);
		PrismContainer<AssignmentType> assignment = assignmentDef.instantiate();
		AssignmentType assignmentType = assignment.createNewValue().asContainerable();
		QName relation = null;
		AssignmentPropertiesSpecificationType assignmentProperties = autoassignMappingType.getAssignmentProperties();
		if (assignmentProperties != null) {
			relation = assignmentProperties.getRelation();
		}
		assignmentType.targetRef(role.getOid(), role.asPrismObject().getDefinition().getTypeName(), relation);
		Source<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> source = 
				new Source<>(assignment, null, assignment, FocusType.F_ASSIGNMENT);
		return (Source<V, D>) source;
	}
	
	public <O extends ObjectType> O getOriginObject() {
		if (objectTemplateMappingType != null) {
			return (O) objectTemaplate;
		} else {
			return (O) role;
		}
	}
	
	public ObjectTemplateMappingEvaluationPhaseType getEvaluationPhase() {
		if (objectTemplateMappingType == null) {
			return ObjectTemplateMappingEvaluationPhaseType.BEFORE_ASSIGNMENTS;
		}
		ObjectTemplateMappingEvaluationPhaseType evaluationPhase = objectTemplateMappingType.getEvaluationPhase();
		if (evaluationPhase == null) {
			return ObjectTemplateMappingEvaluationPhaseType.BEFORE_ASSIGNMENTS;
		}
		return evaluationPhase;
	}

	@Override
	public void shortDump(StringBuilder sb) {
		if (objectTemplateMappingType != null) {
			sb.append("template mapping ");
			sb.append("'").append(objectTemplateMappingType.getName()).append("' in ").append(objectTemaplate);
		} else {
			sb.append("autoassign mapping ");
			sb.append("'").append(autoassignMappingType.getName()).append("' in ").append(role);
		}
	}

}
