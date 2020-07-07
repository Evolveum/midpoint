/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.mapping.metadata.builtin;

import com.evolveum.midpoint.model.common.mapping.MappingEvaluationEnvironment;
import com.evolveum.midpoint.model.common.mapping.metadata.ValueMetadataComputation;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataMappingScopeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StorageMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueMetadataType;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.List;

/**
 * Mapping that provides storage/createTimestamp.
 */
@Component
public class CreateTimestampBuiltinMapping extends BaseBuiltinMetadataMapping {

    private static final ItemPath CREATE_PATH = ItemPath.create(ValueMetadataType.F_STORAGE, StorageMetadataType.F_CREATE_TIMESTAMP);

    CreateTimestampBuiltinMapping() {
        super(CREATE_PATH);
    }

    @Override
    public void apply(@NotNull ValueMetadataComputation computation) throws SchemaException {

        XMLGregorianCalendar rv;
        MappingEvaluationEnvironment env = computation.getEnv();
        MetadataMappingScopeType scope = computation.getScope();
        List<PrismValue> input;
        switch (scope) {
            case TRANSFORMATION:
                input = null;
                rv = env.now;
                break;
            case CONSOLIDATION:
                input = computation.getInputValues();
                rv = earliestTimestamp(input, CREATE_PATH);
                break;
            default:
                throw new AssertionError(scope);
        }
//        System.out.println("Computed creation timestamp for " + scope + " in " + env.contextDescription + " from " + input + ": " + rv);
        addPropertyRealValue(computation.getOutputMetadata(), rv);
    }
}
