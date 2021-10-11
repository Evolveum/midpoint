/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.EvaluationTimeType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ReferentialIntegrityType;

import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

/**
 * @author Radovan Semancik
 */
public interface PrismReferenceValue extends PrismValue, ShortDumpable {
    /**
     * OID of the object that this reference refers to (reference target).
     *
     * May return null, but the reference is in that case incomplete and
     * unusable.
     *
     * @return the target oid
     */
    String getOid();

    void setOid(String oid);

    /**
     * Returns object that this reference points to. The object is supposed to be used
     * for caching and optimizations. Only oid and type of the object really matters for
     * the reference.
     *
     * The object is transient. It will NOT be serialized. Therefore the client must
     * expect that the object can disappear when serialization boundary is crossed.
     * The client must expect that the object is null.
     */
    PrismObject getObject();

    void setObject(PrismObject object);

    /**
     * Returns XSD type of the object that this reference refers to. It may be
     * used in XPath expressions and similar filters.
     *
     * May return null if the type name is not set.
     *
     * @return the target type name
     */
    QName getTargetType();

    void setTargetType(QName targetType);

    /**
     * @param targetType
     * @param allowEmptyNamespace This is an ugly hack. See comment in DOMUtil.validateNonEmptyQName.
     */
    void setTargetType(QName targetType, boolean allowEmptyNamespace);

    /**
     * Returns cached name of the target object.
     * This is a ephemeral value. It is usually not stored.
     * It may be computed at object retrieval time or it may not be present at all.
     * This is NOT an authoritative information. Setting it or changing it will
     * not influence the reference meaning. OID is the only authoritative linking
     * mechanism.
     * @return cached name of the target object.
     */
    PolyString getTargetName();

    void setTargetName(PolyString name);

    void setTargetName(PolyStringType name);

    // The PRV (this object) should have a parent with a prism context
    Class<Objectable> getTargetTypeCompileTimeClass();

    Class<Objectable> getTargetTypeCompileTimeClass(PrismContext prismContext);

    QName getRelation();

    void setRelation(QName relation);

    PrismReferenceValue relation(QName relation);

    String getDescription();

    void setDescription(String description);

    SearchFilterType getFilter();

    void setFilter(SearchFilterType filter);

    EvaluationTimeType getResolutionTime();

    void setResolutionTime(EvaluationTimeType resolutionTime);

    ReferentialIntegrityType getReferentialIntegrity();

    void setReferentialIntegrity(ReferentialIntegrityType referentialIntegrity);

    PrismReferenceDefinition getDefinition();

    <IV extends PrismValue,ID extends ItemDefinition> PartiallyResolvedItem<IV,ID> findPartial(ItemPath path);

    @Override
    void applyDefinition(ItemDefinition definition, boolean force) throws SchemaException;

    void applyDefinition(PrismReferenceDefinition definition, boolean force) throws SchemaException;

    /**
     * Returns a version of this value that is canonical, that means it has the minimal form.
     * E.g. it will have only OID and no object.
     */
    PrismReferenceValue toCanonical();

    Referencable asReferencable();

    String debugDump(int indent, boolean expandObject);

    @Override
    PrismReferenceValue clone();

    @Override
    PrismReferenceValue createImmutableClone();

    @Override
    PrismReferenceValue cloneComplex(CloneStrategy strategy);

    @Override
    Class<?> getRealClass();

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    Referencable getRealValue();
}
