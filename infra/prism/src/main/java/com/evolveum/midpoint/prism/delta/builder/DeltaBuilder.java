/*
 * Copyright (c) 2010-2018 Evolveum
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
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Grammar:
 *
 *  ObjectDelta ::= (ItemDelta)* ( 'OBJECT-DELTA(oid)' | 'ITEM-DELTA' | 'ITEM-DELTAS' )
 *
 *  ItemDelta ::= 'ITEM(...)' ( ( 'ADD-VALUES(...)' 'DELETE-VALUES(...)'? ) | 'DELETE-VALUES(...)' | 'REPLACE-VALUES(...)' )
 *
 * @author mederly
 */
public interface DeltaBuilder extends S_ItemEntry, S_MaybeDelete, S_ValuesEntry {

    static <C extends Containerable> S_ItemEntry deltaFor(Class<C> objectClass, PrismContext prismContext) throws SchemaException {
        return new DeltaBuilderImpl<>(objectClass, prismContext);
    }

}
