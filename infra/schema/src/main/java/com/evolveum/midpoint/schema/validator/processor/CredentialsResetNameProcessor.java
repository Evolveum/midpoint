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
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsResetPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;

@SuppressWarnings("unused")
public class CredentialsResetNameProcessor implements UpgradeObjectProcessor<SecurityPolicyType> {

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
        return matchParentTypeAndItemName(object, path, CredentialsResetPolicyType.class, CredentialsResetPolicyType.F_NAME);
    }

    @Override
    public boolean process(PrismObject<SecurityPolicyType> object, ItemPath path) throws Exception {
        CredentialsResetPolicyType resetPolicy = object.asObjectable().getCredentialsReset();
        if (resetPolicy.getIdentifier() == null) {
            resetPolicy.setIdentifier(resetPolicy.getName());
        }
        resetPolicy.setName(null);

        return true;
    }
}
