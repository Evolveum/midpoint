/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import javax.xml.namespace.QName;

import com.evolveum.axiom.api.schema.AxiomItemDefinition;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.QNameUtil;

import org.jetbrains.annotations.NotNull;

/**
 * Abstract item definition in the schema.
 *
 * This is supposed to be a superclass for all item definitions. Items are things
 * that can appear in property containers, which generally means only a property
 * and property container itself. Therefore this is in fact superclass for those
 * two definitions.
 *
 * The definitions represent data structures of the schema. Therefore instances
 * of Java objects from this class represent specific <em>definitions</em> from
 * the schema, not specific properties or objects. E.g the definitions does not
 * have any value.
 *
 * To transform definition to a real property or object use the explicit
 * instantiate() methods provided in the definition classes. E.g. the
 * instantiate() method will create instance of Property using appropriate
 * PropertyDefinition.
 *
 * The convenience methods in Schema are using this abstract class to find
 * appropriate definitions easily.
 *
 * @author Radovan Semancik
 *
 */
public abstract class ItemDefinitionImpl<I extends Item> extends DefinitionImpl implements MutableItemDefinition<I>, ItemDefinitionTestAccess {
    private static final long serialVersionUID = -2643332934312107274L;

    @NotNull protected ItemName itemName;
    private int minOccurs = 1;
    private int maxOccurs = 1;
    private boolean operational = false;
    private boolean dynamic;
    private boolean canAdd = true;
    private boolean canRead = true;
    private boolean canModify = true;
    private boolean inherited;
    protected QName substitutionHead;
    protected boolean heterogeneousListItem;
    private PrismReferenceValue valueEnumerationRef;

    private AxiomItemDefinition axiomDef;

    private boolean indexOnly = false;

    // TODO: annotations

    /**
     * The constructors should be used only occasionally (if used at all).
     * Use the factory methods in the ResourceObjectDefinition instead.
     *
     * @param itemName definition name (element Name)
     * @param typeName type name (XSD complex or simple type)
     */
    ItemDefinitionImpl(@NotNull QName itemName, @NotNull QName typeName, @NotNull PrismContext prismContext) {
        super(typeName, prismContext);
        this.itemName = ItemName.fromQName(itemName);     // todo
    }

    /**
     * Returns name of the defined entity.
     *
     * The name is a name of the entity instance if it is fixed by the schema.
     * E.g. it may be a name of the property in the container that cannot be
     * changed.
     *
     * The name corresponds to the XML element name in the XML representation of
     * the schema. It does NOT correspond to a XSD type name.
     *
     * Name is optional. If name is not set the null value is returned. If name is
     * not set the type is "abstract", does not correspond to the element.
     *
     * @return the name name of the entity or null.
     */
    @Override
    @NotNull
    public ItemName getItemName() {
        return itemName;
    }

    public void setItemName(@NotNull QName name) {
        checkMutable();
        this.itemName = ItemName.fromQName(name); // todo
    }

    @Override
    public String getNamespace() {
        return getItemName().getNamespaceURI();
    }

    /**
     * Return the number of minimal value occurrences.
     *
     * @return the minOccurs
     */
    @Override
    public int getMinOccurs() {
        return minOccurs;
    }

    public void setMinOccurs(int minOccurs) {
        checkMutable();
        this.minOccurs = minOccurs;
    }

    /**
     * Return the number of maximal value occurrences.
     * <p>
     * Any negative number means "unbounded".
     *
     * @return the maxOccurs
     */
    @Override
    public int getMaxOccurs() {
        return maxOccurs;
    }

    public void setMaxOccurs(int maxOccurs) {
        checkMutable();
        this.maxOccurs = maxOccurs;
    }

    /**
     * Returns true if property is mandatory.
     *
     * @return true if property is mandatory.
     */
    @Override
    public boolean isMandatory() {
        return getMinOccurs() > 0;
    }

    /**
     * Returns true if property is optional.
     *
     * @return true if property is optional.
     */
    @Override
    public boolean isOptional() {
        return getMinOccurs() == 0;
    }

    @Override
    public boolean isOperational() {
        return operational;
    }

    @Override
    public void setOperational(boolean operational) {
        checkMutable();
        this.operational = operational;
    }

    @Override
    public boolean isDynamic() {
        return dynamic;
    }

    public void setDynamic(boolean dynamic) {
        checkMutable();
        this.dynamic = dynamic;
    }

