/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.delta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.equivalence.ParameterizedEquivalenceStrategy;
import com.evolveum.midpoint.prism.path.InfraItemName;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.util.DebugUtil;

public class ContainerTreeDeltaValue<C extends Containerable>
        extends ItemTreeDeltaValue<PrismContainerValue<C>, ContainerTreeDelta<C>> {

    private List<ItemTreeDelta<?, ?, ?, ?>> deltas;

    private Long id;

    public ContainerTreeDeltaValue() {
        this(null);
    }

    public ContainerTreeDeltaValue(Long id) {
        this(id, null, null);
    }

    public ContainerTreeDeltaValue(PrismContainerValue<C> value, ModificationType modificationType) {
        this(null, value, modificationType);
    }

    public ContainerTreeDeltaValue(Long id, PrismContainerValue<C> value, ModificationType modificationType) {
        super(value, modificationType);

        this.id = id;
    }

    public int getSize() {
        return deltas == null ? 0: deltas.size();
    }

    @Override
    public List<Object> getNaturalKey() {
        PrismContainerValue<C> pcv = getValue();
        if (pcv != null && pcv.getParent() != null) {
            ItemDefinition def = pcv.getParent().getDefinition();
            if (def != null && def.getNaturalKeyInstance() != null) {
                // todo not very nice/clean, but it should work for now
                Collection<Item<?, ?>> constituents = def.getNaturalKeyInstance().getConstituents(pcv);
                return new ArrayList<>(constituents);
            }
        }

        return super.getNaturalKey();
    }

    public Long getId() {
        PrismContainerValue<C> value = getValue();

        return value != null ? value.getId() : id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<ItemTreeDelta<?, ?, ?, ?>> getDeltas() {
        if (deltas == null) {
            deltas = new ArrayList<>();
        }
        return deltas;
    }

    public void setDeltas(List<ItemTreeDelta<?, ?, ?, ?>> deltas) {
        this.deltas = deltas;
    }

    @Override
    protected String debugDumpShortName() {
        return "CTDV";
    }

    @Override
    protected void debugDumpIdentifiers(StringBuilder sb) {
        if (getId() != null) {
            sb.append(" id: ").append(getId());
        }
    }

    @Override
    protected void debugDumpChildren(StringBuilder sb, int indent) {
        DebugUtil.debugDump(sb, deltas, indent, false);
    }

    public <D extends ItemTreeDelta> D findItemDelta(ItemPath path, Class<D> deltaClass) {
        return findItemDelta(path, deltaClass, false);
    }

    public <D extends ItemTreeDelta> D findOrCreateItemDelta(ItemPath path, Class<D> deltaClass) {
        return findItemDelta(path, deltaClass, true);
    }

    public <D extends ItemTreeDelta> D findItemDelta(ItemPath path, Class<D> deltaClass, boolean createIfNotExists) {
        ItemName name = path.firstToNameOrNull();
        if (name == null) {
            throw new IllegalArgumentException("Attempt to get segment without a name from a container delta");
        }

        ItemTreeDelta<?, ?, ?, ?> delta = getDeltas().stream()
                .filter(deltaClass::isInstance)
                .filter(d -> {
                    ItemName itemName = name;
                    if (name instanceof InfraItemName infraName && InfraItemName.METADATA.equals(infraName)) {
                        itemName = SchemaConstantsGenerated.C_VALUE_METADATA;
                    }

                    return ItemPath.equivalent(itemName, ItemPath.create(d.getItemName()));
                })
                .findFirst()
                .orElse(null);

        if (delta == null) {
            if (!createIfNotExists) {
                return null;
            }

            delta = createItemTreeDelta(name);

            delta.setParent(this);
            getDeltas().add(delta);
        }

        ItemPath rest = path.rest();
        if (rest.isEmpty()) {
            if (!deltaClass.isAssignableFrom(delta.getClass())) {
                throw new IllegalArgumentException("Delta class " + delta.getClass() + " is not compatible with requested class " + deltaClass);
            }
            return (D) delta;
        }

        if (delta instanceof ContainerTreeDelta<?> ctd) {
            return ctd.findItemDelta(rest, deltaClass, createIfNotExists);
        } else {
            throw new IllegalArgumentException("Attempt to find or create delta for non-container item");
        }
    }

    private ItemTreeDelta createItemTreeDelta(ItemName name) {
        ItemDefinition def = getParent().getDefinition();
        ItemDefinition itemDefinition = def.findItemDefinition(name, ItemDefinition.class);

        ItemTreeDelta delta;
        if (itemDefinition instanceof PrismContainerDefinition<?> pcd) {
            delta = new ContainerTreeDelta<>(pcd);
        } else if (itemDefinition instanceof PrismPropertyDefinition<?> ppd) {
            delta = new PropertyTreeDelta<>(ppd);
        } else if (itemDefinition instanceof PrismReferenceDefinition prd) {
            delta = new ReferenceTreeDelta(prd);
        } else {
            throw new IllegalArgumentException("Can't create tree delta for item definition " + itemDefinition);
        }

        return delta;
    }

    @Override
    public ItemPath getPath() {
        Long id = getId();

        ItemTreeDelta parent = getParent();
        if (parent == null) {
            return id != null ? ItemPath.create(id) : null;
        }

        return id != null ? parent.getPath().append(id) : parent.getPath();
    }

    @Override
    public void accept(Visitor visitor) {
        super.accept(visitor);

        getDeltas().forEach(d -> d.accept(visitor));
    }

    @Override
    public List<Conflict> getConflictsWith(ItemTreeDeltaValue other, EquivalenceStrategy strategy) {
        List<Conflict> result = super.getConflictsWith(other, strategy);
        if (!result.isEmpty()) {
            return result;
        }

        result = new ArrayList<>();

        ContainerTreeDeltaValue<C> otherValue = (ContainerTreeDeltaValue<C>) other;
        for (ItemTreeDelta delta : getDeltas()) {
            ItemTreeDelta otherDelta = otherValue.findItemDelta(ItemPath.create(delta.getItemName()), delta.getClass());
            if (otherDelta == null) {
                continue;
            }

            Collection<Conflict> deltaConflicts = delta.getConflictsWith(otherDelta, strategy);
            result.addAll(deltaConflicts);
        }

        return result;
    }

    @Override
    protected boolean hasConflictWith(PrismContainerValue<C> otherValue) {
        if (otherValue == null) {
            return false;
        }

        // todo natural keys

        return Objects.equals(getId(), otherValue.getId());
    }

    @Override
    public <V extends ItemTreeDeltaValue> boolean match(V other, EquivalenceStrategy strategy) {
        if (!(other instanceof ContainerTreeDeltaValue<?> otherValue)) {
            return false;
        }

        if (Objects.equals(getId(), otherValue.getId())) {
            return true;
        }

        if (strategy instanceof ParameterizedEquivalenceStrategy pes && pes.isConsideringNaturalKeys()) {
            Objects.equals(getNaturalKey(), otherValue.getNaturalKey());
        }

        return false;
    }

    @Override
    public boolean containsModifications() {
        if (super.containsModifications()) {
            return true;
        }

        return getDeltas().stream().anyMatch(ItemTreeDelta::containsModifications);
    }

    @Override
    public Collection<? extends ItemDelta<?, ?>> getModifications(boolean ignoreItself) {
        Collection<? extends ItemDelta<?, ?>> modifications = super.getModifications(ignoreItself);

        getDeltas().forEach(delta -> modifications.addAll((Collection) delta.getModifications()));

        return modifications;
    }
}
