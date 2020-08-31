/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.mapping.metadata.builtin;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.collections4.ListUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.common.mapping.metadata.ConsolidationMetadataComputation;
import com.evolveum.midpoint.model.common.mapping.metadata.TransformationalMetadataComputation;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StorageMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueMetadataType;

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
    public void applyForTransformation(@NotNull TransformationalMetadataComputation computation) throws SchemaException {
        addPropertyRealValue(computation.getOutputMetadataValue(), computation.getEnv().now);
    }

    @Override
    public void applyForConsolidation(@NotNull ConsolidationMetadataComputation computation) throws SchemaException {
        List<ValueMetadataType> allValues = ListUtils.union(computation.getNonNegativeValues(), computation.getExistingValues());
        XMLGregorianCalendar earliest = earliestTimestamp(allValues, CREATE_PATH);
        addPropertyRealValue(computation.getOutputMetadataValue(), earliest);
    }

    private XMLGregorianCalendar earliestTimestamp(List<ValueMetadataType> values, ItemPath path) {
        Set<XMLGregorianCalendar> realValues = getRealValues(values, path);
        return MiscUtil.getEarliestTimeIgnoringNull(realValues);
    }

    private <T> Set<T> getRealValues(List<ValueMetadataType> values, ItemPath path) {
        //noinspection unchecked
        return (Set<T>) values.stream()
                .map(v -> v != null ? v.asPrismContainerValue().findItem(path) : null)
                .filter(Objects::nonNull)
                .map(Item::getRealValue)
                .collect(Collectors.toSet());
    }

}
