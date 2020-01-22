/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
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
 * Assignment object relation specification. Data structure that contains information about possible
 * assignment targets or holders for a particular object and possible relation/archetype combinations.
 *
 * This data structure is used in two related, but slight distinct cases: looking for assignment targets
 * and looking for assignment holders. In both cases this structure describes candidate objects on the
 * "other side" of the assignment.
 *
 * For the "target" case, simply speaking, those are object types that can be targets of assignments for this object
 * and the respective relations. This means "what assignments can I have"
 * or "what are the valid targets for relations that I hold". It is the reverse of assignmentRelation definition in AssignmentType in schema.
 *
 * For the "holder" case it is a reverse of the above. In that case it specifies objects that ca be
 * potential members.
 *
 * If objectType, archetype or relation is null then there is no constraint for that
 * specific aspect. E.g. if the archetypeRefs is null then any archetype is allowed.
 *
 * If objectType, archetype or relation is empty list then no value for that particular
 * aspect is allowed. Which means that this specification does not really allow anything.
 * This should not really happen when used in ArchetypeInteractionSpecification as such
 * specification would be meaningless.
 *
 * If more that one targetType, archetype or relation is specified then all possible
 * combinations of those values are allowed (carthesian product).
 *
 * @author Radovan Semancik
 *
 */
public class AssignmentObjectRelation implements DebugDumpable, ShortDumpable, Serializable {
    private static final long serialVersionUID = 1L;

    public List<QName> objectTypes;
    public List<ObjectReferenceType> archetypeRefs;
    public List<QName> relations;
    public String description;

    public List<QName> getObjectTypes() {
        return objectTypes;
    }

    public void setObjectTypes(List<QName> targetTypes) {
        this.objectTypes = targetTypes;
    }

    public void addObjectTypes(List<QName> newTargetTypes) {
        if (newTargetTypes == null) {
            return;
        }
        if (this.objectTypes == null) {
            this.objectTypes = new ArrayList<>();
        }
        this.objectTypes.addAll(newTargetTypes);
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

    public void addArchetypeRef(ObjectReferenceType archetypeRef) {
        if (archetypeRefs == null) {
            archetypeRefs = new ArrayList<>();
        }
        archetypeRefs.add(archetypeRef);
    }

    public void addArchetypeRefs(Collection<ObjectReferenceType> archetypeRefs) {
        if (this.archetypeRefs == null) {
            this.archetypeRefs = new ArrayList<>();
        }
        this.archetypeRefs.addAll(archetypeRefs);
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
        if (objectTypes == null) {
            sb.append("*");
        } else {
            DebugUtil.shortDumpCollectionPrettyPrintOptionalBrackets(sb, objectTypes);
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
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(AssignmentObjectRelation.class, indent);
        DebugUtil.debugDumpWithLabelLn(sb, "description", description, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "objectTypes", objectTypes, indent + 1);
        SchemaDebugUtil.debugDumpWithLabelLn(sb, "archetypeRefs", archetypeRefs, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "relations", relations, indent + 1);
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("AssignmentObjectRelation()");
        shortDump(sb);
        sb.append(")");
        return sb.toString();
    }




}
