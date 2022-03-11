/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.items;

import com.evolveum.midpoint.model.api.correlator.CorrelatorContext;
import com.evolveum.midpoint.schema.route.ItemRoute;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelationItemDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelationItemSourceDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemCorrelationType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemsCorrelatorType;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.jetbrains.annotations.NotNull;

/**
 * Resolves routes in correlation item definitions, trying to find referenced definitions
 * in "items" and "places".
 *
 * Very experimental.
 */
@Experimental
public class CorrelationItemRouteFinder {

    /**
     * Resolves a relative route (relative to the place for source items) to given source item.
     *
     * The route is taken either from the local item definition bean, or from referenced named item definition.
     */
    static ItemRoute findForSource(
            @NotNull CorrelationItemDefinitionType itemBean,
            @NotNull CorrelatorContext<?> correlatorContext) throws ConfigurationException {
        String ref = itemBean instanceof ItemCorrelationType ? ((ItemCorrelationType) itemBean).getRef() : null;
        if (ref != null) {
            CorrelationItemSourceDefinitionType sharedDefinition = correlatorContext.getNamedItemSourceDefinition(ref);
            return ItemRoute.fromBeans(
                    sharedDefinition.getPath(),
                    sharedDefinition.getRoute());
        }
        CorrelationItemSourceDefinitionType sourceBean = itemBean.getSource();
        if (sourceBean != null) {
            return ItemRoute.fromBeans(
                    sourceBean.getPath(),
                    sourceBean.getRoute());
        }

        ItemPathType pathBean = itemBean instanceof ItemCorrelationType ? ((ItemCorrelationType) itemBean).getPath() : null;
        if (pathBean != null) {
            return ItemRoute.fromPath(pathBean.getItemPath());
        }
        throw new ConfigurationException("Neither ref, nor path, nor source present in " + itemBean);
    }
}
