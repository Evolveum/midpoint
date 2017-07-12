/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.model.impl.validator;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaHandlingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author mederly
 */
class DuplicateObjectTypeDetector {

	@NotNull final private Set<ObjectTypeRecord> records = new HashSet<>();
	@NotNull final private Set<ObjectTypeRecord> duplicates = new HashSet<>();

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
