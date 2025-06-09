/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.validator.processor;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.validator.UpgradeObjectProcessor;
import com.evolveum.midpoint.schema.validator.UpgradePhase;
import com.evolveum.midpoint.schema.validator.UpgradePriority;
import com.evolveum.midpoint.schema.validator.UpgradeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@SuppressWarnings("unused")
public class CleanupPolicyProcessor implements UpgradeObjectProcessor<ObjectType> {

    @Override
    public UpgradePhase getPhase() {
        return UpgradePhase.BEFORE;
    }

    @Override
    public UpgradePriority getPriority() {
        return UpgradePriority.NECESSARY;
    }

    @Override
    public UpgradeType getType() {
        return UpgradeType.SEAMLESS;
    }

    @Override
    public boolean isApplicable(PrismObject<?> object, ItemPath path) {
        return matchObjectTypeAndPathTemplate(
                object, path, SystemConfigurationType.class, ItemPath.create(
                        SystemConfigurationType.F_CLEANUP_POLICY,
                        CleanupPoliciesType.F_OBJECT_RESULTS))
                ||
                matchObjectTypeAndPathTemplate(
                        object, path, TaskType.class, ItemPath.create(
                                TaskType.F_ACTIVITY,
                                ActivityDefinitionType.F_WORK,
                                WorkDefinitionsType.F_CLEANUP,
                                CleanupWorkDefinitionType.F_POLICIES,
                                CleanupPoliciesType.F_OBJECT_RESULTS));
    }

    @Override
    public boolean process(PrismObject<ObjectType> object, ItemPath path) throws Exception {
        ItemPath allExceptLast = path.allExceptLast();
        PrismContainer<CleanupPoliciesType> container = object.findContainer(allExceptLast);
        CleanupPoliciesType policies = container.getRealValue();

        CleanupPolicyType policy = policies.getObjectResults();
        policies.setObjectResults(null);
        if (policies.getSimpleOperationExecutions() == null) {
            policies.setSimpleOperationExecutions(createOperationExecutionCleanupPolicyType(policy));
        }

        if (policies.getComplexOperationExecutions() == null) {
            policies.setComplexOperationExecutions(createOperationExecutionCleanupPolicyType(policy));
        }

        return true;
    }

    private OperationExecutionCleanupPolicyType createOperationExecutionCleanupPolicyType(CleanupPolicyType policy) {
        OperationExecutionCleanupPolicyType result = new OperationExecutionCleanupPolicyType();
        result.setMaxAge(policy.getMaxAge());
        result.setMaxRecords(policy.getMaxRecords());
        return result;
    }
}
