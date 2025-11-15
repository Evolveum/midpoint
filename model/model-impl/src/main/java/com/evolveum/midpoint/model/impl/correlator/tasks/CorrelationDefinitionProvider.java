/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.model.impl.correlator.tasks;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public interface CorrelationDefinitionProvider {

    CorrelationDefinitionType get(OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException;

    record ResourceWithObjectTypeId(String oid, ShadowKindType kind, String intent) {
        public static ResourceWithObjectTypeId from(ResourceObjectSetType resourceObjectSet) {
            return new ResourceWithObjectTypeId(resourceObjectSet.getResourceRef().getOid(),
                    resourceObjectSet.getKind(), resourceObjectSet.getIntent());
        }
    }

}
