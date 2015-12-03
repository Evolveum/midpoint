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

package com.evolveum.midpoint.prism.query.builder;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.AllFilter;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrFilter;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.apache.commons.lang.Validate;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mederly
 */
public class QueryBuilder {

    final private Class<? extends Containerable> queryClass;
    final private ComplexTypeDefinition containerCTD;
    final private PrismContext prismContext;

    private QueryBuilder(Class<? extends Containerable> queryClass, PrismContext prismContext) throws SchemaException {
        this.queryClass = queryClass;
        this.prismContext = prismContext;
        containerCTD = prismContext.getSchemaRegistry().findComplexTypeDefinitionByCompileTimeClass(queryClass);
        if (containerCTD == null) {
            throw new SchemaException("Couldn't find definition for complex type " + queryClass);
        }
    }

    public Class<? extends Containerable> getQueryClass() {
        return queryClass;
    }

    public PrismContext getPrismContext() {
        return prismContext;
    }

    public static S_FilterEntryOrEmpty queryFor(Class<? extends Containerable> queryClass, PrismContext prismContext) throws SchemaException {
        QueryBuilder builder = new QueryBuilder(queryClass, prismContext);
        return R_Filter.create(builder);
    }

//    ItemDefinition findItemDefinition(ItemPath itemPath) throws SchemaException {
//        ItemDefinition itemDefinition = containerCTD.findItemDefinition(itemPath);
//        if (itemDefinition == null) {
//            throw new SchemaException("Couldn't find definition for '" + itemPath + "' in " + containerCTD);
//        }
//        return itemDefinition;
//    }

}
