/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.delta.builder;

import java.util.*;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.delta.builder.S_ItemEntry;
import com.evolveum.midpoint.prism.delta.builder.S_MaybeDelete;
import com.evolveum.midpoint.prism.delta.builder.S_ValuesEntry;
import com.evolveum.midpoint.prism.impl.PrismPropertyValueImpl;
import com.evolveum.midpoint.prism.impl.delta.ContainerDeltaImpl;
import com.evolveum.midpoint.prism.impl.delta.PropertyDeltaImpl;
import com.evolveum.midpoint.prism.impl.delta.ReferenceDeltaImpl;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Grammar:
 * <p>
 * ObjectDelta ::= (ItemDelta)* ( 'OBJECT-DELTA(oid)' | 'ITEM-DELTA' | 'ITEM-DELTAS' )
 * <p>
 * ItemDelta ::= 'ITEM(...)' ( ( 'ADD-VALUES(...)' 'DELETE-VALUES(...)'? ) | 'DELETE-VALUES(...)' | 'REPLACE-VALUES(...)' )
 * <p>
 * EXPERIMENTAL IMPLEMENTATION.
 */
@Experimental
public class DeltaBuilder<T extends Containerable> implements S_ItemEntry, S_MaybeDelete, S_ValuesEntry {

    private final Class<T> objectClass;
    private final ComplexTypeDefinition containerCTD;
    private final PrismContext prismContext;

    // BEWARE - although these are final, their content may (and does) vary. Not much clean.
    private final List<ItemDelta<?, ?>> deltas;
    private final ItemDelta currentDelta;

    public DeltaBuilder(Class<T> objectClass, PrismContext prismContext) throws SchemaException {
        this.objectClass = objectClass;
        this.prismContext = prismContext;
        containerCTD = prismContext.getSchemaRegistry().findComplexTypeDefinitionByCompileTimeClass(this.objectClass);
        if (containerCTD == null) {
            throw new SchemaException("Couldn't find definition for complex type " + this.objectClass);
        }
        deltas = new ArrayList<>();
        currentDelta = null;
    }

    public DeltaBuilder(Class<T> objectClass, ComplexTypeDefinition containerCTD, PrismContext prismContext, List<ItemDelta<?, ?>> deltas, ItemDelta currentDelta) {
        this.objectClass = objectClass;
        this.containerCTD = containerCTD;
        this.prismContext = prismContext;
        this.deltas = deltas;
        this.currentDelta = currentDelta;
    }

    public Class<T> getObjectClass() {
        return objectClass;
    }

    public PrismContext getPrismContext() {
        return prismContext;
    }

    public static <C extends Containerable> S_ItemEntry deltaFor(Class<C> objectClass, PrismContext prismContext) throws SchemaException {
        return new DeltaBuilder<>(objectClass, prismContext);
    }

    @Override
    public S_ValuesEntry item(QName... names) {
        return item(ItemPath.create(names));
    }

    @Override
    public S_ValuesEntry item(Object... namesOrIds) {
        return item(ItemPath.create(namesOrIds));
    }

    @Override
    public S_ValuesEntry item(ItemPath path) {
        ItemDefinition definition = containerCTD.findItemDefinition(path);
        if (definition == null) {
            throw new IllegalArgumentException("Undefined or dynamic path: " + path + " in: " + containerCTD);
        }
        return item(path, definition);
    }

    @Override
    public S_ValuesEntry item(ItemPath path, ItemDefinition definition) {
        ItemDelta newDelta;
        if (definition instanceof PrismPropertyDefinition) {
            newDelta = new PropertyDeltaImpl(path, (PrismPropertyDefinition) definition, prismContext);
        } else if (definition instanceof PrismContainerDefinition) {
            newDelta = new ContainerDeltaImpl(path, (PrismContainerDefinition) definition, prismContext);
        } else if (definition instanceof PrismReferenceDefinition) {
            newDelta = new ReferenceDeltaImpl(path, (PrismReferenceDefinition) definition, prismContext);
        } else {
            throw new IllegalStateException("Unsupported definition type: " + definition);
        }
        List<ItemDelta<?, ?>> newDeltas = deltas;
        if (currentDelta != null) {
            newDeltas.add(currentDelta);
        }
        return new DeltaBuilder(objectClass, containerCTD, prismContext, newDeltas, newDelta);
    }

    @Override
    public S_ValuesEntry property(QName... names) {
        return property(ItemPath.create(names));
    }

