/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.equivalence.ParameterizedEquivalenceStrategy;
import com.evolveum.midpoint.prism.impl.delta.ObjectDeltaImpl;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Common supertype for all identity objects. Defines basic properties that each
 * object must have to live in our system (identifier, name).
 *
 * Objects consists of identifier and name (see definition below) and a set of
 * properties represented as XML elements in the object's body. The attributes
 * are represented as first-level XML elements (tags) of the object XML
 * representation and may be also contained in other tags (e.g. extension,
 * attributes). The QName (namespace and local name) of the element holding the
 * property is considered to be a property name.
 *
 * This class is named PrismObject instead of Object to avoid confusion with
 * java.lang.Object.
 *
 * @author Radovan Semancik
 *
 * Class invariant: has at most one value (potentially empty).
 * When making this object immutable and there's no value, we create one; in order
 * to prevent exceptions on later getValue calls.
 */
public class PrismObjectImpl<O extends Objectable> extends PrismContainerImpl<O> implements PrismObject<O> {

    private static final long serialVersionUID = 7321429132391159949L;

    public PrismObjectImpl(QName name, Class<O> compileTimeClass) {
        super(name, compileTimeClass);
    }

    public PrismObjectImpl(QName name, Class<O> compileTimeClass, PrismContext prismContext) {
        super(name, compileTimeClass, prismContext);
    }

    public PrismObjectImpl(QName name, PrismObjectDefinition<O> definition, PrismContext prismContext) {
        super(name, definition, prismContext);
    }

    public PrismObjectValue<O> createNewValue() {
        checkMutable();
        PrismObjectValue<O> newValue = new PrismObjectValueImpl<>(prismContext);
        try {
            add(newValue, false);
            return newValue;
        } catch (SchemaException e) {
            // This should not happen
            throw new SystemException("Internal Error: " + e.getMessage(), e);
        }
    }

    @NotNull
    public PrismObjectValue<O> getValue() {
        if (values.isEmpty()) {
            return createNewValue();
        } else if (values.size() > 1) {
            throw new IllegalStateException("PrismObject with more than one value: " + values);
        }
        return (PrismObjectValue<O>) values.get(0);
    }

    @Override
    public void setValue(@NotNull PrismContainerValue<O> value) throws SchemaException {
        clear();
        add(value, false);
    }

    @Override
    public boolean add(@NotNull PrismContainerValue newValue, boolean checkUniqueness) throws SchemaException {
        if (!(newValue instanceof PrismObjectValue)) {
            throw new IllegalArgumentException("Couldn't add non-PrismObjectValue to a PrismObject: value = "
                    + newValue + ", object = " + this);
        }
        if (values.size() > 1) {
            throw new IllegalStateException("PrismObject with more than one value: " + this);
        } else if (values.size() == 1) {
            PrismObjectValue<O> value = (PrismObjectValue<O>) values.get(0);
            if (value.isEmpty() && value.getOid() == null) {
                clear();
            } else {
                throw new IllegalStateException("PrismObject cannot have more than one value. New value = " + newValue
                        + ", object = " + this);
            }
        }
        return super.add(newValue, checkUniqueness);
    }

    /**
     * Returns Object ID (OID).
     *
     * May return null if the object does not have an OID.
     *
     * @return Object ID (OID)
     */
    public String getOid() {
        return getValue().getOid();
    }

    public void setOid(String oid) {
        checkMutable();
        getValue().setOid(oid);
    }

    public String getVersion() {
        return getValue().getVersion();
    }

    public void setVersion(String version) {
        checkMutable();
        getValue().setVersion(version);
    }

    @Override
    public PrismObjectDefinition<O> getDefinition() {
        return (PrismObjectDefinition<O>) super.getDefinition();
    }

    @NotNull
    public O asObjectable() {
        return getValue().asObjectable();
    }

    public PolyString getName() {
        PrismProperty<PolyString> nameProperty = getValue().findProperty(getNamePropertyElementName());
        if (nameProperty == null) {
            return null;
        }
        return nameProperty.getRealValue();
    }

    private ItemName getNamePropertyElementName() {
        return new ItemName(getElementName().getNamespaceURI(), PrismConstants.NAME_LOCAL_NAME);
    }

    public PrismContainer<?> getExtension() {
        //noinspection unchecked
        return (PrismContainer<?>) getValue().findItem(getExtensionContainerElementName(), PrismContainer.class);
    }

    public PrismContainer<?> getOrCreateExtension() throws SchemaException {
        //noinspection unchecked
        return (PrismContainer<?>) getValue().findOrCreateItem(getExtensionContainerElementName(), PrismContainer.class);
    }

    public PrismContainerValue<?> getExtensionContainerValue() {
        PrismContainer<?> extension = getExtension();
        if (extension == null || extension.getValues().isEmpty()) {
            return null;
        } else {
            return extension.getValue();
        }
    }

