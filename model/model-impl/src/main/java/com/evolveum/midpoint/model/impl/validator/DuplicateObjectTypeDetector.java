/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.validator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaHandlingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationType;

/**
 * @author mederly
 */
class DuplicateObjectTypeDetector {

    @NotNull private final Set<ObjectTypeRecord> records = new HashSet<>();
    @NotNull private final Set<ObjectTypeRecord> duplicates = new HashSet<>();

    DuplicateObjectTypeDetector(@Nullable SchemaHandlingType schemaHandling) {
        addCheckingDuplicates(ObjectTypeRecord.extractFrom(schemaHandling));
    }

    DuplicateObjectTypeDetector(SynchronizationType objectSynchronization) {
        addCheckingDuplicates(ObjectTypeRecord.extractFrom(objectSynchronization));
    }

    private void add(ObjectTypeRecord objectTypeRecord) {
        if (!records.add(objectTypeRecord)) {
            duplicates.add(objectTypeRecord);
        }
    }

    private void addCheckingDuplicates(List<ObjectTypeRecord> objectTypeRecords) {
        for (ObjectTypeRecord record : objectTypeRecords) {
            add(record);
        }
    }

    boolean hasDuplicates() {
        return !duplicates.isEmpty();
    }

    String getDuplicatesList() {
        return ObjectTypeRecord.asFormattedList(duplicates);
    }
}
