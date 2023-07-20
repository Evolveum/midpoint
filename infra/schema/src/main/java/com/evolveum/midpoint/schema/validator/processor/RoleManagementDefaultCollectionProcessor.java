/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.validator.processor;

import java.util.Objects;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.validator.UpgradeObjectProcessor;
import com.evolveum.midpoint.schema.validator.UpgradePhase;
import com.evolveum.midpoint.schema.validator.UpgradePriority;
import com.evolveum.midpoint.schema.validator.UpgradeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@SuppressWarnings("unused")
public class RoleManagementDefaultCollectionProcessor implements UpgradeObjectProcessor<SystemConfigurationType> {

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
        return matchObjectTypeAndPathTemplate(object, path, SystemConfigurationType.class,
                ItemPath.create(SystemConfigurationType.F_ROLE_MANAGEMENT, RoleManagementConfigurationType.F_DEFAULT_COLLECTION));
    }

    @Override
    public boolean process(PrismObject<SystemConfigurationType> object, ItemPath path) {
        SystemConfigurationType system = object.asObjectable();
        RoleManagementConfigurationType roleManagement = system.getRoleManagement();
        ObjectCollectionUseType collection = roleManagement.getDefaultCollection();
        if (collection == null) {
            return false;
        }

        String uri = collection.getCollectionUri();

        RoleCatalogType roleCatalog = getRoleCatalog(system);
        RoleCollectionViewType roleCollection = roleCatalog.getCollection().stream()
                .filter(c -> Objects.equals(uri, c.getCollectionIdentifier()))
                .findFirst()
                .orElse(null);

        if (roleCollection == null) {
            roleCollection = new RoleCollectionViewType();
            roleCollection.setIdentifier(uri);
            roleCollection.setCollectionIdentifier(uri);

            roleCatalog.getCollection().add(roleCollection);
        }

        roleCollection.setDefault(true);

        roleManagement.setDefaultCollection(null);
        if (roleManagement.asPrismContainerValue().isEmpty()) {
            system.setRoleManagement(null);
        }

        return true;
    }
}
