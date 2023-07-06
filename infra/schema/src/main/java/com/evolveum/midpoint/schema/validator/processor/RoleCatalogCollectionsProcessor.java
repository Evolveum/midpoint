/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.validator.processor;

import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.validator.UpgradeObjectProcessor;
import com.evolveum.midpoint.schema.validator.UpgradePhase;
import com.evolveum.midpoint.schema.validator.UpgradePriority;
import com.evolveum.midpoint.schema.validator.UpgradeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@SuppressWarnings("unused")
public class RoleCatalogCollectionsProcessor implements UpgradeObjectProcessor<SystemConfigurationType> {

    @Override
    public UpgradePhase getPhase() {
        // todo before in 4.7.* but after in 4.4.*
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
        return matchesTypeAndHasPathItem(object, path, SystemConfigurationType.class);
    }

    @Override
    public boolean process(PrismObject<SystemConfigurationType> object, ItemPath path) {
        SystemConfigurationType system = object.asObjectable();
        RoleManagementConfigurationType roleManagement = system.getRoleManagement();
        ObjectCollectionsUseType roleCatalogCollections = roleManagement.getRoleCatalogCollections();
        if (roleCatalogCollections == null) {
            return false;
        }

        RoleCatalogType roleCatalog = getRoleCatalog(system);

        List<RoleCollectionViewType> views = roleCatalogCollections.getCollection().stream()
                .map(oc -> {
                    RoleCollectionViewType collection = new RoleCollectionViewType();
                    collection.setIdentifier(oc.getCollectionUri());
                    collection.setCollectionIdentifier(oc.getCollectionUri());
                    return collection;
                })
                .collect(Collectors.toList());

        roleCatalog.getCollection().addAll(views);

        roleManagement.setRoleCatalogCollections(null);

        return false;
    }

    private RoleCatalogType getRoleCatalog(SystemConfigurationType system) {
        AdminGuiConfigurationType adminGuiConfiguration = system.getAdminGuiConfiguration();
        if (adminGuiConfiguration == null) {
            adminGuiConfiguration = new AdminGuiConfigurationType();
            system.setAdminGuiConfiguration(adminGuiConfiguration);
        }

        AccessRequestType accessRequest = adminGuiConfiguration.getAccessRequest();
        if (accessRequest == null) {
            accessRequest = new AccessRequestType();
            adminGuiConfiguration.setAccessRequest(accessRequest);
        }

        RoleCatalogType roleCatalog = accessRequest.getRoleCatalog();
        if (roleCatalog == null) {
            roleCatalog = new RoleCatalogType();
            accessRequest.setRoleCatalog(roleCatalog);
        }

        return roleCatalog;
    }
}