    @Override
    public S_ValuesEntry property(Object... namesOrIds) {
        return property(ItemPath.create(namesOrIds));
    }

    @Override
    public S_ValuesEntry property(ItemPath path) {
        PrismPropertyDefinition<T> definition = containerCTD.findPropertyDefinition(path);
        return property(path, definition);
    }

    @Override
    public <T> S_ValuesEntry property(ItemPath path, PrismPropertyDefinition<T> definition) {
        PropertyDelta<Object> newDelta = new PropertyDeltaImpl(path, definition, prismContext);
        List<ItemDelta<?, ?>> newDeltas = deltas;
        if (currentDelta != null) {
            newDeltas.add(currentDelta);
        }
        return new DeltaBuilder(objectClass, containerCTD, prismContext, newDeltas, newDelta);
    }

    // TODO fix this after ObjectDelta is changed to accept Containerable
    @Override
    public ObjectDelta asObjectDelta(String oid) {
        return prismContext.deltaFactory().object().createModifyDelta(oid, getAllDeltas(), (Class) objectClass
        );
    }

    @Override
    public List<ObjectDelta<?>> asObjectDeltas(String oid) {
        return Collections.<ObjectDelta<?>>singletonList(
                prismContext.deltaFactory().object().createModifyDelta(oid, getAllDeltas(), (Class) objectClass
                ));
    }

    @Override
    public ItemDelta asItemDelta() {
        List<ItemDelta<?, ?>> allDeltas = getAllDeltas();
        if (allDeltas.size() > 1) {
            throw new IllegalStateException("Too many deltas to fit into item delta: " + allDeltas.size());
        } else if (allDeltas.size() == 1) {
            return allDeltas.get(0);
        } else {
            return null;
        }
    }

    @Override
    public List<ItemDelta<?, ?>> asItemDeltas() {
        return getAllDeltas();
    }

    private List<ItemDelta<?, ?>> getAllDeltas() {
        if (currentDelta != null) {
            deltas.add(currentDelta);
        }
        return deltas;
    }

    @Override
    public S_MaybeDelete add(Object... realValues) {
        return addRealValues(Arrays.asList(realValues));
    }

    @Override
    public S_MaybeDelete addRealValues(Collection<?> realValues) {
        for (Object v : realValues) {
            if (v != null) {
                currentDelta.addValueToAdd(toPrismValue(currentDelta, v));
            }
        }
        return this;
    }

    @Override
    public S_MaybeDelete add(PrismValue... values) {
        for (PrismValue v : values) {
            if (v != null) {
                currentDelta.addValueToAdd(v);
            }
        }
        return this;
    }

    @Override
    public S_MaybeDelete add(Collection<? extends PrismValue> values) {
        for (PrismValue v : values) {
            if (v != null) {
                currentDelta.addValueToAdd(v);
            }
        }
        return this;
    }

    @Override
    public S_ValuesEntry old(Object... realValues) {
        return oldRealValues(Arrays.asList(realValues));
    }

    @Override
    public S_ValuesEntry oldRealValues(Collection<?> realValues) {
        for (Object v : realValues) {
            if (v != null) {
                currentDelta.addEstimatedOldValue(toPrismValue(currentDelta, v));
            }
        }
        return this;
    }

    @Override
    public <T> S_ValuesEntry oldRealValue(T realValue) {
        if (realValue != null) {
            currentDelta.addEstimatedOldValue(toPrismValue(currentDelta, realValue));
        }
        return this;
    }

    @Override
    public S_ValuesEntry old(PrismValue... values) {
        for (PrismValue v : values) {
            if (v != null) {
                currentDelta.addEstimatedOldValue(v);
            }
        }
        return this;
    }

    @Override
    public S_ValuesEntry old(Collection<? extends PrismValue> values) {
        for (PrismValue v : values) {
            if (v != null) {
                currentDelta.addEstimatedOldValue(v);
            }
        }
        return this;
    }

    @Override
    public S_ItemEntry delete(Object... realValues) {
        return deleteRealValues(Arrays.asList(realValues));
    }

    @Override
    public S_ItemEntry deleteRealValues(Collection<?> realValues) {
        for (Object v : realValues) {
            if (v != null) {
                currentDelta.addValueToDelete(toPrismValue(currentDelta, v));
            }
        }
        return this;
    }

//    protected void checkNullMisuse(Object[] realValues) {
//        if (realValues.length == 1 && realValues[0] == null) {
//            throw new IllegalArgumentException("NULL value should be represented as no value, not as 'null'");
//        }
//    }

