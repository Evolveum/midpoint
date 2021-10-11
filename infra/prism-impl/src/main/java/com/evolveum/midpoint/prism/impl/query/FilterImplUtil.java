/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.query;

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
public class FilterImplUtil {

    public static ItemDefinition findItemDefinition(ItemPath itemPath, PrismContainerDefinition<? extends Containerable> containerDef) {
        ItemDefinition itemDef = containerDef.findItemDefinition(itemPath);
        if (itemDef == null) {
            throw new IllegalStateException("No definition for item " + itemPath + " in container definition "
                    + containerDef);
        }

        return itemDef;
    }

    public static ItemDefinition findItemDefinition(ItemPath parentPath, ComplexTypeDefinition complexTypeDefinition) {
        ItemDefinition itemDef = complexTypeDefinition.findItemDefinition(parentPath);
        if (itemDef == null) {
            throw new IllegalStateException("No definition for item " + parentPath + " in complex type definition "
                    + complexTypeDefinition);
        }
        return itemDef;
    }

    public static ItemDefinition findItemDefinition(ItemPath parentPath, Class type, PrismContext prismContext) {
        ComplexTypeDefinition complexTypeDefinition = prismContext.getSchemaRegistry().findComplexTypeDefinitionByCompileTimeClass(type);
        if (complexTypeDefinition == null) {
            // TODO SchemaException instead?
            throw new IllegalStateException("Definition of complex type " + type + " couldn't be not found");
        }
        return findItemDefinition(parentPath, complexTypeDefinition);
    }

}
