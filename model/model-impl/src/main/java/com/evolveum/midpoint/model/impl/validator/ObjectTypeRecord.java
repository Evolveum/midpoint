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

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author mederly
 */
class ObjectTypeRecord {

	@NotNull public final ShadowKindType kind;
	@NotNull public final String intent;

	ObjectTypeRecord(ShadowKindType kind, String intent) {
		this.kind = kind != null ? kind : ShadowKindType.ACCOUNT;
		this.intent = intent != null ? intent : SchemaConstants.INTENT_DEFAULT;
	}

	ObjectTypeRecord(ResourceObjectTypeDefinitionType definition) {
		this(definition.getKind(), definition.getIntent());
	}

	ObjectTypeRecord(ObjectSynchronizationType synchronization) {
		this(synchronization.getKind(), synchronization.getIntent());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		ObjectTypeRecord that = (ObjectTypeRecord) o;

		return kind == that.kind && intent.equals(that.intent);
	}

	@Override
	public int hashCode() {
		int result = kind.hashCode();
		result = 31 * result + intent.hashCode();
		return result;
	}

	@NotNull
	static List<ObjectTypeRecord> extractFrom(SchemaHandlingType schemaHandling) {
		List<ObjectTypeRecord> rv = new ArrayList<>();
		if (schemaHandling != null) {
			for (ResourceObjectTypeDefinitionType objectType : schemaHandling.getObjectType()) {
				rv.add(new ObjectTypeRecord(objectType));
			}
		}
		return rv;
	}

	@NotNull
	static List<ObjectTypeRecord> extractFrom(SynchronizationType synchronization) {
		List<ObjectTypeRecord> rv = new ArrayList<>();
		if (synchronization != null) {
			for (ObjectSynchronizationType objectSynchronization : synchronization.getObjectSynchronization()) {
				rv.add(new ObjectTypeRecord(objectSynchronization));
			}
		}
		return rv;
	}

	@NotNull
	static String asFormattedList(Collection<ObjectTypeRecord> records) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (ObjectTypeRecord record : records) {
			if (first) {
				first = false;
			} else {
				sb.append(", ");
			}
			sb.append(record.kind).append("/").append(record.intent);
		}
		return sb.toString();
	}
}
