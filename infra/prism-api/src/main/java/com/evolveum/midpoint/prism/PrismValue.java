/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.equivalence.ParameterizedEquivalenceStrategy;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

/**
 * @author semancik
 *
 */
public interface PrismValue extends Visitable, PathVisitable, Serializable, DebugDumpable, Revivable {      // todo ShortDumpable?

    void setPrismContext(PrismContext prismContext);

    void setOriginObject(Objectable source);

    void setOriginType(OriginType type);

    OriginType getOriginType();

    Objectable getOriginObject();

    Map<String, Object> getUserData();

    Object getUserData(@NotNull String key);

    void setUserData(@NotNull String key, Object value);

    Itemable getParent();

    void setParent(Itemable parent);

    @NotNull
    ItemPath getPath();

    /**
     * Used when we are removing the value from the previous parent.
     * Or when we know that the previous parent will be discarded and we
     * want to avoid unnecessary cloning.
     */
    void clearParent();

    PrismContext getPrismContext();

    void applyDefinition(ItemDefinition definition) throws SchemaException;

    void applyDefinition(ItemDefinition definition, boolean force) throws SchemaException;

    void revive(PrismContext prismContext) throws SchemaException;

    /**
     * Recompute the value or otherwise "initialize" it before adding it to a prism tree.
     * This may as well do nothing if no recomputing or initialization is needed.
     */
    void recompute();

    void recompute(PrismContext prismContext);

    @Override
    void accept(Visitor visitor);

    @Override
    void accept(Visitor visitor, ItemPath path, boolean recursive);

    void checkConsistenceInternal(Itemable rootItem, boolean requireDefinitions, boolean prohibitRaw, ConsistencyCheckScope scope);

    /**
     * Returns true if this and other value represent the same value.
     * E.g. if they have the same IDs, OIDs or it is otherwise know
     * that they "belong together" without a deep examination of the
     * values.
     *
     * @param lax If we can reasonably assume that the two values belong together even if they don't have the same ID,
     *            e.g. if they both belong to single-valued parent items. This is useful e.g. when comparing
     *            multi-valued containers. But can cause problems when we want to be sure we are removing the correct
     *            value.
     */
    boolean representsSameValue(PrismValue other, boolean lax);

    void normalize();

    /**
     * Literal clone.
     */
    PrismValue clone();

    /**
     * Complex clone with different cloning strategies.
     * @see CloneStrategy
     */
    PrismValue cloneComplex(CloneStrategy strategy);

    int hashCode(@NotNull EquivalenceStrategy equivalenceStrategy);

    int hashCode(@NotNull ParameterizedEquivalenceStrategy equivalenceStrategy);

    boolean equals(PrismValue otherValue, @NotNull EquivalenceStrategy strategy);

    boolean equals(PrismValue otherValue, @NotNull ParameterizedEquivalenceStrategy strategy);

    boolean equals(PrismValue thisValue, PrismValue otherValue);

    /**
     * Assumes matching representations. I.e. it assumes that both this and otherValue represent the same instance of item.
     * E.g. the container with the same ID.
     */
    Collection<? extends ItemDelta> diff(PrismValue otherValue);

    /**
     * Assumes matching representations. I.e. it assumes that both this and otherValue represent the same instance of item.
     * E.g. the container with the same ID.
     */
    Collection<? extends ItemDelta> diff(PrismValue otherValue, ParameterizedEquivalenceStrategy strategy);

    boolean isImmutable();

    void setImmutable(boolean immutable);

    @Nullable
    Class<?> getRealClass();

    @Experimental // todo reconsider method name
    default boolean hasRealClass() {
        return getRealClass() != null;
    }

    @Nullable
    <T> T getRealValue();

    @Nullable
    @Experimental // todo reconsider method name
    default Object getRealValueOrRawType(PrismContext prismContext) {
        if (hasRealClass()) {
            return getRealValue();
        } else {
            return new RawType(this, getTypeName(), prismContext);
        }
    }

    // Returns a root of PrismValue tree. For example, if we have a AccessCertificationWorkItemType that has a parent (owner)
    // of AccessCertificationCaseType, which has a parent of AccessCertificationCampaignType, this method returns the PCV
    // of AccessCertificationCampaignType.
    //
    // Generally, this method returns either "this" (PrismValue) or a PrismContainerValue.
    PrismValue getRootValue();

    PrismContainerValue<?> getParentContainerValue();

    QName getTypeName();

    // Path may contain ambiguous segments (e.g. assignment/targetRef when there are more assignments)
    // Note that the path can contain name segments only (at least for now)
    @NotNull
    Collection<PrismValue> getAllValues(ItemPath path);

    boolean isRaw();

    boolean isEmpty();

    String toHumanReadableString();

//    // todo hide from public
//    void diffMatchingRepresentation(PrismValue otherValue,
//            Collection<? extends ItemDelta> deltas, boolean ignoreMetadata, boolean isLiteral);

    Object find(ItemPath path);
}
