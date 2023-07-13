/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.validator.processor;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.validator.UpgradeObjectProcessor;
import com.evolveum.midpoint.schema.validator.UpgradePhase;
import com.evolveum.midpoint.schema.validator.UpgradePriority;
import com.evolveum.midpoint.schema.validator.UpgradeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationModulesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.Saml2AuthenticationModuleType;
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
                        AuthenticationModulesType.F_SAML_2,
                        Saml2AuthenticationModuleType.F_NETWORK));
    }

    @Override
    public boolean process(PrismObject<SecurityPolicyType> object, ItemPath path) {
        // something like authentication/modules/saml2/1
        ItemPath allExceptLast = path.allExceptLast();

        PrismContainer item = object.findContainer(allExceptLast.namedSegmentsOnly());
        PrismContainerValue<Saml2AuthenticationModuleType> value = item.getValue((Long) allExceptLast.last());

        Saml2AuthenticationModuleType module = value.getRealValue();

        module.setNetwork(null);

        return true;
    }
}
