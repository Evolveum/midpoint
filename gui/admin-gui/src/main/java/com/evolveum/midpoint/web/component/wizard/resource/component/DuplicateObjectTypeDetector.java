package com.evolveum.midpoint.web.component.wizard.resource.component;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSynchronizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * @author mederly
 */
public class DuplicateObjectTypeDetector {

	@NotNull final private Set<ObjectTypeRecord> records = new HashSet<>();
	@NotNull final private Set<ObjectTypeRecord> duplicates = new HashSet<>();

	public boolean hasDuplicates() {
		return !duplicates.isEmpty();
	}

	public String getDuplicatesList() {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (ObjectTypeRecord duplicate : duplicates) {
			if (first) {
				first = false;
			} else {
				sb.append(", ");
			}
			sb.append(duplicate.kind).append("/").append(duplicate.intent);
		}
		return sb.toString();
	}

	private static class ObjectTypeRecord {

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
	}

	public void add(ResourceObjectTypeDefinitionType objectType) {
		add(new ObjectTypeRecord(objectType));
	}

	public void add(ObjectSynchronizationType synchronization) {
		add(new ObjectTypeRecord(synchronization));
	}

	private void add(ObjectTypeRecord objectTypeRecord) {
		if (!records.add(objectTypeRecord)) {
			duplicates.add(objectTypeRecord);
		}
	}

}
