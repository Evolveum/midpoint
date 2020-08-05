/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.mapping.metadata.builtin;

import javax.annotation.PostConstruct;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.prism.*;

import com.evolveum.midpoint.util.MiscUtil;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * TODO
 */
abstract class BaseBuiltinMetadataMapping implements BuiltinMetadataMapping {

    @NotNull private final ItemPath targetPath;

    @Autowired PrismContext prismContext;
    @Autowired private BuiltinMetadataMappingsRegistry registry;

    BaseBuiltinMetadataMapping(@NotNull ItemPath targetItem) {
        this.targetPath = targetItem;
    }

    @PostConstruct
    void register() {
        registry.registerBuiltinMapping(this);
    }

    @Override
    @NotNull
    public ItemPath getTargetPath() {
        return targetPath;
    }

    void addPropertyRealValue(PrismContainerValue<?> outputMetadata, Object value) throws SchemaException {
        if (value != null) {
            PrismProperty property = outputMetadata.findOrCreateProperty(targetPath);
            //noinspection unchecked
            property.addRealValue(value);
        }
    }

    XMLGregorianCalendar earliestTimestamp(List<PrismValue> values, ItemPath path) {
        Set<XMLGregorianCalendar> realValues = getRealValues(values, path);
        return MiscUtil.getEarliestTimeIgnoringNull(realValues);
    }

    private <T> Set<T> getRealValues(List<PrismValue> values, ItemPath path) {
        //noinspection unchecked
        return (Set<T>) values.stream()
                .flatMap(prismValue -> prismValue.getValueMetadata().getValues().stream()) // TEMPORARY!!!
                .map(v -> v.findItem(path))
                .filter(Objects::nonNull)
                .map(Item::getRealValue)
                .collect(Collectors.toSet());
    }
}
