/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.mapping.metadata.builtin;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StorageMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueMetadataType;

import org.springframework.stereotype.Component;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.List;

/**
 * Mapping that provides storage/createTimestamp.
 */
@Component
public class CreateTimestampBuiltinMapping extends BaseBuiltinMetadataMapping {

    CreateTimestampBuiltinMapping() {
        super(ItemPath.create(ValueMetadataType.F_STORAGE, StorageMetadataType.F_CREATE_TIMESTAMP));
    }

    @Override
    public void apply(List<PrismValue> valuesTuple, PrismContainerValue<ValueMetadataType> outputMetadata,
            String contextDescription, XMLGregorianCalendar now, Task task, OperationResult result) throws SchemaException {
        addPropertyRealValue(outputMetadata, now);
    }
}
