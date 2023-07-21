/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.validator.processor;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;

public interface ProcessorMixin {

    default String getIdentifier(Class<?> processor) {
        return processor.getSimpleName().replaceFirst("Processor$", "");
    }

    default RoleCatalogType getRoleCatalog(SystemConfigurationType system) {
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

    /**
     * Matches object type and path template (without container ids in case of multivalue containers).
     *
     * @param object tested object
     * @param path validation item path
     * @param type expected type (ObjectType)
     * @param expected exptected path template
     * @param <O>
     * @return true if matches
     */
    default <O extends ObjectType> boolean matchObjectTypeAndPathTemplate(
            @NotNull PrismObject<?> object, @NotNull ItemPath path, @NotNull Class<O> type, @NotNull ItemPath expected) {

        if (!type.isAssignableFrom(object.getCompileTimeClass())) {
            return false;
        }

        if (!path.namedSegmentsOnly().equivalent(expected)) {
            return false;
        }

        Item item = object.findItem(path);
        if (item == null || item.isEmpty()) {
            return false;
        }

        return true;
    }
}
