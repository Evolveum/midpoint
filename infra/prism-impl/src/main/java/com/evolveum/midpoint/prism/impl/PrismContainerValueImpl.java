/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.equivalence.ParameterizedEquivalenceStrategy;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.impl.xnode.RootXNodeImpl;
import com.evolveum.midpoint.prism.marshaller.JaxbDomHack;
import com.evolveum.midpoint.prism.path.*;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.*;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

/**
 * @author semancik
 *
 */
public class PrismContainerValueImpl<C extends Containerable> extends PrismValueImpl implements PrismContainerValue<C> {

    private static final Trace LOGGER = TraceManager.getTrace(PrismContainerValueImpl.class);

    // We use linked map because we need to maintain the order internally to provide consistent
    // output in DOM and other ordering-sensitive representations.
    // The QNames here should be qualified if at all possible. Unqualified names are kept here nevertheless
    // (in order to maintain the ordering) but they are maintained in a separate set to know they require a separate
    // handling.
    protected final LinkedHashMap<QName, Item<?,?>> items = new LinkedHashMap<>();
    protected final Set<String> unqualifiedItemNames = new HashSet<>();

    private Long id;

    private C containerable = null;

    // Definition of this value. Usually it is the same as CTD declared in the parent container.
    // However, in order to support polymorphism (as well as parent-less values) we distinguish between PC and PCV type definition.
    protected ComplexTypeDefinition complexTypeDefinition = null;

    public PrismContainerValueImpl() {
    }

    public PrismContainerValueImpl(C containerable) {
        this(containerable, null);
    }

    public PrismContainerValueImpl(PrismContext prismContext) {
        this(null, prismContext);
    }

    public PrismContainerValueImpl(C containerable, PrismContext prismContext) {
        super(prismContext);
        this.containerable = containerable;

        if (prismContext != null) {
            getComplexTypeDefinition();        // to determine CTD (could be also called with null prismContext, but non-null prismContext provides additional information in some cases)
        }
    }

    public PrismContainerValueImpl(OriginType type, Objectable source, PrismContainerable container, Long id, ComplexTypeDefinition complexTypeDefinition, PrismContext prismContext) {
        super(prismContext, type, source, container);
        this.id = id;
        this.complexTypeDefinition = complexTypeDefinition;
    }

    public static <T extends Containerable> T asContainerable(PrismContainerValue<T> value) {
        return value != null ? value.asContainerable() : null;
    }

    @Override
    public PrismContext getPrismContext() {
        if (prismContext != null) {
            return prismContext;
        }
        if (complexTypeDefinition != null && complexTypeDefinition.getPrismContext() != null) {
            return complexTypeDefinition.getPrismContext();
        }
        if (getParent() != null) {
            return getParent().getPrismContext();
        }
        return null;
    }

    // Primarily for testing
    public PrismContext getPrismContextLocal() {
        return prismContext;
    }

    /**
     * Returns a set of items that the property container contains. The items may be properties or inner property containers.
     * <p>
     * The set may be null. In case there are no properties an empty set is
     * returned.
     * <p>
     * Returned set is mutable. Live object is returned.
     *
     * @return set of items that the property container contains.
     */
    @NotNull
    public Collection<Item<?,?>> getItems() {
        if (isImmutable()) {
            return Collections.unmodifiableCollection(items.values());
        } else {
            return items.values();
        }
    }

    // avoid using because of performance penalty
    @SuppressWarnings("unchecked")
    public <I extends Item<?,?>> List<I> getItems(Class<I> type) {
        List<I> rv = new ArrayList<>();
        for (Item<?, ?> item : items.values()) {
            if (type.isAssignableFrom(item.getClass())) {
                rv.add(((I) item));
            }
        }
        return rv;
    }

    public int size() {
        return items.size();
    }

    /**
     * Returns a set of properties that the property container contains.
     * <p>
     * The set must not be null. In case there are no properties an empty set is
     * returned.
     * <p>
     * Returned set is immutable! Any change to it will be ignored.
     *
     * @return set of properties that the property container contains.
     */
    @NotNull
    public Set<PrismProperty<?>> getProperties() {
        Set<PrismProperty<?>> properties = new HashSet<>();
        for (Item<?,?> item : items.values()) {
            if (item instanceof PrismProperty) {
                properties.add((PrismProperty<?>) item);
            }
        }
        return properties;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        checkMutable();
        this.id = id;
    }

    @SuppressWarnings("unchecked")
    public PrismContainerable<C> getParent() {
        Itemable parent = super.getParent();
        if (parent == null) {
            return null;
        }
        if (!(parent instanceof PrismContainerable)) {
            throw new IllegalStateException("Expected that parent of "+ PrismContainerValue.class.getName()+" will be "+
                    PrismContainerable.class.getName()+", but it is "+parent.getClass().getName());
        }
        return (PrismContainerable<C>)parent;
    }

    @SuppressWarnings("unchecked")
    public PrismContainer<C> getContainer() {
        Itemable parent = super.getParent();
        if (parent == null) {
            return null;
        }
        if (!(parent instanceof PrismContainer)) {
            throw new IllegalStateException("Expected that parent of "+ PrismContainerValue.class.getName()+" will be "+
                    PrismContainer.class.getName()+", but it is "+parent.getClass().getName());
        }
        return (PrismContainer<C>)super.getParent();
    }

    @NotNull
    public ItemPath getPath() {
        Itemable parent = getParent();
        @NotNull ItemPath parentPath = ItemPath.EMPTY_PATH;
        if (parent != null) {
            parentPath = parent.getPath();
        }
        if (getId() != null) {
            return parentPath.append(getId());
        } else {
            return parentPath;
        }
    }

    @Override
    protected Object getPathComponent() {
        return getId();
    }

    // For compatibility with other PrismValue types
    public C getValue() {
        return asContainerable();
    }

    @NotNull
    public C asContainerable() {
        if (containerable != null) {
            return containerable;
        }
        if (getParent() == null && complexTypeDefinition == null) {
            throw new IllegalStateException("Cannot represent container value without a parent and complex type definition as containerable; value: " + this);
        }
        return asContainerableInternal(resolveClass(null));
    }

    public Class<C> getCompileTimeClass() {
        if (containerable != null) {
            return (Class<C>) containerable.getClass();
        } else {
            return resolveClass(null);
        }
    }

    public boolean canRepresent(Class<?> clazz) {
        Class<C> compileTimeClass = getCompileTimeClass();
        if (compileTimeClass == null) {
            return false;
        }
        return clazz.isAssignableFrom(compileTimeClass);
    }

    // returned class must be of type 'requiredClass' (or any of its subtypes)
    public C asContainerable(Class<C> requiredClass) {
        if (containerable != null) {
            return containerable;
        }
        return asContainerableInternal(resolveClass(requiredClass));
    }

    private Class<C> resolveClass(@Nullable Class<C> requiredClass) {
        if (complexTypeDefinition != null && complexTypeDefinition.getCompileTimeClass() != null) {
            Class<?> actualClass = complexTypeDefinition.getCompileTimeClass();
            if (requiredClass != null && !requiredClass.isAssignableFrom(actualClass)) {
                throw new IllegalStateException("asContainerable was called to produce " + requiredClass
                        + ", but the actual class in PCV is " + actualClass);
            } else {
                return (Class<C>) actualClass;
            }
        } else {
            PrismContainerable parent = getParent();
            if (parent != null) {
                Class<?> parentClass = parent.getCompileTimeClass();
                if (parentClass != null) {
                    if (requiredClass != null && !requiredClass.isAssignableFrom(parentClass)) {
                        // mismatch; but this can occur (see ShadowAttributesType vs ShadowIdentifiersType in ShadowAssociationType)
                        // but TODO maybe this is only a workaround and the problem is in the schema itself (?)
                        return requiredClass;
                    } else {
                        return (Class<C>) parentClass;
                    }
                }
            }
        }
        return requiredClass;
    }

