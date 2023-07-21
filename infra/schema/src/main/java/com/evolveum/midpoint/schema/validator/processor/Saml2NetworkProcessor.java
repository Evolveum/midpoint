/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.validator.processor;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.validator.UpgradeObjectProcessor;
import com.evolveum.midpoint.schema.validator.UpgradePhase;
import com.evolveum.midpoint.schema.validator.UpgradePriority;
import com.evolveum.midpoint.schema.validator.UpgradeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationModulesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;

@SuppressWarnings("unused")
public class Saml2NetworkProcessor implements UpgradeObjectProcessor<SecurityPolicyType> {

    @Override
    public UpgradePhase getPhase() {
        return UpgradePhase.AFTER;
    }

    @Override
    public UpgradePriority getPriority() {
        return UpgradePriority.OPTIONAL;
    }

    @Override
    public UpgradeType getType() {
        return UpgradeType.SEAMLESS;
    }

    @Override
    public boolean isApplicable(PrismObject<?> object, ItemPath path) {
        return matchObjectTypeAndPathTemplate(
                object, path , SecurityPolicyType.class, ItemPath.create(
                        SecurityPolicyType.F_AUTHENTICATION,
                        AuthenticationsPolicyType.F_MODULES,
                        AuthenticationModulesType.F_SAML2,
                        new QName(SchemaConstantsGenerated.NS_COMMON, "network")));
    }

    @Override
    public boolean process(PrismObject<SecurityPolicyType> object, ItemPath path) {
        return true;
    }
}
