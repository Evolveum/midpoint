/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.mappings;

import com.evolveum.midpoint.model.common.util.PopulatorUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ItemDeltaCollectionsUtil;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.Source;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.List;

/**
 *
 */
public class AutoassignRoleMappingEvaluationRequest extends FocalMappingEvaluationRequest<AutoassignMappingType, AbstractRoleType> {

    // Internal state
    private PrismContainerDefinition<AssignmentType> assignmentDef;
    private AssignmentType assignmentType;

    public AutoassignRoleMappingEvaluationRequest(@NotNull AutoassignMappingType mapping, @NotNull AbstractRoleType role) {
        super(mapping, MappingKindType.AUTO_ASSIGN, role);
    }

    @Override
    public <V extends PrismValue, D extends ItemDefinition, AH extends AssignmentHolderType> Source<V, D> constructDefaultSource(
            ObjectDeltaObject<AH> focusOdo) throws SchemaException {
        PrismObject<AH> focus = focusOdo.getAnyObject();
        assignmentDef = focus.getDefinition().findContainerDefinition(FocusType.F_ASSIGNMENT);
        PrismContainer<AssignmentType> assignment = assignmentDef.instantiate();
        assignmentType = assignment.createNewValue().asContainerable();
        QName relation;
        AssignmentPropertiesSpecificationType assignmentProperties = mapping.getAssignmentProperties();
        if (assignmentProperties != null) {
            relation = assignmentProperties.getRelation();
            assignmentType.getSubtype().addAll(assignmentProperties.getSubtype());
        } else {
            relation = null;
        }
        assignmentType.targetRef(originObject.getOid(), originObject.asPrismObject().getDefinition().getTypeName(), relation);

        Source<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> source =
                new Source<>(assignment, null, assignment, FocusType.F_ASSIGNMENT, assignmentDef);
        //noinspection unchecked
        return (Source<V, D>) source;
    }

    @Override
    public void mappingPreExpression(ExpressionEvaluationContext context, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        PopulateType populate = mapping.getPopulate();
        if (populate == null) {
            return;
        }
        List<ItemDelta<PrismValue, ItemDefinition>> populateItemDeltas = PopulatorUtil
                .computePopulateItemDeltas(populate, assignmentDef, context.getVariables(), context,
                        context.getContextDescription(), context.getTask(), result);
        if (populateItemDeltas != null) {
            ItemDeltaCollectionsUtil.applyTo(populateItemDeltas, assignmentType.asPrismContainerValue());
        }
    }

    @Override
    public ObjectTemplateMappingEvaluationPhaseType getEvaluationPhase() {
        return ObjectTemplateMappingEvaluationPhaseType.BEFORE_ASSIGNMENTS;
    }

    @Override
    public void shortDump(StringBuilder sb) {
        sb.append("autoassign mapping ");
        sb.append("'").append(getMappingInfo()).append("' in ").append(originObject);
    }
}
