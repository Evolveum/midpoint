/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.model.api.correlation;

import com.evolveum.midpoint.schema.util.CorrelatorsDefinitionUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public interface CorrelationDefinitionProvider {

    CorrelationDefinitionType get() throws SchemaException, ObjectNotFoundException, ConfigurationException;

    default CorrelationDefinitionProvider union(CorrelationDefinitionProvider provider) {
        return () -> {
            final CorrelationDefinitionType targetCorrelationDef = this.get();
            final CorrelationDefinitionType sourceCorrelationDef = provider.get();

            return CorrelatorsDefinitionUtil.mergeCorrelationDefinitions(targetCorrelationDef, sourceCorrelationDef);
        };
    }

}
