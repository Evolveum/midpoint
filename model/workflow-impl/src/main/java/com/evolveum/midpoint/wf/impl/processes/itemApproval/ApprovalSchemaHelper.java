/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.processes.itemApproval;

import com.evolveum.midpoint.schema.util.cases.ApprovalContextUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ApprovalSchemaHelper {

    public void prepareSchema(ApprovalSchemaType schema, RelationResolver relationResolver, ReferenceResolver referenceResolver) {
        ApprovalContextUtil.normalizeStages(schema);
        for (ApprovalStageDefinitionType stageDef : schema.getStage()) {
            prepareStage(stageDef, relationResolver, referenceResolver);
        }
    }

    public void prepareStage(ApprovalStageDefinitionType stageDef, RelationResolver relationResolver, ReferenceResolver referenceResolver) {
        try {
            // resolves filters in approvers
            List<ObjectReferenceType> resolvedApprovers = new ArrayList<>();
            for (ObjectReferenceType ref : stageDef.getApproverRef()) {
                resolvedApprovers.addAll(referenceResolver.resolveReference(ref, "approver ref"));
            }
            // resolves approver relations
            resolvedApprovers.addAll(
                    relationResolver.getApprovers(stageDef.getApproverRelation()));
            stageDef.getApproverRef().clear();
            stageDef.getApproverRef().addAll(resolvedApprovers);
            stageDef.getApproverRelation().clear();

            // default values
            if (stageDef.getOutcomeIfNoApprovers() == null) {
                stageDef.setOutcomeIfNoApprovers(ApprovalLevelOutcomeType.REJECT);
            }
            if (stageDef.getGroupExpansion() == null) {
                stageDef.setGroupExpansion(GroupExpansionType.BY_CLAIMING_WORK_ITEMS);
            }
        } catch (CommonException e) {
            throw new SystemException("Couldn't prepare approval schema for execution: " + e.getMessage(), e);
        }
    }

    // should be called only after preparation!
    public boolean shouldBeSkipped(ApprovalSchemaType schema) {
        return schema.getStage().stream().allMatch(this::shouldBeSkipped);
    }

    private boolean shouldBeSkipped(ApprovalStageDefinitionType stage) {
        if (!stage.getApproverRelation().isEmpty()) {
            throw new IllegalStateException("Schema stage was not prepared correctly; contains unresolved approver relations: " + stage);
        }
        return stage.getOutcomeIfNoApprovers() == ApprovalLevelOutcomeType.SKIP && stage.getApproverRef().isEmpty() && stage.getApproverExpression().isEmpty();
    }
}
