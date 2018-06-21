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

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * @author mederly
 */
public interface S_ItemEntry {

    S_ValuesEntry item(QName... names);
    S_ValuesEntry item(Object... namesOrIds);
    S_ValuesEntry item(ItemPath path);
    S_ValuesEntry item(ItemPath path, ItemDefinition itemDefinition);

    List<ObjectDelta<?>> asObjectDeltas(String oid);
    ObjectDelta<?> asObjectDelta(String oid);

    // TEMPORARY HACK
    @SuppressWarnings("unchecked")
    default <X extends Objectable> ObjectDelta<X> asObjectDeltaCast(String oid) {
        return (ObjectDelta<X>) asObjectDelta(oid);
    }

    ItemDelta<?,?> asItemDelta();
    List<ItemDelta<?,?>> asItemDeltas();

}
