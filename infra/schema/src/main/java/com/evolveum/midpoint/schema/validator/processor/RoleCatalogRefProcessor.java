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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleCatalogType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleManagementConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

@SuppressWarnings("unused")
public class RoleCatalogRefProcessor implements UpgradeObjectProcessor<SystemConfigurationType>, ProcessorMixin {

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
                ItemPath.create(SystemConfigurationType.F_ROLE_MANAGEMENT, RoleManagementConfigurationType.F_ROLE_CATALOG_REF));
    }

    @Override
    public boolean process(PrismObject<SystemConfigurationType> object, ItemPath path) {
        SystemConfigurationType system = object.asObjectable();
        RoleManagementConfigurationType roleManagement = system.getRoleManagement();
        ObjectReferenceType roleCatalogRef = roleManagement.getRoleCatalogRef();
        if (roleCatalogRef == null) {
            return false;
        }

        RoleCatalogType roleCatalog = getRoleCatalog(system);
        roleCatalog.setRoleCatalogRef(roleCatalogRef);

        roleManagement.setRoleCatalogRef(null);
        if (roleManagement.asPrismContainerValue().isEmpty()) {
            system.setRoleManagement(null);
        }

        return true;
    }
}
