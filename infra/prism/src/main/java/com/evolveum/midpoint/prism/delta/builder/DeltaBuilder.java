/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.prism.delta.builder;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.*;

/**
 * Grammar:
 *
 *  ObjectDelta ::= (ItemDelta)* ( 'OBJECT-DELTA(oid)' | 'ITEM-DELTA' | 'ITEM-DELTAS' )
 *
 *  ItemDelta ::= 'ITEM(...)' ( ( 'ADD-VALUES(...)' 'DELETE-VALUES(...)'? ) | 'DELETE-VALUES(...)' | 'REPLACE-VALUES(...)' )
 *
 * EXPERIMENTAL IMPLEMENTATION.
 *
 * @author mederly
 */
public class DeltaBuilder<T extends Containerable> implements S_ItemEntry, S_MaybeDelete, S_ValuesEntry {

    final private Class<T> objectClass;
    final private ComplexTypeDefinition containerCTD;
    final private PrismContext prismContext;

    // BEWARE - although these are final, their content may (and does) vary. Not much clean.
    final List<ItemDelta<?,?>> deltas;
    final ItemDelta currentDelta;

    private DeltaBuilder(Class<T> objectClass, PrismContext prismContext) throws SchemaException {
        this.objectClass = objectClass;
        this.prismContext = prismContext;
        containerCTD = prismContext.getSchemaRegistry().findComplexTypeDefinitionByCompileTimeClass(this.objectClass);
        if (containerCTD == null) {
            throw new SchemaException("Couldn't find definition for complex type " + this.objectClass);
        }
        deltas = new ArrayList<>();
        currentDelta = null;
    }

    public DeltaBuilder(Class<T> objectClass, ComplexTypeDefinition containerCTD, PrismContext prismContext, List<ItemDelta<?,?>> deltas, ItemDelta currentDelta) {
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
        return item(new ItemPath(names));
    }

    @Override
    public S_ValuesEntry item(Object... namesOrIds) {
        return item(new ItemPath(namesOrIds));
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
            newDelta = new PropertyDelta(path, (PrismPropertyDefinition) definition, prismContext);
        } else if (definition instanceof PrismContainerDefinition) {
            newDelta = new ContainerDelta(path, (PrismContainerDefinition) definition, prismContext);
        } else if (definition instanceof PrismReferenceDefinition) {
            newDelta = new ReferenceDelta(path, (PrismReferenceDefinition) definition, prismContext);
        } else {
            throw new IllegalStateException("Unsupported definition type: " + definition);
        }
        List<ItemDelta<?,?>> newDeltas = deltas;
        if (currentDelta != null) {
            newDeltas.add(currentDelta);
        }
        return new DeltaBuilder(objectClass, containerCTD, prismContext, newDeltas, newDelta);
    }

    // TODO fix this after ObjectDelta is changed to accept Containerable
    @Override
    public ObjectDelta asObjectDelta(String oid) {
        return ObjectDelta.createModifyDelta(oid, getAllDeltas(), (Class) objectClass, prismContext);
    }

    @Override
    public List<ObjectDelta<?>> asObjectDeltas(String oid) {
        return Collections.<ObjectDelta<?>>singletonList(ObjectDelta.createModifyDelta(oid, getAllDeltas(), (Class) objectClass, prismContext));
    }

    @Override
    public ItemDelta asItemDelta() {
        List<ItemDelta<?,?>> allDeltas = getAllDeltas();
        if (allDeltas.size() > 1) {
            throw new IllegalStateException("Too many deltas to fit into item delta: " + allDeltas.size());
        } else if (allDeltas.size() == 1) {
            return allDeltas.get(0);
        } else {
            return null;
        }
    }

    @Override
    public List<ItemDelta<?,?>> asItemDeltas() {
        return getAllDeltas();
    }

    private List<ItemDelta<?,?>> getAllDeltas() {
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

    private PrismValue toPrismValue(ItemDelta<?,?> currentDelta, @NotNull Object v) {
        ItemDefinition definition = currentDelta.getDefinition();
        if (definition instanceof PrismPropertyDefinition) {
            return new PrismPropertyValue<>(v);
        } else if (definition instanceof PrismContainerDefinition) {
            return ((Containerable) v).asPrismContainerValue();
        } else if (definition instanceof PrismReferenceDefinition) {
            if (v instanceof Referencable) {
                return ((Referencable) v).asReferenceValue();
            } else {
                throw new IllegalStateException("Expected Referencable, got: " + v);
            }
        } else {
            throw new IllegalStateException("Unsupported definition type: " + definition);
        }
    }

}