    /**
     * Returns true if the property can be read. I.e. if it is returned in objects
     * retrieved from "get", "search" and similar operations.
     */
    @Override
    public boolean canRead() {
        return canRead;
    }

    /**
     * Returns true if the item can be modified. I.e. if it can be changed
     * during a modification of existing object.
     */
    @Override
    public boolean canModify() {
        return canModify;
    }

    /**
     *
     */
    public void setReadOnly() {
        checkMutable();
        canAdd = false;
        canRead = true;
        canModify = false;
    }

    @Override
    public void setCanRead(boolean read) {
        checkMutable();
        this.canRead = read;
    }

    @Override
    public void setCanModify(boolean modify) {
        checkMutable();
        this.canModify = modify;
    }

    @Override
    public void setCanAdd(boolean add) {
        checkMutable();
        this.canAdd = add;
    }

    public void setAxiomDefinition(AxiomItemDefinition def) {
        checkMutable();
        this.axiomDef = def;
    }

    public Optional<AxiomItemDefinition> axiomDefinition() {
        return Optional.ofNullable(axiomDef);
    }

    /**
     * Returns true if the item can be added. I.e. if it can be present
     * in the object when a new object is created.
     */
    @Override
    public boolean canAdd() {
        return canAdd;
    }

    @Override
    public QName getSubstitutionHead() {
        return substitutionHead;
    }

    public void setSubstitutionHead(QName substitutionHead) {
        checkMutable();
        this.substitutionHead = substitutionHead;
    }

    @Override
    public boolean isHeterogeneousListItem() {
        return heterogeneousListItem;
    }

    public void setHeterogeneousListItem(boolean heterogeneousListItem) {
        checkMutable();
        this.heterogeneousListItem = heterogeneousListItem;
    }

    /**
     * Reference to an object that directly or indirectly represents possible values for
     * this item. We do not define here what exactly the object has to be. It can be a lookup
     * table, script that dynamically produces the values or anything similar.
     * The object must produce the values of the correct type for this item otherwise an
     * error occurs.
     */
    @Override
    public PrismReferenceValue getValueEnumerationRef() {
        return valueEnumerationRef;
    }

    @Override
    public void setValueEnumerationRef(PrismReferenceValue valueEnumerationRef) {
        checkMutable();
        this.valueEnumerationRef = valueEnumerationRef;
    }

    @Override
    public boolean isValidFor(QName elementQName, Class<? extends ItemDefinition> clazz) {
        return isValidFor(elementQName, clazz, false);
    }

    @Override
    public boolean isValidFor(@NotNull QName elementQName, @NotNull Class<? extends ItemDefinition> clazz,
            boolean caseInsensitive) {
        return clazz.isAssignableFrom(this.getClass())
                && QNameUtil.match(elementQName, getItemName(), caseInsensitive);
    }

    @Override
    public void adoptElementDefinitionFrom(ItemDefinition otherDef) {
        if (otherDef == null) {
            return;
        }
        setItemName(otherDef.getItemName());
        setMinOccurs(otherDef.getMinOccurs());
        setMaxOccurs(otherDef.getMaxOccurs());
    }

    @Override
    public <T extends ItemDefinition> T findItemDefinition(@NotNull ItemPath path, @NotNull Class<T> clazz) {
        if (path.isEmpty()) {
            if (clazz.isAssignableFrom(this.getClass())) {
                return (T) this;
            } else {
                throw new IllegalArgumentException("Looking for definition of class " + clazz + " but found " + this);
            }
        } else {
            return null;
        }
    }

    @Override
    public boolean canBeDefinitionOf(I item) {
        if (item == null) {
            return false;
        }
        ItemDefinition<?> itemDefinition = item.getDefinition();
        if (itemDefinition != null) {
            if (!QNameUtil.match(getTypeName(), itemDefinition.getTypeName())) {
                return false;
            }
            // TODO: compare entire definition? Probably not.
            return true;
        }
        return true;
    }

    @NotNull
    @Override
    public abstract ItemDefinition clone();

    protected void copyDefinitionData(ItemDefinitionImpl<I> clone) {
        super.copyDefinitionData(clone);
        clone.itemName = this.itemName;
        clone.minOccurs = this.minOccurs;
        clone.maxOccurs = this.maxOccurs;
        clone.dynamic = this.dynamic;
        clone.canAdd = this.canAdd;
        clone.canRead = this.canRead;
        clone.canModify = this.canModify;
        clone.operational = this.operational;
        clone.valueEnumerationRef = this.valueEnumerationRef;
        clone.indexOnly = this.indexOnly;
    }