    public <I extends Item> I findExtensionItem(String elementLocalName) {
        return findExtensionItem(new QName(null, elementLocalName));
    }

    public <I extends Item> I findExtensionItem(@NotNull QName elementName) {
        PrismContainer<?> extension = getExtension();
        if (extension == null) {
            return null;
        }
        //noinspection unchecked
        return (I) extension.findItem(ItemName.fromQName(elementName));
    }

    public <I extends Item> void addExtensionItem(I item) throws SchemaException {
        PrismContainer<?> extension = getExtension();
        if (extension == null) {
            extension = createExtension();
        }
        extension.add(item);
    }

    public PrismContainer<?> createExtension() throws SchemaException {
        PrismObjectDefinition<O> objeDef = getDefinition();
        PrismContainerDefinition<Containerable> extensionDef = objeDef.findContainerDefinition(getExtensionContainerElementName());
        PrismContainer<?> extensionContainer = extensionDef.instantiate();
        getValue().add(extensionContainer);
        return extensionContainer;
    }

    private ItemName getExtensionContainerElementName() {
        return new ItemName(getElementName().getNamespaceURI(), PrismConstants.EXTENSION_LOCAL_NAME);
    }

    @Override
    public void applyDefinition(PrismContainerDefinition<O> definition) throws SchemaException {
        if (!(definition instanceof PrismObjectDefinition)) {
            throw new IllegalArgumentException("Cannot apply "+definition+" to object");
        }
        super.applyDefinition(definition);
    }

    @Override
    public <IV extends PrismValue,ID extends ItemDefinition,I extends Item<IV,ID>> void removeItem(ItemPath path, Class<I> itemType) {
        // Objects are only a single-valued containers. The path of the object itself is "empty".
        // Fix this special behavior here.
        ((PrismObjectValueImpl<O>) getValue()).removeItem(path, itemType);
    }

    public void addReplaceExisting(Item<?,?> item) throws SchemaException {
        getValue().addReplaceExisting(item);
    }

    @Override
    public PrismObject<O> clone() {
        return cloneComplex(CloneStrategy.LITERAL);
    }

    @Override
    public PrismObject<O> createImmutableClone() {
        return (PrismObject<O>) super.createImmutableClone();
    }

    @Override
    public PrismObjectImpl<O> cloneComplex(CloneStrategy strategy) {
        if (prismContext != null && prismContext.getMonitor() != null) {
            prismContext.getMonitor().beforeObjectClone(this);
        }

        PrismObjectImpl<O> clone = new PrismObjectImpl<>(getElementName(), getDefinition(), prismContext);
        copyValues(strategy, clone);

        if (prismContext != null && prismContext.getMonitor() != null) {
            prismContext.getMonitor().afterObjectClone(this, clone);
        }
        return clone;
    }

    protected void copyValues(CloneStrategy strategy, PrismObjectImpl<O> clone) {
        super.copyValues(strategy, clone);
    }

    public PrismObjectDefinition<O> deepCloneDefinition(boolean ultraDeep, Consumer<ItemDefinition> postCloneAction) {
        return (PrismObjectDefinition<O>) super.deepCloneDefinition(ultraDeep, postCloneAction);
    }

    @NotNull
    public ObjectDelta<O> diff(PrismObject<O> other) {
        return diff(other, ParameterizedEquivalenceStrategy.DEFAULT_FOR_DIFF);
    }

