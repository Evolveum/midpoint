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

package com.evolveum.midpoint.repo.sql.query2;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sql.query2.definition.Definition;
import com.evolveum.midpoint.repo.sql.query2.definition.EntityDefinition;
import org.apache.commons.lang.Validate;

/**
 * @author mederly
 */
public class DefinitionSearchResult<T extends Definition> {
    private T itemDefinition;                       // definition of the item itself
    private ItemPath remainder;                     // what has remained unresolved of the original search path

    public DefinitionSearchResult(T itemDefinition, ItemPath remainder) {
        Validate.notNull(itemDefinition, "itemDefinition");
        this.itemDefinition = itemDefinition;
        this.remainder = remainder;
    }

    public T getItemDefinition() {
        return itemDefinition;
    }

    public ItemPath getRemainder() {
        return remainder;
    }

    public boolean isComplete() {
        return ItemPath.isNullOrEmpty(remainder);
    }
}