    @Override
    public S_ItemEntry delete(PrismValue... values) {
        for (PrismValue v : values) {
            if (v != null) {
                currentDelta.addValueToDelete(v);
            }
        }
        return this;
    }

    @Override
    public S_ItemEntry delete(Collection<? extends PrismValue> values) {
        for (PrismValue v : values) {
            if (v != null) {
                currentDelta.addValueToDelete(v);
            }
        }
        return this;
    }

    @Override
    public S_ItemEntry replace(Object... realValues) {
        return replaceRealValues(Arrays.asList(realValues));
    }

    @Override
    public S_ItemEntry replaceRealValues(Collection<?> realValues) {
        List<PrismValue> prismValues = new ArrayList<>();
        for (Object v : realValues) {
            if (v != null) {
                prismValues.add(toPrismValue(currentDelta, v));
            }
        }
        currentDelta.setValuesToReplace(prismValues);
        return this;
    }

    @Override
    public S_ItemEntry replace(Collection<? extends PrismValue> values) {
        List<PrismValue> prismValues = new ArrayList<>();
        for (PrismValue v : values) {
            if (v != null) {
                prismValues.add(v);
            }
        }
        currentDelta.setValuesToReplace(prismValues);
        return this;
    }

    @Override
    public S_ItemEntry replace(PrismValue... values) {
        List<PrismValue> prismValues = new ArrayList<>();
        for (PrismValue v : values) {
            if (v != null) {
                prismValues.add(v);
            }
        }
        currentDelta.setValuesToReplace(prismValues);
        return this;
    }

    @Override
    public S_ItemEntry mod(PlusMinusZero plusMinusZero, Object... realValues) {
        return modRealValues(plusMinusZero, Arrays.asList(realValues));
    }

    @Override
    public S_ItemEntry modRealValues(PlusMinusZero plusMinusZero, Collection<?> realValues) {
        List<PrismValue> prismValues = new ArrayList<>();
        for (Object v : realValues) {
            if (v != null) {
                prismValues.add(toPrismValue(currentDelta, v));
            }
        }
        switch (plusMinusZero) {
            case PLUS:
                currentDelta.addValuesToAdd(prismValues);
                break;
            case MINUS:
                currentDelta.addValuesToDelete(prismValues);
                break;
            case ZERO:
                currentDelta.setValuesToReplace(prismValues);
                break;
        }
        return this;
    }

    @Override
    public S_ItemEntry mod(PlusMinusZero plusMinusZero, Collection<? extends PrismValue> values) {
        List<PrismValue> prismValues = new ArrayList<>();
        for (PrismValue v : values) {
            if (v != null) {
                prismValues.add(v);
            }
        }
        switch (plusMinusZero) {
            case PLUS:
                currentDelta.addValuesToAdd(prismValues);
                break;
            case MINUS:
                currentDelta.addValuesToDelete(prismValues);
                break;
            case ZERO:
                currentDelta.setValuesToReplace(prismValues);
                break;
        }
        return this;
    }

    @Override
    public S_ItemEntry mod(PlusMinusZero plusMinusZero, PrismValue... values) {
        List<PrismValue> prismValues = new ArrayList<>();
        for (PrismValue v : values) {
            if (v != null) {
                prismValues.add(v);
            }
        }
        switch (plusMinusZero) {
            case PLUS:
                currentDelta.addValuesToAdd(prismValues);
                break;
            case MINUS:
                currentDelta.addValuesToDelete(prismValues);
                break;
            case ZERO:
                currentDelta.setValuesToReplace(prismValues);
                break;
        }
        return this;
    }

    private PrismValue toPrismValue(ItemDelta<?, ?> currentDelta, @NotNull Object v) {
        if (currentDelta instanceof PropertyDelta<?>) {
            return new PrismPropertyValueImpl<>(v);
        } else if (currentDelta instanceof ContainerDelta<?>) {
            return ((Containerable) v).asPrismContainerValue();
        } else if (currentDelta instanceof ReferenceDelta) {
            if (v instanceof Referencable) {
                return ((Referencable) v).asReferenceValue();
            } else {
                throw new IllegalStateException("Expected Referencable, got: " + v);
            }
        } else {
            throw new IllegalStateException("Unsupported delta type: " + currentDelta);
        }
    }

}
