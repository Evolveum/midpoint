/*
 * Copyright (C) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyStatementTypeType.APPLY;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyStatementTypeType.EXCLUDE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.projector.ProjectorProcessor;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.repo.common.EvaluatedPolicyStatements;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyStatementType;

@Component
public class PolicyStatementProcessor implements ProjectorProcessor {

    private static final String CLASS_DOT = PolicyStatementProcessor.class.getName() + ".";
    private static final String OP_PROCESS_POLICY_STATEMENTS = CLASS_DOT + "processPolicyStatements";

    <O extends ObjectType> void processPolicyStatements(
            LensFocusContext<O> focusContext, Task task, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(OP_PROCESS_POLICY_STATEMENTS);
        try {
            EvaluatedPolicyStatements evaluatedPolicyStatements = collectPolicyStatements(focusContext, result);
            focusContext.setEvaluatedPolicyStatements(evaluatedPolicyStatements);
        } catch (Exception e) {
            result.recordException(e);
            throw e;
        } finally {
            result.close();
        }
    }

    private <O extends ObjectType> EvaluatedPolicyStatements collectPolicyStatements(LensFocusContext<O> focusContext, OperationResult result) {
        List<PolicyStatementType> addedStatements = new ArrayList<>();
        List<PolicyStatementType> deletedStatements = new ArrayList<>();

        if (focusContext.getPrimaryDelta() != null) {
            ContainerDelta<PolicyStatementType> policyStatementDelta = focusContext.getPrimaryDelta().findContainerDelta(ObjectType.F_POLICY_STATEMENT);
            addedStatements.addAll(policyStatementDelta == null ? List.of() : collectRealValues(policyStatementDelta.getValuesToAdd()));
            deletedStatements.addAll(policyStatementDelta == null ? List.of() : collectRealValues(policyStatementDelta.getValuesToDelete()));
        }

        O objectNewOrCurrent = focusContext.getObjectNewOrCurrentRequired().asObjectable();

        List<PolicyStatementType> zeroStatements = new ArrayList<>();
        for (PolicyStatementType zeroStatement : objectNewOrCurrent.getPolicyStatement()) {
            if (isNotInPlusOrMinusSet(zeroStatement, addedStatements, deletedStatements)) {
                checkForPolicyViolation(zeroStatements, zeroStatement, result);
                zeroStatements.add(zeroStatement);
            }
        }
        checkConsistency(addedStatements, zeroStatements, result);

        EvaluatedPolicyStatements evaluatedPolicyStatements = new EvaluatedPolicyStatements();

        for (var deleted : deletedStatements) {
            if (APPLY.equals(deleted.getType())) {
                evaluatedPolicyStatements.addMarkRefToDelete(deleted.getMarkRef().clone());
            }
        }

        evaluatePolicyStatements(addedStatements, evaluatedPolicyStatements);
        evaluatePolicyStatements(zeroStatements, evaluatedPolicyStatements);

        return evaluatedPolicyStatements;
    }

    private void checkConsistency(@NotNull List<PolicyStatementType> addedStatements,
            @NotNull List<PolicyStatementType> zeroStatements, OperationResult result) {
        for (var added : addedStatements) {
            checkForPolicyViolation(zeroStatements, added, result);
        }
    }

    //TODO maybe later exception?
    private void checkForPolicyViolation(List<PolicyStatementType> originalStatements, PolicyStatementType added, OperationResult result) {
        for (var zero : originalStatements) {
            if (policiesInViolation(zero, added)) {
                result.recordPartialError("Policy statement with the same mark ({}) cannot be defined with different type (apply vs. exclude) , exclude always wins" + added.getMarkRef());
            }
        }
    }

    private boolean policiesInViolation(PolicyStatementType original, PolicyStatementType added) {
        return original.getMarkRef().getOid().equals(added.getMarkRef().getOid()) && original.getType() != added.getType();
    }

    private void evaluatePolicyStatements(List<PolicyStatementType> statements, EvaluatedPolicyStatements evaluatedPolicyStatements) {
        if (statements == null) {
            return;
        }
        for (var statement : statements) {
            if (APPLY.equals(statement.getType())) {
                evaluatedPolicyStatements.addMarkRefToAdd(statement.getMarkRef().clone());
            }
            if (EXCLUDE.equals(statement.getType())) {
                evaluatedPolicyStatements.addMarkRefToExclude(statement.getMarkRef().clone());
            }

        }
    }

    private boolean isNotInPlusOrMinusSet(PolicyStatementType statement, Collection<PolicyStatementType> addedStatements, Collection<PolicyStatementType> deletedStatements) {
        Collection<PolicyStatementType> allPolicyStatements = new ArrayList<>(addedStatements);
        allPolicyStatements.addAll(deletedStatements);
        if (allPolicyStatements.isEmpty()) {
            return true;
        }

        return !allPolicyStatements.contains(statement);
    }

    private Collection<PolicyStatementType> collectRealValues(Collection<PrismContainerValue<PolicyStatementType>> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream().map(v -> v.asContainerable()).toList();
    }
}