    @NotNull
    private C asContainerableInternal(Class<C> clazz) {
        if (clazz == null) {
            String elementName = getParent() != null ? String.valueOf(getParent().getElementName()) : String.valueOf(this);
            throw new SystemException("Unknown compile time class of container value of '" + elementName + "'.");
        }
        if (Modifier.isAbstract(clazz.getModifiers())) {
            throw new SystemException("Can't create instance of class '" + clazz.getSimpleName() + "', it's abstract.");
        }
        try {
            if (prismContext != null) {
                containerable = clazz.getConstructor(PrismContext.class).newInstance(prismContext);
            } else {
                containerable = clazz.newInstance();
            }
            containerable.setupContainerValue(this);
            return containerable;
        } catch (SystemException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SystemException("Couldn't create jaxb object instance of '" + clazz + "': "+ex.getMessage(), ex);
        }
    }

    @NotNull
    public Collection<QName> getItemNames() {
        return new ArrayList<>(items.keySet());
    }

    public <IV extends PrismValue,ID extends ItemDefinition> void add(Item<IV,ID> item) throws SchemaException {
        add(item, true);
    }

    /**
     * Adds an item to a property container.
     *
     * @param item item to add.
     * @throws IllegalArgumentException an attempt to add value that already exists
     */
    public <IV extends PrismValue,ID extends ItemDefinition> void add(Item<IV,ID> item, boolean checkUniqueness) throws SchemaException {
        checkMutable();
        ItemName itemName = item.getElementName();
        if (itemName == null) {
            throw new IllegalArgumentException("Cannot add item without a name to value of container "+getParent());
        }
        if (checkUniqueness && findItem(itemName, Item.class) != null) {
            throw new IllegalArgumentException("Item " + itemName + " is already present in " + this.getClass().getSimpleName());
        }
        item.setParent(this);
        PrismContext prismContext = getPrismContext();
        if (prismContext != null) {
            item.setPrismContext(prismContext);
        }
        if (getComplexTypeDefinition() != null && item.getDefinition() == null) {
            item.applyDefinition((ID)determineItemDefinition(itemName, getComplexTypeDefinition()), false);
        }
        simpleAdd(item);
    }

    private <IV extends PrismValue, ID extends ItemDefinition> void simpleAdd(Item<IV, ID> item) {
        @NotNull ItemName itemName = item.getElementName();
//        if (itemName.getLocalPart().equals("modelOperationContext")) {
//            System.out.println("Hello!");
//        }
        items.put(itemName, item);
        if (QNameUtil.isUnqualified(itemName)) {
            unqualifiedItemNames.add(itemName.getLocalPart());
        }
    }

    /**
     * Merges the provided item into this item. The values are joined together.
     * Returns true if new item or value was added.
     */
    public <IV extends PrismValue,ID extends ItemDefinition> boolean merge(Item<IV,ID> item) throws SchemaException {
        checkMutable();
        Item<IV, ID> existingItem = findItem(item.getElementName(), Item.class);
        if (existingItem == null) {
            add(item);
            return true;
        } else {
            boolean changed = false;
            for (IV newVal: item.getValues()) {
                if (existingItem.add((IV) newVal.clone())) {
                    changed = true;
                }
            }
            if (item.isIncomplete()) {
                existingItem.setIncomplete(true);
            }
            return changed;
        }
    }

    /**
     * Subtract the provided item from this item. The values of the provided item are deleted
     * from this item.
     * Returns true if this item was changed.
     */
    public <IV extends PrismValue,ID extends ItemDefinition> boolean subtract(Item<IV,ID> item) throws SchemaException {
        checkMutable();
        Item<IV,ID> existingItem = findItem(item.getElementName(), Item.class);
        if (existingItem == null) {
            return false;
        } else {
            boolean changed = false;
            for (IV newVal: item.getValues()) {
                if (existingItem.remove(newVal)) {
                    changed = true;
                }
            }
            return changed;
        }
    }

    /**
     * Adds an item to a property container. Existing value will be replaced.
     *
     * @param item item to add.
     */
    public <IV extends PrismValue,ID extends ItemDefinition> void addReplaceExisting(Item<IV,ID> item) throws SchemaException {
        checkMutable();
        if (item == null) {
            return;
        }
        remove(item);
        add(item);
    }

    public <IV extends PrismValue,ID extends ItemDefinition> void remove(@NotNull Item<IV,ID> item) {
        checkMutable();

        Item<IV,ID> existingItem = findItem(item.getElementName(), Item.class);
        if (existingItem != null) {
            ItemName existingItemName = existingItem.getElementName();
            items.remove(existingItemName);
            removeFromUnqualifiedIfNeeded(existingItemName);
            existingItem.setParent(null);
        }
    }

    public void removeAll() {
        checkMutable();
        Iterator<Item<?,?>> iterator = items.values().iterator();
        while (iterator.hasNext()) {
            Item<?,?> item = iterator.next();
            item.setParent(null);
            iterator.remove();
        }
        unqualifiedItemNames.clear();
    }

    /**
     * Adds a collection of items to a property container.
     *
     * @param itemsToAdd items to add
     * @throws IllegalArgumentException an attempt to add value that already exists
     */
    public void addAll(Collection<? extends Item<?,?>> itemsToAdd) throws SchemaException {
        for (Item<?,?> item : itemsToAdd) {
            add(item);
        }
    }

    /**
     * Adds a collection of items to a property container. Existing values will be replaced.
     *
     * @param itemsToAdd items to add
     */
    public void addAllReplaceExisting(Collection<? extends Item<?,?>> itemsToAdd) throws SchemaException {
        checkMutable();
        for (Item<?,?> item : itemsToAdd) {
            addReplaceExisting(item);
        }
    }

    public <IV extends PrismValue,ID extends ItemDefinition> void replace(Item<IV,ID> oldItem, Item<IV,ID> newItem) throws SchemaException {
        remove(oldItem);
        add(newItem);
    }

    public void clear() {
        checkMutable();
        items.clear();
        unqualifiedItemNames.clear();
    }

    public boolean contains(Item item) {
        return items.values().contains(item);
    }

    public boolean contains(ItemName itemName) {
        return findItem(itemName) != null;
    }

