/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.validator.processor;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.validator.UpgradeObjectProcessor;
import com.evolveum.midpoint.schema.validator.UpgradePhase;
import com.evolveum.midpoint.schema.validator.UpgradePriority;
import com.evolveum.midpoint.schema.validator.UpgradeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalStageExecutionInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;

// todo test
@SuppressWarnings("unused")
public class ExecutionRecordProcessor implements UpgradeObjectProcessor<CaseType> {

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
        return matchParentTypeAndItemName(
                object, path, ApprovalStageExecutionInformationType.class, ApprovalStageExecutionInformationType.F_EXECUTION_RECORD);
    }

    @Override
    public boolean process(PrismObject<CaseType> object, ItemPath path) throws Exception {
        ApprovalStageExecutionInformationType information = getItemParent(object, path);
        information.setExecutionRecord(null);
        return true;
    }
}
