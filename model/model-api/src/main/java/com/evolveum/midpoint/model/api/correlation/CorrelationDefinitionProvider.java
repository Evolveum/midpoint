/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.model.api.correlation;

import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.util.CorrelatorsDefinitionUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelationDefinitionType;

/**
 * Provider of correlation definitions.
 *
 * Implementations may use various sources of the definitions.
 */
public interface CorrelationDefinitionProvider {

    /**
     * Reads the correlation definition of particular object type.
     *
     * The source from which the definition is read is up to the implementation.
     *
     * @param objectTypeId The identification of the object type, correlation definition of which you want to get.
     * @return The correlation definition of given object type.
     */
    CorrelationDefinitionType definitionFor(ResourceObjectTypeIdentification objectTypeId)
            throws SchemaException, ObjectNotFoundException, ConfigurationException;

    /**
     * Returns a combined provider that merges the correlation definitions from this provider and the specified provider.
     *
     * For merging details see the {@link
     * CorrelatorsDefinitionUtil#mergeCorrelationDefinitions(CorrelationDefinitionType, CorrelationDefinitionType)}
     *
     * @param provider The provider to merge with.
     * @return The combined correlation definition provider
     */
    default CorrelationDefinitionProvider union(CorrelationDefinitionProvider provider) {
        return objectTypeId -> {
            final CorrelationDefinitionType targetCorrelationDef = this.definitionFor(objectTypeId);
            final CorrelationDefinitionType sourceCorrelationDef = provider.definitionFor(objectTypeId);

            return CorrelatorsDefinitionUtil.mergeCorrelationDefinitions(targetCorrelationDef, sourceCorrelationDef);
        };
    }

}
