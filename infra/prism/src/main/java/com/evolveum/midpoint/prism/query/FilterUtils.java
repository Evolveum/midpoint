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

package com.evolveum.midpoint.prism.query;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

/**
 * @author mederly
 */
public class FilterUtils {

    static ItemDefinition findItemDefinition(ItemPath itemPath, PrismContainerDefinition<? extends Containerable> containerDef) {
        ItemDefinition itemDef = containerDef.findItemDefinition(itemPath);
        if (itemDef == null) {
            throw new IllegalStateException("No definition for item " + itemPath + " in container definition "
                    + containerDef);
        }

        return itemDef;
    }

    static ItemDefinition findItemDefinition(ItemPath parentPath, ComplexTypeDefinition complexTypeDefinition) {
        ItemDefinition itemDef = complexTypeDefinition.findItemDefinition(parentPath);
        if (itemDef == null) {
            throw new IllegalStateException("No definition for item " + parentPath + " in complex type definition "
                    + complexTypeDefinition);
        }
        return itemDef;
    }

    static ItemDefinition findItemDefinition(ItemPath parentPath, Class type, PrismContext prismContext) {
        ComplexTypeDefinition complexTypeDefinition = prismContext.getSchemaRegistry().findComplexTypeDefinitionByCompileTimeClass(type);
        if (complexTypeDefinition == null) {
            // TODO SchemaException instead?
            throw new IllegalStateException("Definition of complex type " + type + " couldn't be not found");
        }
        return findItemDefinition(parentPath, complexTypeDefinition);
    }

	public static boolean isFilterEmpty(SearchFilterType filter) {
		return filter == null || (filter.getDescription() == null && !filter.containsFilterClause());
	}
}
