/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.equivalence.ParameterizedEquivalenceStrategy;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Common supertype for all identity objects. Defines basic properties that each
 * object must have to live in our system (identifier, name).
 * <p>
 * Objects consists of identifier and name (see definition below) and a set of
 * properties represented as XML elements in the object's body. The attributes
 * are represented as first-level XML elements (tags) of the object XML
 * representation and may be also contained in other tags (e.g. extension,
 * attributes). The QName (namespace and local name) of the element holding the
 * property is considered to be a property name.
 * <p>
 * This class is named PrismObject instead of Object to avoid confusion with
 * java.lang.Object.
 *
 * @author Radovan Semancik
 * <p>
 * Class invariant: has at most one value (potentially empty).
 * When making this object immutable and there's no value, we create one; in order
 * to prevent exceptions on later getValue calls.
 */
public interface PrismObject<O extends Objectable> extends PrismContainer<O> {

    PrismObjectValue<O> createNewValue();

    @NotNull
    PrismObjectValue<O> getValue();

    @Override
    void setValue(@NotNull PrismContainerValue<O> value) throws SchemaException;

    /**
     * Returns Object ID (OID).
     * <p>
     * May return null if the object does not have an OID.
     *
     * @return Object ID (OID)
     */
    String getOid();

    void setOid(String oid);

    String getVersion();

    void setVersion(String version);

    @Override
    PrismObjectDefinition<O> getDefinition();

    @NotNull
    O asObjectable();

    PolyString getName();

    PrismContainer<?> getExtension();

    PrismContainer<?> getOrCreateExtension() throws SchemaException;

    PrismContainerValue<?> getExtensionContainerValue();

    <I extends Item> I findExtensionItem(String elementLocalName);

    <I extends Item> I findExtensionItem(QName elementName);

    <I extends Item> void addExtensionItem(I item) throws SchemaException;

    PrismContainer<?> createExtension() throws SchemaException;

    @Override
    void applyDefinition(PrismContainerDefinition<O> definition) throws SchemaException;

    @Override
    <IV extends PrismValue, ID extends ItemDefinition, I extends Item<IV, ID>> void removeItem(ItemPath path, Class<I> itemType);

    void addReplaceExisting(Item<?, ?> item) throws SchemaException;

    @Override
    PrismObject<O> clone();

    @Override
    PrismObject<O> cloneComplex(CloneStrategy strategy);

    PrismObjectDefinition<O> deepCloneDefinition(boolean ultraDeep, Consumer<ItemDefinition> postCloneAction);

    @NotNull
    ObjectDelta<O> diff(PrismObject<O> other);

    @NotNull
    ObjectDelta<O> diff(PrismObject<O> other, ParameterizedEquivalenceStrategy strategy);

    Collection<? extends ItemDelta<?, ?>> narrowModifications(Collection<? extends ItemDelta<?, ?>> modifications,
            boolean assumeMissingItems);

    ObjectDelta<O> createDelta(ChangeType changeType);

    ObjectDelta<O> createAddDelta();

    ObjectDelta<O> createModifyDelta();

    ObjectDelta<O> createDeleteDelta();

    @Override
    void setParent(PrismContainerValue<?> parentValue);

    @Override
    PrismContainerValue<?> getParent();

    /**
     * this method ignores some part of the object during comparison (e.g. source demarcation in values)
     * These methods compare the "meaningful" parts of the objects.
     */
    boolean equivalent(Object obj);

    /**
     * Returns short string representing identity of this object.
     * It should container object type, OID and name. It should be presented
     * in a form suitable for log and diagnostic messages (understandable for
     * system administrator).
     */
    String toDebugName();

    /**
     * Returns short string identification of object type. It should be in a form
     * suitable for log messages. There is no requirement for the type name to be unique,
     * but it rather has to be compact. E.g. short element names are preferred to long
     * QNames or URIs.
     */
    String toDebugType();

    /**
     * Return display name intended for business users of midPoint
     */
    String getBusinessDisplayName();

    PrismObject<O> cloneIfImmutable();

    PrismObject<O> createImmutableClone();

    @NotNull
    static <T extends Objectable> List<T> asObjectableList(@NotNull List<PrismObject<T>> objects) {
        return objects.stream()
                .map(o -> o.asObjectable())
                .collect(Collectors.toList());
    }

    static PrismObject<?> asPrismObject(Objectable o) {
        return o != null ? o.asPrismObject() : null;
    }

    static <T extends Objectable> T asObjectable(PrismObject<T> object) {
        return object != null ? object.asObjectable() : null;
    }

    default void fixMockUpValueMetadata() {
    }
}
