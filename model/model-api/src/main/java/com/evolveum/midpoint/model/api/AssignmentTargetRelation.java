/**
 * Copyright (c) 2019 Evolveum
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
package com.evolveum.midpoint.model.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * Assignment target relation specification. Simply speaking, those are object types that
 * can be targets of assignments for this object
 * and the respective relations. Simply speaking this means "what assignments can I have"
 * or "what are the valid targets for relations that I hold".
 * It is the reverse of assignmentRelation definition in AssignmentType in schema.
 * 
 * If targetType, archetype or relation is null then there is no constraint for that
 * specific aspect. E.g. if the archetypeRefs is null then any archetype is allowed.
 * 
 * if targetType, archetype or relation is empty list then no value for that particular
 * aspect is allowed. Which means that this specification does not really allow anything.
 * This should not really happen when used in ArchetypeInteractionSpecification as such
 * specification would be meaningless.
 * 
 * If more that one targetType, archetype or relation is specified then all possible
 * combinations of those values are allowed (carthesian product).
 * 
 * @author semancik
 *
 */
public class AssignmentTargetRelation implements DebugDumpable, ShortDumpable, Serializable {
	
	public List<QName> targetTypes;
	public List<ObjectReferenceType> archetypeRefs;
	public List<QName> relations;
	public String description;
	
	public List<QName> getTargetTypes() {
		return targetTypes;
	}
	
	public void setTargetTypes(List<QName> targetTypes) {
		this.targetTypes = targetTypes;
	}
	
	public void addTargetTypes(List<QName> newTargetTypes) {
		if (newTargetTypes == null) {
			return;
		}
		if (this.targetTypes == null) {
			this.targetTypes = new ArrayList<>();
		}
		this.targetTypes.addAll(newTargetTypes);
	}
	
	public List<ObjectReferenceType> getArchetypeRefs() {
		return archetypeRefs;
	}

	public void setArchetypeRefs(List<ObjectReferenceType> archetypeRefs) {
		this.archetypeRefs = archetypeRefs;
	}
	
	public void addArchetypeRef(PrismObject<ArchetypeType> archetype) {
		if (archetypeRefs == null) {
			archetypeRefs = new ArrayList<>();
		}
		ObjectReferenceType ref = MiscSchemaUtil.createObjectReference(archetype, ArchetypeType.class);
		archetypeRefs.add(ref);
	}

	public List<QName> getRelations() {
		return relations;
	}
	
	public void setRelations(List<QName> relations) {
		this.relations = relations;
	}
	
	public void addRelations(List<QName> newRelations) {
		if (newRelations == null) {
			return;
		}
		if (this.relations == null) {
			this.relations = new ArrayList<>();
		}
		this.relations.addAll(newRelations);
	}

	/**
	 * Just for diagnostic purposes (testability).
	 */
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public void shortDump(StringBuilder sb) {
		if (targetTypes == null) {
			sb.append("*");
		} else {
			DebugUtil.shortDumpCollectionPrettyPrintOptionalBrackets(sb, targetTypes);
		}
		if (archetypeRefs != null) {
			sb.append("/");
			SchemaDebugUtil.shortDumpReferenceCollectionOptionalBrackets(sb, archetypeRefs);
		}
		sb.append(":");
		if (relations == null) {
			sb.append("*");
		} else {
			DebugUtil.shortDumpCollectionPrettyPrintOptionalBrackets(sb, relations);
		}
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = DebugUtil.createTitleStringBuilderLn(AssignmentTargetRelation.class, indent);
		DebugUtil.debugDumpWithLabelLn(sb, "description", description, indent + 1);
		DebugUtil.debugDumpWithLabelLn(sb, "targetTypes", targetTypes, indent + 1);
		SchemaDebugUtil.debugDumpWithLabelLn(sb, "archetypeRefs", archetypeRefs, indent + 1);
		DebugUtil.debugDumpWithLabel(sb, "relations", relations, indent + 1);
		return sb.toString();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("AssignmentTargetRelation()");
		shortDump(sb);
		sb.append(")");
		return sb.toString();
	}
	
	
	
	
}