    /**
     * Make a deep clone, cloning all the sub-items and definitions.
     *
     * @param ultraDeep if set to true then even the objects that were same instance in the original will be
     *                  cloned as separate instances in the clone.
     *
     */
    @Override
    public ItemDefinition<I> deepClone(boolean ultraDeep, Consumer<ItemDefinition> postCloneAction) {
        if (ultraDeep) {
            return deepClone(null, new HashMap<>(), postCloneAction);
        } else {
            return deepClone(new HashMap<>(), new HashMap<>(), postCloneAction);
        }
    }

    @Override
    public ItemDefinition<I> deepClone(Map<QName, ComplexTypeDefinition> ctdMap, Map<QName, ComplexTypeDefinition> onThisPath, Consumer<ItemDefinition> postCloneAction) {
        return clone();
    }

    @Override
    public void revive(PrismContext prismContext) {
        if (this.prismContext != null) {
            return;
        }
        this.prismContext = prismContext;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ItemDefinitionImpl<?> that = (ItemDefinitionImpl<?>) o;
        return minOccurs == that.minOccurs && maxOccurs == that.maxOccurs && operational == that.operational && dynamic == that.dynamic && canAdd == that.canAdd && canRead == that.canRead && canModify == that.canModify && inherited == that.inherited && heterogeneousListItem == that.heterogeneousListItem && indexOnly == that.indexOnly && itemName.equals(that.itemName) && Objects.equals(substitutionHead, that.substitutionHead) && Objects.equals(valueEnumerationRef, that.valueEnumerationRef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), itemName, minOccurs, maxOccurs, operational, dynamic, canAdd, canRead, canModify, inherited, substitutionHead, heterogeneousListItem, valueEnumerationRef, indexOnly);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getDebugDumpClassName());
        sb.append(getMutabilityFlag());
        sb.append(":");
        sb.append(PrettyPrinter.prettyPrint(getItemName()));
        sb.append(" ");
        debugDumpShortToString(sb);
        return sb.toString();
    }

    /**
     * Used in debugDumping items. Does not need to have name in it as item already has it. Does not need
     * to have class as that is just too much info that is almost anytime pretty obvious anyway.
     */
    @Override
    public void debugDumpShortToString(StringBuilder sb) {
        sb.append(PrettyPrinter.prettyPrint(getTypeName()));
        debugMultiplicity(sb);
        debugFlags(sb);
    }

    private void debugMultiplicity(StringBuilder sb) {
        sb.append("[");
        sb.append(getMinOccurs());
        sb.append(",");
        sb.append(getMaxOccurs());
        sb.append("]");
    }

    public String debugMultiplicity() {
        StringBuilder sb = new StringBuilder();
        debugMultiplicity(sb);
        return sb.toString();
    }

    private void debugFlags(StringBuilder sb) {
        if (isIgnored()) {
            sb.append(",ignored");
        }
        if (isDynamic()) {
            sb.append(",dyn");
        }
        if (isElaborate()) {
            sb.append(",elaborate");
        }
        extendToString(sb);
    }

    public String debugFlags() {
        StringBuilder sb = new StringBuilder();
        debugFlags(sb);
        // This starts with a collon, we do not want it here
        if (sb.length() > 0) {
            sb.deleteCharAt(0);
        }
        return sb.toString();
    }

    protected void extendToString(StringBuilder sb) {
        sb.append(",");
        if (canRead()) {
            sb.append("R");
        } else {
            sb.append("-");
        }
        if (canAdd()) {
            sb.append("A");
        } else {
            sb.append("-");
        }
        if (canModify()) {
            sb.append("M");
        } else {
            sb.append("-");
        }
        if (isRuntimeSchema()) {
            sb.append(",runtime");
        }
        if (isOperational()) {
            sb.append(",oper");
        }
    }

    @Override
    public boolean isInherited() {
        return inherited;
    }

    @Override
    public void setInherited(boolean inherited) {
        checkMutable();
        this.inherited = inherited;
    }

    @Override
    public boolean isIndexOnly() {
        return indexOnly;
    }

    @Override
    public void setIndexOnly(boolean indexOnly) {
        checkMutable();
        this.indexOnly = indexOnly;
    }

    @Override
    public void replaceName(ItemName newName) {
        itemName = newName;
    }
}
