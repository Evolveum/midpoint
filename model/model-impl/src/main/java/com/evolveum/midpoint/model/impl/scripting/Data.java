/*
 * Copyright (c) 2010-2014 Evolveum
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

package com.evolveum.midpoint.model.impl.scripting;

import com.evolveum.midpoint.model.api.ScriptExecutionException;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

import javax.xml.namespace.QName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Data that are passed between individual scripting actions.
 *
 * The content passed between actions (expressions) is a list of prism items
 * (object, container, reference, property) - possibly multivalued. They are
 * expected to be of the same type.
 *
 * We insist on the data being prism Items in order to have its
 * definition (including data type and name) in place.
 *
 * @author mederly
 */
public class Data implements DebugDumpable {

    private static final QName PLAIN_STRING_ELEMENT_NAME = new QName(SchemaConstants.NS_C, "string");

    private List<Item> data;

    // we want clients to use explicit constructors
    private Data() {
    }

    public List<Item> getData() {
        return data;
    }

    public void setData(List<Item> data) {
        this.data = data;
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String debugDump(int indent) {
        return DebugUtil.debugDump(data, indent);
    }

    public static Data create(Item data) {
        Data d = createEmpty();
        d.addItem(data);
        return d;
    }

    public static Data createEmpty() {
        Data d = new Data();
        d.data = new ArrayList<>();
        return d;
    }

    public void addAllFrom(Data data) {
        if (data != null) {
            this.data.addAll(data.getData());
        }
    }

    public void addItem(Item item) {
        data.add(item);
    }

    public String getDataAsSingleString() throws ScriptExecutionException {
        if (data != null && !data.isEmpty()) {
            if (data.size() == 1) {
                return (String) ((PrismProperty) data.get(0)).getRealValue();       // todo implement some diagnostics when this would fail
            } else {
                throw new ScriptExecutionException("Multiple values where just one is expected");
            }
        } else {
            return null;
        }
    }

    public static Data createProperty(Object object, PrismContext prismContext) {
        return createProperty(Arrays.asList(object), object.getClass(), prismContext);
    }

    public static Data createProperty(List<Object> objects, Class<?> clazz, PrismContext prismContext) {
        // TODO fix this temporary solution (haven't we somewhere universal method to do this?)
        QName elementName;
        QName typeName;
        if (String.class.isAssignableFrom(clazz)) {
            elementName = PLAIN_STRING_ELEMENT_NAME;
            typeName = DOMUtil.XSD_STRING;
        } else if (ObjectDeltaType.class.isAssignableFrom(clazz)) {
            elementName = SchemaConstants.T_OBJECT_DELTA;
            typeName = SchemaConstants.T_OBJECT_DELTA_TYPE;
        } else {
            throw new IllegalStateException("Unsupported data class (to be put into scripting data as property): " + clazz);
        }
        PrismPropertyDefinition propertyDefinition = new PrismPropertyDefinition<>(elementName, typeName, prismContext);
        PrismProperty property = propertyDefinition.instantiate();
        for (Object object : objects) {
            property.addRealValue(object);
        }
        return create(property);
    }

    public Collection<ObjectReferenceType> getDataAsReferences(QName defaultTargetType) throws ScriptExecutionException {
        Collection<ObjectReferenceType> retval = new ArrayList<ObjectReferenceType>(data.size());
        for (Item item : data) {
            if (item instanceof PrismObject) {
                ObjectReferenceType ref = new ObjectReferenceType();
                ref.setType(item.getDefinition().getTypeName());            // todo check the definition is present
                ref.setOid(((PrismObject) item).getOid());                  // todo check if oid is present
                retval.add(ref);
            } else if (item instanceof PrismProperty) {
                for (Object value : ((PrismProperty) item).getRealValues()) {
                    if (value instanceof String) {
                        ObjectReferenceType ref = new ObjectReferenceType();
                        ref.setType(defaultTargetType);
                        ref.setOid((String) value);                         // todo implement search by name
                        retval.add(ref);
                    } else if (value instanceof ObjectReferenceType) {
                        retval.add((ObjectReferenceType) value);
                    } else {
                        throw new ScriptExecutionException("Unsupported reference type: " + value.getClass());
                    }
                }
            } else if (item instanceof PrismReference) {
                PrismReference reference = (PrismReference) item;
                for (PrismReferenceValue value : reference.getValues()) {
                    ObjectReferenceType ref = new ObjectReferenceType();
                    ref.setupReferenceValue(value);
                    retval.add(ref);
                }
            }
        }
        return retval;
    }
}
