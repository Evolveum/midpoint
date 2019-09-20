/*
 * Copyright (c) 2019-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector.mappings;

import com.evolveum.midpoint.model.common.mapping.MappingPreExpression;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.Source;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateMappingEvaluationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.jetbrains.annotations.NotNull;

/**
 * Contains some of the information necessary to evaluate a mapping. It is used when mappings are collected e.g. from
 * template and its referenced sub-templates, auto-assigned roles, or (in the future) assignments. Each of these mappings need
 * to go along some minimal context (e.g. the holding template, role, or assignment path) that is to be used when mapping
 * is evaluated.
 *
 * @author semancik
 */
public abstract class FocalMappingEvaluationRequest<MT extends MappingType, OO extends ObjectType> implements ShortDumpable,
		MappingPreExpression {

	@NotNull protected final MT mapping;
	@NotNull protected final OO originObject;

	FocalMappingEvaluationRequest(@NotNull MT mapping, @NotNull OO originObject) {
		this.mapping = mapping;
		this.originObject = originObject;
	}

	// Internal state
	PrismContainerDefinition<AssignmentType> assignmentDef;
	AssignmentType assignmentType;

	@NotNull
	public MT getMapping() {
		return mapping;
	}
	
	public <V extends PrismValue, D extends ItemDefinition, AH extends AssignmentHolderType> Source<V,D> constructDefaultSource(
			ObjectDeltaObject<AH> focusOdo) throws SchemaException {
		return null;
	}

	/**
	 * Executed before mapping expression is executed. It is used to populate the assignment.
	 * We need to do that just before mapping expression is executed, because we want all the sources
	 * and variables set the same way as mapping is set.
	 */
	public void mappingPreExpression(ExpressionEvaluationContext context, OperationResult result) throws SchemaException,
			ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException,
			SecurityViolationException {
	}

	@NotNull
	public OO getOriginObject() {
		return originObject;
	}
	
	public abstract ObjectTemplateMappingEvaluationPhaseType getEvaluationPhase();

}