    public static <C extends Containerable> boolean containsRealValue(Collection<PrismContainerValue<C>> cvalCollection,
            PrismContainerValue<C> cval) {
        for (PrismContainerValue<C> colVal: cvalCollection) {
            if (colVal.equals(cval, EquivalenceStrategy.REAL_VALUE)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Object find(ItemPath path) {
        if (path == null || path.isEmpty()) {
            return this;
        }
        Object first = path.first();
        if (!ItemPath.isName(first)) {
            throw new IllegalArgumentException("Attempt to lookup item using a non-name path "+path+" in "+this);
        }
        ItemName subName = ItemPath.toName(first);
        ItemPath rest = path.rest();
        Item<?,?> subItem = findItem(subName);
        if (subItem == null) {
            return null;
        }
        return subItem.find(rest);
    }

    @Override
    public <IV extends PrismValue,ID extends ItemDefinition> PartiallyResolvedItem<IV,ID> findPartial(ItemPath path) {
        if (path == null || path.isEmpty()) {
            // Incomplete path
            return null;
        }
        Object first = path.first();
        if (!ItemPath.isName(first)) {
            throw new IllegalArgumentException("Attempt to lookup item using a non-name path "+path+" in "+this);
        }
        ItemName subName = ItemPath.toName(first);
        ItemPath rest = path.rest();
        Item<?,?> subItem = findItem(subName);
        if (subItem == null) {
            return null;
        }
        return subItem.findPartial(rest);
    }

    public <X> PrismProperty<X> findProperty(ItemPath propertyPath) {
        return (PrismProperty) findItem(propertyPath, PrismProperty.class);
    }

    /**
     * Finds a specific property in the container by definition.
     * <p>
     * Returns null if nothing is found.
     *
     * @param propertyDefinition property definition to find.
     * @return found property or null
     */
    public <X> PrismProperty<X> findProperty(PrismPropertyDefinition<X> propertyDefinition) {
        if (propertyDefinition == null) {
            throw new IllegalArgumentException("No property definition");
        }
        return findProperty(propertyDefinition.getItemName());
    }

    public <X extends Containerable> PrismContainer<X> findContainer(QName containerName) {
        return findItem(ItemName.fromQName(containerName), PrismContainer.class);
    }

    public PrismReference findReference(QName elementName) {
        return findItem(ItemName.fromQName(elementName), PrismReference.class);
    }

    // todo optimize this some day
    public PrismReference findReferenceByCompositeObjectElementName(QName elementName) {
        for (Item item: items.values()) {
            if (item instanceof PrismReference) {
                PrismReference ref = (PrismReference)item;
                PrismReferenceDefinition refDef = ref.getDefinition();
                if (refDef != null) {
                    if (elementName.equals(refDef.getCompositeObjectElementName())) {
                        return ref;
                    }
                }
            }
        }
        return null;
    }

    public <IV extends PrismValue,ID extends ItemDefinition, I extends Item<IV,ID>> I findItem(ItemPath itemPath, Class<I> type) {
        try {
            return findCreateItem(itemPath, type, null, false);
        } catch (SchemaException e) {
            // This should not happen
            throw new SystemException("Internal Error: "+e.getMessage(),e);
        }
    }

    @SuppressWarnings("unchecked")
    <IV extends PrismValue,ID extends ItemDefinition, I extends Item<IV,ID>> I findCreateItem(QName itemName, Class<I> type, ID itemDefinition, boolean create) throws SchemaException {
        Item<IV,ID> item = findItemByQName(itemName);
        if (item != null) {
            if (type.isAssignableFrom(item.getClass())) {
                return (I) item;
            } else {
                if (create) {
                    throw new IllegalStateException("The " + type.getSimpleName() + " cannot be created because "
                            + item.getClass().getSimpleName() + " with the same name exists ("+item.getElementName()+")");
                } else {
                    return null;
                }
            }
        }
        if (create) {   // todo treat unqualified names
            return createSubItem(itemName, type, itemDefinition);
        } else {
            return null;
        }
    }

    public <IV extends PrismValue,ID extends ItemDefinition, I extends Item<IV,ID>> I findItem(ItemDefinition itemDefinition, Class<I> type) {
        if (itemDefinition == null) {
            throw new IllegalArgumentException("No item definition");
        }
        return findItem(itemDefinition.getItemName(), type);
    }

    public boolean containsItem(ItemPath path, boolean acceptEmptyItem) throws SchemaException {
        if (!path.startsWithName()) {
            throw new IllegalArgumentException("Attempt to lookup item using a non-name path "+path+" in "+this);
        }
        QName subName = path.firstToName();
        ItemPath rest = path.rest();
        Item item = findItemByQName(subName);
        if (item != null) {
            if (rest.isEmpty()) {
                return acceptEmptyItem || !item.isEmpty();
            } else {
                // Go deeper
                if (item instanceof PrismContainer) {
                    return ((PrismContainer<?>)item).containsItem(rest, acceptEmptyItem);
                } else {
                    return acceptEmptyItem || !item.isEmpty();
                }
            }
        }

        return false;
    }

    // Expects that "self" path is NOT present in propPath
    @SuppressWarnings("unchecked")
    <IV extends PrismValue,ID extends ItemDefinition, I extends Item<IV,ID>> I findCreateItem(ItemPath propPath, Class<I> type, ID itemDefinition, boolean create) throws SchemaException {
        Object first = propPath.first();
        if (!ItemPath.isName(first)) {
            throw new IllegalArgumentException("Attempt to lookup item using a non-name path "+propPath+" in "+this);
        }
        ItemName subName = ItemPath.toName(first);
        ItemPath rest = propPath.rest();
        I item = (I) findItemByQName(subName);
        if (item != null) {
            if (rest.isEmpty()) {
                if (type.isAssignableFrom(item.getClass())) {
                    return (I)item;
                } else {
                    if (create) {
                        throw new SchemaException("The " + type.getSimpleName() + " cannot be created because "
                                + item.getClass().getSimpleName() + " with the same name exists ("+item.getElementName()+")");
                    } else {
                        return null;
                    }
                }
            } else {
                // Go deeper
                if (item instanceof PrismContainer) {
                    return ((PrismContainer<?>)item).findCreateItem(rest, type, itemDefinition, create);
                } else {
                    if (create) {
                        throw new SchemaException("The " + type.getSimpleName() + " cannot be created because "
                                + item.getClass().getSimpleName() + " with the same name exists ("+item.getElementName()+")");
                    } else {
                        // Return the item for non-container even if the path is non-empty
                        // FIXME: This is not the best solution but it is needed to be able to look inside properties
                        // such as PolyString
                        if (type.isAssignableFrom(item.getClass())) {
                            return item;
                        } else {
                            return null;
                        }
                    }
                }
            }
        }

        if (create) {       // todo treat unqualified names
            if (rest.isEmpty()) {
                return createSubItem(subName, type, itemDefinition);
            } else {
                // Go deeper
                PrismContainer<?> subItem = createSubItem(subName, PrismContainer.class, null);
                return subItem.findCreateItem(rest, type, itemDefinition, create);
            }
        } else {
            return null;
        }
    }

    private <IV extends PrismValue,ID extends ItemDefinition> Item<IV,ID> findItemByQName(QName subName) throws SchemaException {
        if (QNameUtil.isUnqualified(subName) || unqualifiedItemNames.contains(subName.getLocalPart())) {
            return findItemByQNameFullScan(subName);
        } else {
            //noinspection unchecked
            return (Item<IV, ID>) items.get(subName);
        }
    }

    private <IV extends PrismValue,ID extends ItemDefinition> Item<IV,ID> findItemByQNameFullScan(QName subName) throws SchemaException {
//        LOGGER.warn("Full scan while finding {} in {}", subName, this);
        Item<IV,ID> matching = null;
        for (Item<?,?> item : items.values()) {
            if (QNameUtil.match(subName, item.getElementName())) {
                if (matching != null) {
                    String containerName = getParent() != null ? DebugUtil.formatElementName(getParent().getElementName()) : "";
                    throw new SchemaException("More than one items matching " + subName + " in container " + containerName);
                } else {
                    matching = (Item<IV, ID>) item;
                }
            }
        }
        return matching;
    }

    public <IV extends PrismValue,ID extends ItemDefinition,I extends Item<IV,ID>> I createDetachedSubItem(QName name,
            Class<I> type, ID itemDefinition, boolean immutable) throws SchemaException {
        I newItem = createDetachedNewItemInternal(name, type, itemDefinition);
        if (immutable) {
            newItem.freeze();
        }
        return newItem;
    }

    private <IV extends PrismValue,ID extends ItemDefinition,I extends Item<IV,ID>> I createSubItem(QName name, Class<I> type, ID itemDefinition) throws SchemaException {
        checkMutable();
        I newItem = createDetachedNewItemInternal(name, type, itemDefinition);
        add(newItem);
        return newItem;
    }

    private <IV extends PrismValue,ID extends ItemDefinition,I extends Item<IV,ID>> I createDetachedNewItemInternal(QName name, Class<I> type,
            ID itemDefinition) throws SchemaException {
        I newItem;
        if (itemDefinition == null) {
            ComplexTypeDefinition ctd = getComplexTypeDefinition();
            itemDefinition = determineItemDefinition(name, ctd);
            if (ctd != null && itemDefinition == null) {
                throw new SchemaException("No definition for item "+name+" in "+getParent());
            }
        }

        if (itemDefinition != null) {
            if (StringUtils.isNotBlank(name.getNamespaceURI())) {
                newItem = (I) itemDefinition.instantiate(name);
            } else {
                QName computed = new QName(itemDefinition.getNamespace(), name.getLocalPart());
                newItem = (I) itemDefinition.instantiate(computed);
            }
            if (newItem instanceof PrismObject) {
                throw new IllegalStateException("PrismObject instantiated as a subItem in "+this+" from definition "+itemDefinition);
            }
        } else {
            newItem = ItemImpl.createNewDefinitionlessItem(name, type, prismContext);
            if (newItem instanceof PrismObject) {
                throw new IllegalStateException("PrismObject instantiated as a subItem in "+this+" as definitionless instance of class "+type);
            }
        }
        if (type.isAssignableFrom(newItem.getClass())) {
            return newItem;
        } else {
            throw new IllegalStateException("The " + type.getSimpleName() + " cannot be created because the item should be of type "
                    + newItem.getClass().getSimpleName() + " (item: "+newItem.getElementName()+")");
        }
    }

    public <T extends Containerable> PrismContainer<T> findOrCreateContainer(QName containerName) throws SchemaException {
        return findCreateItem(containerName, PrismContainer.class, null, true);
    }

    public PrismReference findOrCreateReference(QName referenceName) throws SchemaException {
        return findCreateItem(referenceName, PrismReference.class, null, true);
    }

    public <IV extends PrismValue,ID extends ItemDefinition> Item<IV,ID> findOrCreateItem(QName containerName) throws SchemaException {
        return findCreateItem(containerName, Item.class, null, true);
    }

    public <IV extends PrismValue,ID extends ItemDefinition,I extends Item<IV,ID>> I findOrCreateItem(QName containerName, Class<I> type) throws SchemaException {
        return findCreateItem(containerName, type, null, true);
    }

    public <IV extends PrismValue,ID extends ItemDefinition,I extends Item<IV,ID>> I findOrCreateItem(ItemPath path, Class<I> type, ID definition) throws SchemaException {
        return findCreateItem(path, type, definition, true);
    }

//    public <X> PrismProperty<X> findOrCreateProperty(QName propertyQName) throws SchemaException {
//        PrismProperty<X> property = findItem(ItemName.fromQName(propertyQName), PrismProperty.class);
//        if (property != null) {
//            return property;
//        }
//        return createProperty(propertyQName);
//    }

    public <X> PrismProperty<X> findOrCreateProperty(ItemPath propertyPath) throws SchemaException {
        return findOrCreateItem(propertyPath, PrismProperty.class, null);
    }

    public <X> PrismProperty<X> findOrCreateProperty(PrismPropertyDefinition propertyDef) throws SchemaException {
        PrismProperty<X> property = findItem(propertyDef.getItemName(), PrismProperty.class);
        if (property != null) {
            return property;
        }
        return createProperty(propertyDef);
    }

    public <X> PrismProperty<X> createProperty(QName propertyName) throws SchemaException {
        checkMutable();
        PrismPropertyDefinition propertyDefinition = determineItemDefinition(propertyName, getComplexTypeDefinition());
        if (propertyDefinition == null) {
            // container has definition, but there is no property definition. This is either runtime schema
            // or an error
            if (getParent() != null && getDefinition() != null && !getDefinition().isRuntimeSchema()) {        // TODO clean this up
                throw new IllegalArgumentException("No definition for property "+propertyName+" in "+complexTypeDefinition);
            }
        }
        PrismProperty<X> property;
        if (propertyDefinition == null) {
            property = new PrismPropertyImpl<>(propertyName, prismContext);        // Definitionless
        } else {
            property = propertyDefinition.instantiate();
        }
        add(property, false);
        return property;
    }

    public <X> PrismProperty<X> createProperty(PrismPropertyDefinition propertyDefinition) throws SchemaException {
        PrismProperty<X> property = propertyDefinition.instantiate();
        add(property);
        return property;
    }

    public void removeProperty(ItemPath path) {
        removeItem(path, PrismProperty.class);
    }

    public void removeContainer(ItemPath path) {
        removeItem(path, PrismContainer.class);
    }

    public void removeReference(ItemPath path) {
        removeItem(path, PrismReference.class);
    }

    // Expects that "self" path is NOT present in propPath
    <I extends Item<?,?>> void removeItem(ItemPath itemPath, Class<I> itemType) {
        checkMutable();
        if (!itemPath.startsWithName()) {
            throw new IllegalArgumentException("Attempt to remove item using a non-name path "+itemPath+" in "+this);
        }
        QName subName = itemPath.firstToName();
        ItemPath rest = itemPath.rest();
        Iterator<Item<?,?>> itemsIterator = items.values().iterator();
        while (itemsIterator.hasNext()) {
            Item<?,?> item = itemsIterator.next();
            ItemName itemName = item.getElementName();
            if (subName.equals(itemName)) {
                if (!rest.isEmpty() && item instanceof PrismContainer) {
                    ((PrismContainer) item).removeItem(rest, itemType);
                    return;
                } else {
                    if (itemType.isAssignableFrom(item.getClass())) {
                        itemsIterator.remove();
                        removeFromUnqualifiedIfNeeded(itemName);
                    } else {
                           throw new IllegalArgumentException("Attempt to remove item "+subName+" from "+this+
                                   " of type "+itemType+" while the existing item is of incompatible type "+item.getClass());
                    }
                }
            }
        }
    }

    private void removeFromUnqualifiedIfNeeded(ItemName itemName) {
        if (QNameUtil.isUnqualified(itemName)) {
            removeUnqualifiedItemName(itemName);
        }
    }

    private void removeUnqualifiedItemName(ItemName itemName) {
        for (Item<?, ?> item : items.values()) {
            if (itemName.equals(item.getElementName())) {
                return;
            }
        }
        unqualifiedItemNames.remove(itemName.getLocalPart());
    }

    public <T> void setPropertyRealValue(QName propertyName, T realValue, PrismContext prismContext) throws SchemaException {
        checkMutable();
        PrismProperty<T> property = findOrCreateProperty(ItemName.fromQName(propertyName));
        property.setRealValue(realValue);
        if (property.getPrismContext() == null) {
            property.setPrismContext(prismContext);
        }
    }

    public <T> T getPropertyRealValue(QName propertyName, Class<T> type) {
        PrismProperty<T> property = findProperty(ItemName.fromQName(propertyName));
        if (property == null) {          // when using sql repo, even non-existing properties do not have 'null' here
            return null;
        }
        return property.getRealValue(type);
    }

    @Override
    public void recompute(PrismContext prismContext) {
        // Nothing to do. The subitems should be already recomputed as they are added to this container.
    }

    @Override
    public void accept(Visitor visitor) {
        super.accept(visitor);
        for (Item<?,?> item : new ArrayList<>(items.values())) {     // to allow modifying item list via the acceptor
            item.accept(visitor);
        }
    }

    @Override
    public void accept(Visitor visitor, ItemPath path, boolean recursive) {
        if (path == null || path.isEmpty()) {
            // We are at the end of path, continue with regular visits all the way to the bottom
            if (recursive) {
                accept(visitor);
            } else {
                visitor.visit(this);
            }
        } else {
            Object first = path.first();
            if (!ItemPath.isName(first)) {
                throw new IllegalArgumentException("Attempt to lookup item using a non-name path "+path+" in "+this);
            }
            QName subName = ItemPath.toName(first);
            ItemPath rest = path.rest();
            for (Item<?,?> item : items.values()) {            // todo unqualified names!
                if (subName.equals(item.getElementName())) {
                    item.accept(visitor, rest, recursive);
                }
            }
        }
    }

    @Override
    public void acceptParentVisitor(Visitor visitor) {
        visitor.visit(this);
        PrismContainerable<C> parent = getParent();
        if (parent != null) {
            parent.acceptParentVisitor(visitor);
        }
    }

    public boolean hasCompleteDefinition() {
        for (Item<?,?> item : getItems()) {
            if (!item.hasCompleteDefinition()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean representsSameValue(PrismValue other, boolean lax) {
        return other instanceof PrismContainerValue && representsSameValue((PrismContainerValue<C>) other, lax);
    }

    @SuppressWarnings("Duplicates")
    private boolean representsSameValue(PrismContainerValue<C> other, boolean lax) {
        if (lax && getParent() != null) {
            PrismContainerDefinition definition = getDefinition();
            if (definition != null) {
                if (definition.isSingleValue()) {
                    // There is only one value, therefore it always represents the same thing
                    return true;
                }
            }
        }
        if (lax && other.getParent() != null) {
            PrismContainerDefinition definition = other.getDefinition();
            if (definition != null) {
                if (definition.isSingleValue()) {
                    // There is only one value, therefore it always represents the same thing
                    return true;
                }
            }
        }
        return this.getId() != null && other.getId() != null && this.getId().equals(other.getId());
    }

    @Override
    public void diffMatchingRepresentation(PrismValue otherValue,
            Collection<? extends ItemDelta> deltas, ParameterizedEquivalenceStrategy strategy) {
        if (otherValue instanceof PrismContainerValue) {
            diffRepresentation((PrismContainerValue)otherValue, deltas, strategy);
        } else {
            throw new IllegalStateException("Comparing incompatible values "+this+" - "+otherValue);
        }
    }

    private void diffRepresentation(PrismContainerValue<C> otherValue,
            Collection<? extends ItemDelta> deltas, ParameterizedEquivalenceStrategy strategy) {
        diffItems(this, otherValue, deltas, strategy);
    }

    @Override
    public boolean isRaw() {
        return false;
    }

    public boolean addRawElement(Object element) throws SchemaException {
        checkMutable();
        PrismContainerDefinition<C> definition = getDefinition();
        if (definition == null) {
            throw new UnsupportedOperationException("Definition-less containers are not supported any more.");
        } else {
            // We have definition here, we can parse it right now
            Item<?,?> subitem = parseRawElement(element, definition);
            return merge(subitem);
        }
    }

    public boolean deleteRawElement(Object element) throws SchemaException {
        checkMutable();
        PrismContainerDefinition<C> definition = getDefinition();
        if (definition == null) {
            throw new UnsupportedOperationException("Definition-less containers are not supported any more.");
        } else {
            // We have definition here, we can parse it right now
            Item<?,?> subitem = parseRawElement(element, definition);
            return subtract(subitem);
        }
    }


    public boolean removeRawElement(Object element) {
        checkMutable();
        throw new UnsupportedOperationException("Definition-less containers are not supported any more.");
    }

    private <IV extends PrismValue,ID extends ItemDefinition> Item<IV,ID> parseRawElement(Object element, PrismContainerDefinition<C> definition) throws SchemaException {
        JaxbDomHack jaxbDomHack = ((PrismContextImpl) definition.getPrismContext()).getJaxbDomHack();
        return jaxbDomHack.parseRawElement(element, definition);
    }

    private void diffItems(PrismContainerValue<C> thisValue, PrismContainerValue<C> other,
            Collection<? extends ItemDelta> deltas, ParameterizedEquivalenceStrategy strategy) {

        thisValue.getItems();
        for (Item<?,?> thisItem: thisValue.getItems()) {
            Item otherItem = other.findItem(thisItem.getElementName());
            if (!strategy.isConsideringOperationalData()) {
                ItemDefinition itemDef = thisItem.getDefinition();
                if (itemDef == null && other.getDefinition() != null) {
                    itemDef = other.getDefinition().findLocalItemDefinition(thisItem.getElementName());
                }
                if (isOperationalOnly(thisItem, itemDef)
                        && (otherItem == null || isOperationalOnly(otherItem, itemDef))) {
                    continue;
                }
            }
            // The "delete" delta will also result from the following diff
            ((ItemImpl) thisItem).diffInternal(otherItem, deltas, false, strategy);
        }

        other.getItems();
        for (Item otherItem: other.getItems()) {
            Item thisItem = thisValue.findItem(otherItem.getElementName());
            if (thisItem != null) {
                // Already processed in previous loop
                continue;
            }
            if (!strategy.isConsideringOperationalData()) {
                ItemDefinition itemDef = otherItem.getDefinition();
                if (itemDef == null && thisValue.getDefinition() != null) {
                    itemDef = thisValue.getDefinition().findLocalItemDefinition(otherItem.getElementName());
                }
                if (isOperationalOnly(otherItem, itemDef)) {
                    continue;
                }
            }

            // Other has an item that we don't have, this must be an add
            ItemDelta itemDelta = otherItem.createDelta();
            itemDelta.addValuesToAdd(otherItem.getClonedValues());
            if (!itemDelta.isEmpty()) {
                ((Collection)deltas).add(itemDelta);
            }
        }
    }

    private boolean isOperationalOnly(Item item, ItemDefinition itemDef) {
        if (itemDef != null && itemDef.isOperational()) {
            return true;
        }
        if (item.isEmpty()) {
            return false;
        }
        if (!(item instanceof PrismContainer)) {
            return false;
        }
        PrismContainer<?> container = (PrismContainer)item;
        for (PrismContainerValue<?> cval: container.getValues()) {
            if (cval != null) {
                Collection<Item<?, ?>> subitems = cval.getItems();
                for (Item<?, ?> subitem: subitems) {
                    ItemDefinition subItemDef = subitem.getDefinition();
                    if (subItemDef == null && itemDef != null) {
                        subItemDef = ((PrismContainerDefinition)itemDef).findItemDefinition(subitem.getElementName());
                    }
                    if (subItemDef == null) {
                        return false;
                    }
                    if (!subItemDef.isOperational()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public PrismContainerDefinition<C> getDefinition() {
        return (PrismContainerDefinition<C>) super.getDefinition();
    }

    @Override
    public void applyDefinition(ItemDefinition definition, boolean force) throws SchemaException {
        checkMutable();
        if (!(definition instanceof PrismContainerDefinition)) {
            throw new IllegalArgumentException("Cannot apply "+definition+" to container " + this);
        }
        applyDefinition((PrismContainerDefinition<C>)definition, force);
    }

    public void applyDefinition(@NotNull PrismContainerDefinition<C> containerDef, boolean force) throws SchemaException {
        checkMutable();
        ComplexTypeDefinition definitionToUse = containerDef.getComplexTypeDefinition();
        if (complexTypeDefinition != null) {
            if (!force) {
                return;                // there's a definition already
            }
            if (!complexTypeDefinition.getTypeName().equals(containerDef.getTypeName())) {
                // the second condition is a hack because e.g.
                // {http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/bundle/com.evolveum.icf.dummy/com.evolveum.icf.dummy.connector.DummyConnector}ConfigurationType
                // is clearly a runtime schema but it is _not_ marked as such (why?) -- see TestUcfDummy
                if (!definitionToUse.isRuntimeSchema() && definitionToUse.getCompileTimeClass() != null) {
                    // this is the case in which we are going to overwrite a specific definition
                    // (e.g. WfPrimaryChangeProcessorStateType) with a generic one (e.g. WfProcessorSpecificStateType)
                    // --> we should either skip this, or fetch the fresh definition from the prism context
                    ComplexTypeDefinition freshCtd = prismContext.getSchemaRegistry().findComplexTypeDefinitionByType(complexTypeDefinition.getTypeName());
                    if (freshCtd != null) {
                        //System.out.println("Using " + freshCtd + " instead of " + definitionToUse);
                        definitionToUse = freshCtd;
                    }
                } else {
                    // we are probably applying a dynamic definition over static one -- so, let's proceed
                }
            }
        }
        replaceComplexTypeDefinition(definitionToUse);

        // we need to continue even if CTD is null or 'any' - e.g. to resolve definitions within object extension
        applyDefinitionToItems(force);
    }

    private void applyDefinitionToItems(boolean force) throws SchemaException {
        // We change items during this operation, so we need to create a copy of them.
        ArrayList<Item<?, ?>> existingItems = new ArrayList<>(items.values());

        for (Item item : existingItems) {
            if (item.getDefinition() == null || force) {
                ItemDefinition<?> itemDefinition = determineItemDefinition(item.getElementName(), complexTypeDefinition);
                if (itemDefinition == null && item.getDefinition() != null && item.getDefinition().isDynamic()) {
                    // We will not apply the null definition here. The item has a dynamic definition that we don't
                    // want to destroy as it cannot be reconstructed later.
                } else if (item instanceof PrismProperty && itemDefinition instanceof PrismContainerDefinition) {
                    // Special case: we parsed something as (raw) property but later we found out it's in fact a container!
                    // (It could be also a reference but this will be implemented later.)
                    parseRawPropertyAsContainer((PrismProperty<?>) item, (PrismContainerDefinition<?>) itemDefinition);
                } else {
                    //noinspection unchecked
                    item.applyDefinition(itemDefinition, force);
                }
            } else {
                // Item has a definition already, no need to apply it
            }
        }
    }

    private <C1 extends Containerable> void parseRawPropertyAsContainer(PrismProperty<?> property,
            PrismContainerDefinition<C1> containerDefinition) throws SchemaException {
        PrismContainer<C1> container = containerDefinition.instantiate();
        for (PrismPropertyValue<?> value : property.getValues()) {
            XNode rawElement = value.getRawElement();
            if (rawElement == null) {
                throw new IllegalStateException("Couldn't apply container definition (" + containerDefinition
                        + ") to a non-raw property value: " + value);
            } else {
                RootXNodeImpl rootXnode = new RootXNodeImpl(containerDefinition.getItemName(), rawElement);
                PrismValue parsedValue = prismContext.parserFor(rootXnode).definition(containerDefinition).parseItemValue();
                if (parsedValue instanceof PrismContainerValue) {
                    //noinspection unchecked
                    container.add((PrismContainerValue<C1>) parsedValue);
                }
            }
        }
        remove(property);
        add(container);
    }

    /**
     * This method can both return null and throws exception. It returns null in case there is no definition
     * but it is OK (e.g. runtime schema). It throws exception if there is no definition and it is not OK.
     */
    @SuppressWarnings("unchecked")
    private <ID extends ItemDefinition> ID determineItemDefinition(QName itemName, @Nullable ComplexTypeDefinition ctd) throws SchemaException {
        ID itemDefinition = ctd != null ? ctd.findLocalItemDefinition(itemName) : null;
        if (itemDefinition != null) {
            return itemDefinition;
        }
        if (ctd == null || ctd.isXsdAnyMarker() || ctd.isRuntimeSchema()) {
            // If we have prism context, try to locate global definition. But even if that is not
            // found it is still OK. This is runtime container. We tolerate quite a lot here.
            PrismContext prismContext = getPrismContext();
            if (prismContext != null) {
                return (ID) prismContext.getSchemaRegistry().resolveGlobalItemDefinition(itemName, ctd);
            } else {
                return null;
            }
        } else {
            throw new SchemaException("No definition for item " + itemName + " in " + getParent());
        }
    }

    @Override
    public void revive(PrismContext prismContext) throws SchemaException {
        if (this.prismContext == null) {
            this.prismContext = prismContext;
        }
        super.revive(prismContext);
        for (Item<?,?> item: items.values()) {
            item.revive(prismContext);
        }
    }

    @Override
    public boolean isEmpty() {
        return id == null && hasNoItems();
    }

    @Override
    public boolean hasNoItems() {
        return items.isEmpty();
    }

    public boolean isIdOnly() {
        return id != null && hasNoItems();
    }

    @Override
    public void normalize() {
        checkMutable();
        for (Item<?, ?> item : items.values()) {
            item.normalize();
        }
    }

    @Override
    public void checkConsistenceInternal(Itemable rootItem, boolean requireDefinitions, boolean prohibitRaw, ConsistencyCheckScope scope) {
        ItemPath myPath = getPath();
        if (getDefinition() == null) {
            throw new IllegalStateException("Definition-less container value " + this +" ("+myPath+" in "+rootItem+")");
        }
        for (Item<?,?> item: items.values()) {
            if (scope.isThorough()) {
                if (item == null) {
                    throw new IllegalStateException("Null item in container value "+this+" ("+myPath+" in "+rootItem+")");
                }
                if (item.getParent() == null) {
                    throw new IllegalStateException("No parent for item "+item+" in container value "+this+" ("+myPath+" in "+rootItem+")");
                }
                if (item.getParent() != this) {
                    throw new IllegalStateException("Wrong parent for item "+item+" in container value "+this+" ("+myPath+" in "+rootItem+"), " +
                            "bad parent: "+ item.getParent());
                }
            }
            item.checkConsistenceInternal(rootItem, requireDefinitions, prohibitRaw, scope);
        }
    }

    public void assertDefinitions(String sourceDescription) throws SchemaException {
        assertDefinitions(false, sourceDescription);
    }

    public void assertDefinitions(boolean tolerateRaw, String sourceDescription) throws SchemaException {
        for (Item<?,?> item: getItems()) {
            item.assertDefinitions(tolerateRaw, "value("+getId()+") in "+sourceDescription);
        }
    }

    @Override
    public PrismContainerValue<C> clone() {
        return cloneComplex(CloneStrategy.LITERAL);
    }

    @Override
    public PrismContainerValue<C> createImmutableClone() {
        //noinspection unchecked
        return (PrismContainerValue<C>) super.createImmutableClone();
    }

    @Override
    public PrismContainerValueImpl<C> cloneComplex(CloneStrategy strategy) {    // TODO resolve also the definition?
        PrismContainerValueImpl<C> clone = new PrismContainerValueImpl<>(getOriginType(), getOriginObject(), getParent(), null,
                this.complexTypeDefinition, this.prismContext);
        copyValues(strategy, clone);
        return clone;
    }

    protected void copyValues(CloneStrategy strategy, PrismContainerValueImpl<C> clone) {
        super.copyValues(strategy, clone);
        if (strategy == CloneStrategy.LITERAL) {
            clone.id = this.id;
        }
        for (Item<?,?> item : this.items.values()) {
            Item<?,?> clonedItem = item.cloneComplex(strategy);
            clonedItem.setParent(clone);
            clone.simpleAdd(clonedItem);
        }
    }

    void deepCloneDefinition(boolean ultraDeep, PrismContainerDefinition<C> clonedContainerDef, Consumer<ItemDefinition> postCloneAction) {
        // special treatment of CTD (we must not simply overwrite it with clonedPCD.CTD!)
        PrismContainerable<?> parent = getParent();
        if (parent != null && complexTypeDefinition != null) {
            if (complexTypeDefinition == parent.getComplexTypeDefinition()) {
                replaceComplexTypeDefinition(clonedContainerDef.getComplexTypeDefinition());
            } else {
                replaceComplexTypeDefinition(complexTypeDefinition.deepClone(ultraDeep ? null : new HashMap<>(), new HashMap<>(), postCloneAction));        // OK?
            }
        }
        for (Item<?,?> item: items.values()) {
            deepCloneDefinitionItem(item, ultraDeep, clonedContainerDef, postCloneAction);
        }
    }

    private <IV extends PrismValue,ID extends ItemDefinition, I extends Item<IV,ID>> void deepCloneDefinitionItem(I item, boolean ultraDeep, PrismContainerDefinition<C> clonedContainerDef, Consumer<ItemDefinition> postCloneAction) {
        PrismContainerDefinition<C> oldContainerDef = getDefinition();
        ItemName itemName = item.getElementName();
        ID oldItemDefFromContainer = oldContainerDef.findLocalItemDefinition(itemName);
        ID oldItemDef = item.getDefinition();
        ID clonedItemDef;
        if (oldItemDef == null || oldItemDefFromContainer == oldItemDef) {
            clonedItemDef = clonedContainerDef.findItemDefinition(itemName);
        } else {
            //noinspection unchecked
            clonedItemDef = (ID) oldItemDef.deepClone(ultraDeep, postCloneAction);
        }
        //noinspection unchecked
        ((ItemImpl<?, ID>) item).propagateDeepCloneDefinition(ultraDeep, clonedItemDef, postCloneAction);        // propagate to items in values
        item.setDefinition(clonedItemDef);                                    // sets CTD in values only if null!
    }

    @Override
    public boolean equals(PrismValue other, @NotNull ParameterizedEquivalenceStrategy strategy) {
        return other instanceof PrismContainerValue<?> && equals((PrismContainerValue<?>) other, strategy);
    }

    private boolean equals(PrismContainerValue<?> other, ParameterizedEquivalenceStrategy strategy) {
        if (!super.equals(other, strategy)) {
            return false;
        }
        if (strategy.isConsideringContainerIds()) {
            if (!Objects.equals(id, other.getId())) {
                return false;
            }
        } else if (strategy.isConsideringDifferentContainerIds()) {
            if (PrismValueUtil.differentIds(this, other)) {
                return false;
            }
        }
        if (!this.equalsItems((PrismContainerValue<C>) other, strategy)) {
            return false;
        }
        return true;
    }

    private boolean equalsItems(PrismContainerValue<C> other, ParameterizedEquivalenceStrategy strategy) {
        Collection<? extends ItemDelta<?,?>> deltas = new ArrayList<>();
        // The EMPTY_PATH is a lie. We don't really care if the returned deltas have correct path or not
        // we only care whether some deltas are returned or not.
        diffItems(this, other, deltas, strategy);
        return deltas.isEmpty();
    }

    public boolean equivalent(PrismContainerValue<?> other) {
        return equals(other, EquivalenceStrategy.REAL_VALUE);
    }

    // TODO consider taking equivalence strategy into account
    @Override
    public int hashCode(ParameterizedEquivalenceStrategy strategy) {
        final int prime = 31;
        int result = super.hashCode(strategy);
        // Do not include id. containers with non-null id and null id may still be considered equivalent
        // We also need to make sure that container valus that contain only metadata will produce zero hashcode
        // so it will not ruin hashcodes of parent containers
        int itemsHash = 0;
        itemsHash = MiscUtil.unorderedCollectionHashcode(items.values(), item -> !item.isOperational());
        if (itemsHash != 0) {
            result = prime * result + itemsHash;
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PCV(");
        sb.append(getId());
        sb.append("):");
        sb.append(getItems());
        return sb.toString();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        boolean wasIndent = false;
        if (DebugUtil.isDetailedDebugDump()) {
            DebugUtil.indentDebugDump(sb, indent);
            wasIndent = true;
            detailedDebugDumpStart(sb);
        }
        boolean multivalue = true;
        PrismContainerable<C> parent = getParent();
        if (parent != null && parent.getDefinition() != null) {
            multivalue = parent.getDefinition().isMultiValue();
        }
        Long id = getId();
        if (multivalue || id != null || DebugUtil.isDetailedDebugDump()) {
            if (!wasIndent) {
                DebugUtil.indentDebugDump(sb, indent);
                wasIndent = true;
            }
            debugDumpIdentifiers(sb);
        }
        appendOriginDump(sb);
        Collection<Item<?,?>> items = getItems();
        if (items.isEmpty()) {
            if (wasIndent) {
                sb.append("\n");
            }
            DebugUtil.indentDebugDump(sb, indent + 1);
            sb.append("(no items)");
        } else {
            Iterator<Item<?,?>> i = getItems().iterator();
            if (wasIndent && i.hasNext()) {
                sb.append("\n");
            }
            while (i.hasNext()) {
                Item<?,?> item = i.next();
                sb.append(item.debugDump(indent + 1));
                if (i.hasNext()) {
                    sb.append("\n");
                }
            }
        }
        return sb.toString();
    }

    protected void debugDumpIdentifiers(StringBuilder sb) {
        sb.append("id=").append(PrettyPrinter.prettyPrint(getId()));
    }

    protected void detailedDebugDumpStart(StringBuilder sb) {
        sb.append("PCV").append(": ");
    }

    @Override
    public String toHumanReadableString() {
        return "id="+id+": "+items.size()+" items";
    }

    @Override
    public QName getTypeName() {
        return getComplexTypeDefinition() != null ? getComplexTypeDefinition().getTypeName() : null;
    }

    @Nullable
    public ComplexTypeDefinition getComplexTypeDefinition() {
        if (complexTypeDefinition == null) {
            complexTypeDefinition = determineComplexTypeDefinition();
        }
        return complexTypeDefinition;
    }

    // will correctly work only if argument is not null (otherwise the CTD will be determined on next call to getCTD)
    void replaceComplexTypeDefinition(ComplexTypeDefinition complexTypeDefinition) {
//        if (this.complexTypeDefinition != null && complexTypeDefinition != null && !this.complexTypeDefinition.getTypeName().equals(complexTypeDefinition.getTypeName())) {
//            System.out.println("Dangerous!");
//        }
        this.complexTypeDefinition = complexTypeDefinition;
    }


    private ComplexTypeDefinition determineComplexTypeDefinition() {
        PrismContainerable<C> parent = getParent();
        ComplexTypeDefinition parentCTD = parent != null && parent.getDefinition() != null ?
                parent.getDefinition().getComplexTypeDefinition() : null;
        if (containerable == null) {
            return parentCTD;
        }
        if (prismContext == null) {
            // check if parentCTD matches containerable
            if (parentCTD != null && containerable.getClass().equals(parentCTD.getCompileTimeClass())) {
                return parentCTD;
            } else {
                //throw new IllegalStateException("Cannot determine complexTypeDefinition for PrismContainerValue because prismContext is missing; PCV = " + this);
                return null;
            }
        }
        ComplexTypeDefinition def = prismContext.getSchemaRegistry().findComplexTypeDefinitionByCompileTimeClass(containerable.getClass());
        return def;        // may be null at this place
    }

    public static <T extends Containerable> List<PrismContainerValue<T>> toPcvList(List<T> beans) {
        List<PrismContainerValue<T>> rv = new ArrayList<>(beans.size());
        for (T bean : beans) {
            rv.add(bean.asPrismContainerValue());
        }
        return rv;
    }

    @Override
    public void performFreeze() {
        // Before freezing this PCV we should initialize it (if needed).
        // We assume that this object is NOT shared at this moment.
        if (getComplexTypeDefinition() != null && getComplexTypeDefinition().getCompileTimeClass() != null) {
            asContainerable();
        } else {
            // Calling asContainerable does not make sense anyway.
        }

        // And now let's freeze it; from the bottom up.
        for (Item item : items.values()) {
            item.freeze();
        }
        super.performFreeze();
    }

    @Override
    public Class<?> getRealClass() {
        if (containerable != null) {
            return containerable.getClass();
        }
        return resolveClass(null);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public <T> T getRealValue() {
        return (T) asContainerable();
    }

    /**
     * Returns a single-valued container (with a single-valued definition) holding just this value.
     * @param itemName Item name for newly-created container.
     */
    public PrismContainer<C> asSingleValuedContainer(@NotNull QName itemName) throws SchemaException {
        PrismContext prismContext = getPrismContext();
        Validate.notNull(prismContext, "Prism context is null");

        PrismContainerDefinitionImpl<C> definition = new PrismContainerDefinitionImpl<>(itemName,
                getComplexTypeDefinition(), prismContext);
        definition.setMaxOccurs(1);

        PrismContainer<C> pc = definition.instantiate();
        pc.add(clone());
        return pc;
    }

    // EXPERIMENTAL. TODO write some tests
    // BEWARE, it expects that definitions for items are present. Otherwise definition-less single valued items will get overwritten.
    @Experimental
    @SuppressWarnings("unchecked")
    public void mergeContent(@NotNull PrismContainerValue<?> other, @NotNull List<QName> overwrite) throws SchemaException {
        List<ItemName> remainingToOverwrite = overwrite.stream().map(ItemName::fromQName).collect(Collectors.toList());
        for (Item<?, ?> otherItem : other.getItems()) {
            Item<?, ?> existingItem = findItem(otherItem.getElementName());
            if (QNameUtil.remove(remainingToOverwrite, otherItem.getElementName())
                    || existingItem != null && existingItem.isSingleValue()) {
                remove(existingItem);
            }
            merge(otherItem.clone());
        }
        remainingToOverwrite.forEach(name -> removeItem(name, Item.class));
    }

    @Override
    public PrismContainerValue<?> getRootValue() {
        return (PrismContainerValue) super.getRootValue();
    }

    public static <C extends Containerable> List<PrismContainerValue<C>> asPrismContainerValues(List<C> containerables) {
        return containerables.stream().map(c -> (PrismContainerValue<C>) c.asPrismContainerValue()).collect(Collectors.toList());
    }

    public static <C extends Containerable> List<C> asContainerables(List<PrismContainerValue<C>> pcvs) {
        return pcvs.stream().map(c -> c.asContainerable()).collect(Collectors.toList());
    }

    public static <C extends Containerable> Collection<C> asContainerables(Collection<PrismContainerValue<C>> pcvs) {
        return pcvs.stream().map(c -> c.asContainerable()).collect(Collectors.toList());
    }

    /**
     * Set origin type to all values and subvalues
     */
    public void setOriginTypeRecursive(final OriginType originType) {
        accept((visitable) -> {
            if (visitable instanceof PrismValue) {
                ((PrismValue)visitable).setOriginType(originType);
            }
        });
    }

    // TODO optimize a bit + test thoroughly
    public void keepPaths(List<? extends ItemPath> keep) throws SchemaException {
        Collection<QName> itemNames = getItemNames();
        for (QName itemName : itemNames) {
            Item<?, ?> item = findItemByQName(itemName);
            ItemPath itemPath = item.getPath().removeIds();
            if (!ItemPathCollectionsUtil.containsSuperpathOrEquivalent(keep, itemPath)
                && !ItemPathCollectionsUtil.containsSubpathOrEquivalent(keep, itemPath)) {
                removeItem(ItemName.fromQName(itemName), Item.class);
            } else {
                if (item instanceof PrismContainer) {
                    for (PrismContainerValue<?> v : ((PrismContainer<?>) item).getValues()) {
                        v.keepPaths(keep);
                    }
                } else {
                    // TODO some additional checks here (e.g. when trying to keep 'name/xyz' - this is illegal)
                }
            }

        }
    }

    // TODO optimize a bit + test thoroughly
    public void removePaths(List<? extends ItemPath> remove) throws SchemaException {
        Collection<QName> itemNames = getItemNames();
        for (QName itemName : itemNames) {
            Item<?, ?> item = findItemByQName(itemName);
            ItemPath itemPath = item.getPath().removeIds();
            if (ItemPathCollectionsUtil.containsEquivalent(remove, itemPath)) {
                removeItem(ItemName.fromQName(itemName), Item.class);
            } else if (ItemPathCollectionsUtil.containsSuperpath(remove, itemPath)) {
                if (item instanceof PrismContainer) {
                    for (PrismContainerValue<?> v : ((PrismContainer<?>) item).getValues()) {
                        v.removePaths(remove);
                    }
                }
            }
        }
    }

    // Removes all unused definitions, in order to conserve heap. Assumes that the definition is not shared. Use with care!
    void trimItemsDefinitionsTrees(Collection<? extends ItemPath> alwaysKeep) {
        // to play safe, we won't touch PCV-specific complexTypeDefinition
        for (Item<?, ?> item : items.values()) {
            if (item instanceof PrismContainer) {
                Collection<ItemPath> alwaysKeepInSub = ItemPathCollectionsUtil.remainder(CollectionUtils.emptyIfNull(alwaysKeep),
                        item.getElementName(), false);
                ((PrismContainer<?>) item).trimDefinitionTree(alwaysKeepInSub);
            }
        }
    }

    @NotNull
    @Override
    public Collection<PrismValue> getAllValues(ItemPath path) {
        if (path.isEmpty()) {
            return singleton(this);
        }
        Item<PrismValue, ItemDefinition> item = findItem(path.firstToName());
        if (item == null) {
            return emptySet();
        }
        ItemPath rest = path.rest();
        List<PrismValue> rv = new ArrayList<>();
        for (PrismValue prismValue : item.getValues()) {
            rv.addAll(prismValue.getAllValues(rest));
        }
        return rv;
    }

    public void removeItems(List<? extends ItemPath> itemsToRemove) {
        for (ItemPath itemToRemove : itemsToRemove) {
            Item item = findItem(itemToRemove);        // reduce to "removeItem" after fixing that method implementation
            if (item != null) {
                removeItem(item.getPath(), Item.class);
            }
        }
    }

    public void removeOperationalItems() {
        accept(visitable -> {
            if (visitable instanceof Item) {
                Item item = ((Item) visitable);
                if (item.getDefinition() != null && item.getDefinition().isOperational()) {
                    PrismValue parent = item.getParent();
                    if (parent != null) {    // should be the case
                        ((PrismContainerValue) parent).remove(item);
                    }
                }
            }
        });
    }
}
