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

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.builder.R_Filter;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntryOrEmpty;
import com.evolveum.midpoint.util.exception.SchemaException;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
public class DeltaBuilder implements S_ItemEntry, S_MaybeDelete, S_ValuesEntry {

    final private Class<? extends Objectable> objectClass;
    final private ComplexTypeDefinition containerCTD;
    final private PrismContext prismContext;

    // BEWARE - although these are final, their content may (and does) vary. Not much clean.
    final List<ItemDelta> deltas;
    final ItemDelta currentDelta;

    private DeltaBuilder(Class<? extends Objectable> objectClass, PrismContext prismContext) throws SchemaException {
        this.objectClass = objectClass;
        this.prismContext = prismContext;
        containerCTD = prismContext.getSchemaRegistry().findComplexTypeDefinitionByCompileTimeClass(this.objectClass);
        if (containerCTD == null) {
            throw new SchemaException("Couldn't find definition for complex type " + this.objectClass);
        }
        deltas = new ArrayList<>();
        currentDelta = null;
    }

    public DeltaBuilder(Class<? extends Objectable> objectClass, ComplexTypeDefinition containerCTD, PrismContext prismContext, List<ItemDelta> deltas, ItemDelta currentDelta) {
        this.objectClass = objectClass;
        this.containerCTD = containerCTD;
        this.prismContext = prismContext;
        this.deltas = deltas;
        this.currentDelta = currentDelta;
    }

    public Class<? extends Containerable> getObjectClass() {
        return objectClass;
    }

    public PrismContext getPrismContext() {
        return prismContext;
    }

    public static S_ItemEntry deltaFor(Class<? extends Objectable> objectClass, PrismContext prismContext) throws SchemaException {
        return new DeltaBuilder(objectClass, prismContext);
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
        List<ItemDelta> newDeltas = deltas;
        if (currentDelta != null) {
            newDeltas.add(currentDelta);
        }
        return new DeltaBuilder(objectClass, containerCTD, prismContext, newDeltas, newDelta);
    }

    @Override
    public ObjectDelta asObjectDelta(String oid) {
        return ObjectDelta.createModifyDelta(oid, getAllDeltas(), objectClass, prismContext);
    }

    @Override
    public ItemDelta asItemDelta() {
        List<ItemDelta> allDeltas = getAllDeltas();
        if (allDeltas.size() > 1) {
            throw new IllegalStateException("Too many deltas to fit into item delta: " + allDeltas.size());
        } else if (allDeltas.size() == 1) {
            return allDeltas.get(0);
        } else {
            return null;
        }
    }

    @Override
    public List<ItemDelta> asItemDeltas() {
        return getAllDeltas();
    }

    private List<ItemDelta> getAllDeltas() {
        if (currentDelta != null) {
            deltas.add(currentDelta);
        }
        return deltas;
    }

    @Override
    public S_MaybeDelete add(Object... realValues) {
        checkNullMisuse(realValues);
        for (Object v : realValues) {
            currentDelta.addValueToAdd(toPrismValue(currentDelta, v));
        }
        return this;
    }

    @Override
    public S_MaybeDelete add(PrismValue... values) {
        currentDelta.addValuesToAdd(values);
        return this;
    }

    @Override
    public S_ItemEntry delete(Object... realValues) {
        checkNullMisuse(realValues);
        for (Object v : realValues) {
            currentDelta.addValueToDelete(toPrismValue(currentDelta, v));
        }
        return this;
    }

    protected void checkNullMisuse(Object[] realValues) {
        if (realValues.length == 1 && realValues[0] == null) {
            throw new IllegalArgumentException("NULL value should be represented as no value, not as 'null'");
        }
    }

    @Override
    public S_ItemEntry delete(PrismValue... values) {
        currentDelta.addValuesToDelete(values);
        return this;
    }

    @Override
    public S_ItemEntry replace(Object... realValues) {
        checkNullMisuse(realValues);
        List<PrismValue> prismValues = new ArrayList<>();
        for (Object v : realValues) {
            prismValues.add(toPrismValue(currentDelta, v));
        }
        currentDelta.setValuesToReplace(prismValues);
        return this;
    }

    @Override
    public S_ItemEntry replace(Collection<PrismValue> values) {
        currentDelta.setValuesToReplace(values);
        return this;
    }

    @Override
    public S_ItemEntry replace(PrismValue... values) {
        checkNullMisuse(values);
        currentDelta.setValuesToReplace(values);
        return this;
    }

    private PrismValue toPrismValue(ItemDelta currentDelta, Object v) {
        ItemDefinition definition = currentDelta.getDefinition();
        if (definition instanceof PrismPropertyDefinition) {
            return new PrismPropertyValue<>(v);
        } else if (definition instanceof PrismContainerDefinition) {
            return ((Containerable) v).asPrismContainerValue();
        } else if (definition instanceof PrismReferenceDefinition) {
            throw new IllegalStateException("Using real value for reference deltas is not supported: " + v);
        } else {
            throw new IllegalStateException("Unsupported definition type: " + definition);
        }
    }

}
