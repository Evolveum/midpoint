/**
 * Copyright (c) 2017-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector.focus;

import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.common.mapping.MappingPreExpression;
import com.evolveum.midpoint.model.common.util.PopulatorUtil;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ItemDeltaCollectionsUtil;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.repo.common.expression.Source;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPropertiesSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AutoassignMappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateMappingEvaluationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateMappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PopulateType;

/**
 * @author semancik
 *
 */
public class FocalMappingSpec implements ShortDumpable, MappingPreExpression {
	
	private ObjectTemplateMappingType objectTemplateMappingType;
	private ObjectTemplateType objectTemaplate;
	
	private AutoassignMappingType autoassignMappingType;
	private AbstractRoleType role;
	
	// Internal state
	private PrismContainerDefinition<AssignmentType> assignmentDef;
	private AssignmentType assignmentType;

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
	
	public <V extends PrismValue, D extends ItemDefinition, AH extends AssignmentHolderType> Source<V,D> constructDefaultSource(ObjectDeltaObject<AH> focusOdo) throws SchemaException {
		if (objectTemplateMappingType != null) {
			return null;
		}
		PrismObject<AH> focus = focusOdo.getAnyObject();
		assignmentDef = focus.getDefinition().findContainerDefinition(FocusType.F_ASSIGNMENT);
		PrismContainer<AssignmentType> assignment = assignmentDef.instantiate();
		assignmentType = assignment.createNewValue().asContainerable();
		QName relation = null;
		AssignmentPropertiesSpecificationType assignmentProperties = autoassignMappingType.getAssignmentProperties();
		if (assignmentProperties != null) {
			relation = assignmentProperties.getRelation();
			assignmentType.getSubtype().addAll(assignmentProperties.getSubtype());
		}
		assignmentType.targetRef(role.getOid(), role.asPrismObject().getDefinition().getTypeName(), relation);

		Source<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> source =
				new Source<>(assignment, null, assignment, FocusType.F_ASSIGNMENT, assignmentDef);
		return (Source<V, D>) source;
	}
	
	/**
	 * Executed before mapping expression is executed. It is used to populate the assignment.
	 * We need to do that just before mapping expression is executed, because we want all the sources
	 * and variables set the same way as mapping is set.
	 */
	public void mappingPreExpression(ExpressionEvaluationContext context) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
		if (autoassignMappingType == null) {
			return;
		}
		PopulateType populate = autoassignMappingType.getPopulate();
		if (populate == null) {
			return;
		}
		List<ItemDelta<PrismValue, ItemDefinition>> populateItemDeltas = PopulatorUtil.computePopulateItemDeltas(populate, assignmentDef, context.getVariables(), context, context.getContextDescription(), context.getTask(), context.getResult());
		if (populateItemDeltas != null) {
			ItemDeltaCollectionsUtil.applyTo(populateItemDeltas, assignmentType.asPrismContainerValue());
		}
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
