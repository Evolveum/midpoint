/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.validator.processor;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.validator.UpgradeObjectProcessor;
import com.evolveum.midpoint.schema.validator.UpgradePhase;
import com.evolveum.midpoint.schema.validator.UpgradePriority;
import com.evolveum.midpoint.schema.validator.UpgradeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LensProjectionContextType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

@SuppressWarnings("unused")
public class AccountPasswordPolicyProcessor implements UpgradeObjectProcessor<ObjectType> {

    public static final ItemName F_ACCOUNT_PASSWORD_POLICY = new ItemName(SchemaConstantsGenerated.NS_COMMON, "accountPasswordPolicy");

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
                object, path, LensProjectionContextType.class, LensProjectionContextType.F_ACCOUNT_PASSWORD_POLICY_REF)
                || matchParentTypeAndItemName(
                object, path, LensProjectionContextType.class, F_ACCOUNT_PASSWORD_POLICY);
    }

    @Override
    public boolean process(PrismObject<ObjectType> object, ItemPath path) throws Exception {
        LensProjectionContextType parent = getItemParent(object, path);
        parent.setAccountPasswordPolicy(null);
        parent.setAccountPasswordPolicyRef(null);

        return true;
    }
}