    @NotNull
    public ObjectDelta<O> diff(PrismObject<O> other, ParameterizedEquivalenceStrategy strategy) {
        if (other == null) {
            ObjectDelta<O> objectDelta = new ObjectDeltaImpl<>(getCompileTimeClass(), ChangeType.DELETE, getPrismContext());
            objectDelta.setOid(getOid());
            return objectDelta;
        }
        // This must be a modify
        ObjectDelta<O> objectDelta = new ObjectDeltaImpl<>(getCompileTimeClass(), ChangeType.MODIFY, getPrismContext());
        objectDelta.setOid(getOid());

        Collection<? extends ItemDelta> itemDeltas = new ArrayList<>();
        diffInternal(other, itemDeltas, false, strategy);
        objectDelta.addModifications(itemDeltas);

        return objectDelta;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Collection<? extends ItemDelta<?,?>> narrowModifications(Collection<? extends ItemDelta<?, ?>> modifications,
            boolean assumeMissingItems) {
        if (modifications == null) {
            return null;
        }
        Collection narrowedModifications = new ArrayList<>(modifications.size());
        for (ItemDelta<?, ?> modification: modifications) {
            ItemDelta<?, ?> narrowedModification = modification.narrow(this, assumeMissingItems);
            if (narrowedModification != null && !narrowedModification.isEmpty()) {
                narrowedModifications.add(narrowedModification);
            }
        }
        return narrowedModifications;
    }

    public ObjectDelta<O> createDelta(ChangeType changeType) {
        ObjectDelta<O> delta = new ObjectDeltaImpl<>(getCompileTimeClass(), changeType, getPrismContext());
        delta.setOid(getOid());
        return delta;
    }

    public ObjectDelta<O> createAddDelta() {
        ObjectDelta<O> delta = createDelta(ChangeType.ADD);
        // TODO: clone?
        delta.setObjectToAdd(this);
        return delta;
    }

    public ObjectDelta<O> createModifyDelta() {
        ObjectDelta<O> delta = createDelta(ChangeType.MODIFY);
        delta.setOid(this.getOid());
        return delta;
    }

    public ObjectDelta<O> createDeleteDelta() {
        ObjectDelta<O> delta = createDelta(ChangeType.DELETE);
        delta.setOid(this.getOid());
        return delta;
    }

    public void setParent(PrismContainerValue<?> parentValue) {
        throw new IllegalStateException("Cannot set parent for an object");
    }

    @Override
    public PrismContainerValue<?> getParent() {
        return null;
    }

    @NotNull
    @Override
    public ItemPath getPath() {
        return ItemPath.EMPTY_PATH;
    }

    @Override
    protected Object getPathComponent() {
        return null;
    }

//    /**
//     * this method ignores some part of the object during comparison (e.g. source demarcation in values)
//     * These methods compare the "meaningful" parts of the objects.
//     */
//    public boolean equivalent(Object obj) {
//        if (prismContext != null && prismContext.getMonitor() != null) {
//            prismContext.getMonitor().recordPrismObjectCompareCount(this, obj);
//        }
//        if (this == obj)
//            return true;
//        if (getClass() != obj.getClass())
//            return false;
//        PrismObjectImpl other = (PrismObjectImpl) obj;
//        //noinspection unchecked
//        ObjectDelta<O> delta = diff(other, EquivalenceStrategy.REAL_VALUE);
//        return delta.isEmpty();
//    }

    @Override
    public String toString() {
        return toDebugName();
    }

    /**
     * Returns short string representing identity of this object.
     * It should container object type, OID and name. It should be presented
     * in a form suitable for log and diagnostic messages (understandable for
     * system administrator).
     */
    public String toDebugName() {
        return toDebugType()+":"+getOid()+"("+getNamePropertyStringValue()+")";
    }

    private PrismProperty<PolyString> getNameProperty() {
        QName elementName = getElementName();
        String myNamespace = elementName.getNamespaceURI();
        return findProperty(new ItemName(myNamespace, PrismConstants.NAME_LOCAL_NAME));
    }

    private String getNamePropertyStringValue() {
        PrismProperty<PolyString> nameProperty = getNameProperty();
        if (nameProperty == null) {
            return null;
        }
        PolyString realValue = nameProperty.getRealValue();
        if (realValue == null) {
            return null;
        }
        return realValue.getOrig();
    }

    /**
     * Returns short string identification of object type. It should be in a form
     * suitable for log messages. There is no requirement for the type name to be unique,
     * but it rather has to be compact. E.g. short element names are preferred to long
     * QNames or URIs.
     */
    public String toDebugType() {
        QName elementName = getElementName();
        if (elementName == null) {
            return "(unknown)";
        }
        return elementName.getLocalPart();
    }

    /**
     * Return a human readable name of this class suitable for logs.
     */
    protected String getDebugDumpClassName() {
        return "PO";
    }

    @Override
    protected void appendDebugDumpSuffix(StringBuilder sb) {
        sb.append("(").append(getOid());
        if (getVersion() != null) {
            sb.append(", v").append(getVersion());
        }
        PrismObjectDefinition<O> def = getDefinition();
        if (def != null) {
            sb.append(", ").append(DebugUtil.formatElementName(def.getTypeName()));
        }
        sb.append(")");
    }

    /**
     * Return display name intended for business users of midPoint
     */
    public String getBusinessDisplayName() {
        return getNamePropertyStringValue();
    }

    @Override
    public void checkConsistenceInternal(Itemable rootItem, boolean requireDefinitions, boolean prohibitRaw,
            ConsistencyCheckScope scope) {
        super.checkConsistenceInternal(rootItem, requireDefinitions, prohibitRaw, scope);
        if (size() > 1) {
            throw new IllegalStateException("PrismObject holding more than one value: " + size() + ": " + this);
        }
        getValue();            // checks the type by casting to POV
    }

    @Override
    public void performFreeze() {
        if (!this.immutable && values.isEmpty()) {
            createNewValue();
        }
        super.performFreeze();
    }

    public PrismObject<O> cloneIfImmutable() {
        return isImmutable() ? clone() : this;
    }

    @NotNull
    public static <T extends Objectable> List<T> asObjectableList(@NotNull List<PrismObjectImpl<T>> objects) {
        return objects.stream()
                .map(o -> o.asObjectable())
                .collect(Collectors.toList());
    }
}
