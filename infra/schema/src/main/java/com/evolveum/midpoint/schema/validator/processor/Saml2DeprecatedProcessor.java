/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.validator.processor;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.validator.UpgradeObjectProcessor;
import com.evolveum.midpoint.schema.validator.UpgradePhase;
import com.evolveum.midpoint.schema.validator.UpgradePriority;
import com.evolveum.midpoint.schema.validator.UpgradeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings("unused")
public class Saml2DeprecatedProcessor implements UpgradeObjectProcessor<SecurityPolicyType> {

    private static final ItemPath SAML2_PREFIX = ItemPath.create(
            SecurityPolicyType.F_AUTHENTICATION,
            AuthenticationsPolicyType.F_MODULES,
            AuthenticationModulesType.F_SAML_2
    );

    private static final List<ItemPath> DEPRECATED_PATHS = Arrays.asList(
            createPath(
                    Saml2AuthenticationModuleType.F_NETWORK),
            // serviceProvider/metadata is unsued, only serviceProvider/identityProvider/metadata is used
            createPath(
                    Saml2AuthenticationModuleType.F_SERVICE_PROVIDER,
                    Saml2ServiceProviderAuthenticationModuleType.F_METADATA),
            createPath(
                    Saml2AuthenticationModuleType.F_SERVICE_PROVIDER,
                    Saml2ServiceProviderAuthenticationModuleType.F_PROVIDER,
                    Saml2ProviderAuthenticationModuleType.F_ALIAS),
            createPath(
                    Saml2AuthenticationModuleType.F_SERVICE_PROVIDER,
                    Saml2ServiceProviderAuthenticationModuleType.F_PROVIDER,
                    Saml2ProviderAuthenticationModuleType.F_METADATA_TRUST_CHECK),
            createPath(
                    Saml2AuthenticationModuleType.F_SERVICE_PROVIDER,
                    Saml2ServiceProviderAuthenticationModuleType.F_PROVIDER,
                    Saml2ProviderAuthenticationModuleType.F_SKIP_SSL_VALIDATION),
            createPath(
                    Saml2AuthenticationModuleType.F_SERVICE_PROVIDER,
                    Saml2ServiceProviderAuthenticationModuleType.F_WANT_ASSERTIONS_SIGNED),
            createPath(
                    Saml2AuthenticationModuleType.F_SERVICE_PROVIDER,
                    Saml2ServiceProviderAuthenticationModuleType.F_NAME_ID),
            createPath(
                    Saml2AuthenticationModuleType.F_SERVICE_PROVIDER,
                    Saml2ServiceProviderAuthenticationModuleType.F_SINGLE_LOGOUT_ENABLED),
            createPath(
                    Saml2AuthenticationModuleType.F_SERVICE_PROVIDER,
                    Saml2ServiceProviderAuthenticationModuleType.F_DEFAULT_DIGEST)
    );

    private static ItemPath createPath(Object... items) {
        return ItemPath.create(SAML2_PREFIX, items);
    }

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
        return UpgradeType.PREVIEW;
    }

    @Override
    public boolean isApplicable(PrismObject<?> object, ItemPath path) {
        return DEPRECATED_PATHS.stream().anyMatch(
                p -> matchObjectTypeAndPathTemplate(object, path, SecurityPolicyType.class, p)
        );
    }

    @Override
    public boolean process(PrismObject<SecurityPolicyType> object, ItemPath path) {
        ItemPath allExceptLast = path.allExceptLast();
        if (allExceptLast.last() instanceof Number || allExceptLast.last() instanceof IdItemPathSegment) {
            // something like authentication/modules/saml2/1
            // we have to search for container first

            PrismContainer container = object.findContainer(allExceptLast.allExceptLast());
            PrismContainerValue value = container.getValue((Long) allExceptLast.last());
            Item item = value.findItem(path.lastName());
            value.remove(item);
        } else {
            Item item = object.findItem(path);
            item.getParent().remove(item);
        }

        return true;
    }
}
