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
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAuthenticationModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;

@SuppressWarnings("unused")
public class AuthenticationNameProcessor implements UpgradeObjectProcessor<SecurityPolicyType> {

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
                object, path, AuthenticationSequenceType.class, AuthenticationSequenceType.F_NAME)
                || matchParentTypeAndItemName(
                object, path, AuthenticationSequenceModuleType.class, AuthenticationSequenceModuleType.F_NAME)
                || matchParentTypeAndItemName(
                object, path, AbstractAuthenticationModuleType.class, AbstractAuthenticationModuleType.F_NAME);
    }

    @Override
    public boolean process(PrismObject<SecurityPolicyType> object, ItemPath path) throws Exception {
        Object parent = getItemParent(object, path);
        if (parent instanceof AuthenticationSequenceType) {
            AuthenticationSequenceType auth = (AuthenticationSequenceType) parent;
            if (auth.getIdentifier() == null) {
                auth.setIdentifier(auth.getName());
            }
            return true;
        }

        if (parent instanceof AuthenticationSequenceModuleType) {
            AuthenticationSequenceModuleType module = (AuthenticationSequenceModuleType) parent;
            if (module.getIdentifier() == null) {
                module.setIdentifier(module.getName());
            }
            return true;
        }

        if (parent instanceof AbstractAuthenticationModuleType) {
            AbstractAuthenticationModuleType module = (AbstractAuthenticationModuleType) parent;
            if (module.getIdentifier() == null) {
                module.setIdentifier(module.getName());
            }
            return true;
        }

        return false;
    }
}
