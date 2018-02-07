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
import com.evolveum.midpoint.prism.PrismContext;

/**
 * Here is the language structure:

 Query ::= Filter? ('ASC(path)' | 'DESC(path)')*

 Filter ::= 'NOT'? SimpleFilter ( ('AND'|'OR') 'NOT'? SimpleFilter )*

 SimpleFilter ::= PrimitiveFilter |
                  'BLOCK' Filter 'END-BLOCK' |
                  'TYPE(type)' Filter |
                  'EXISTS(path)' Filter

 PrimitiveFilter ::= 'ALL' | 'NONE' | 'UNDEFINED' |
                     ('ITEM(path)' ( ValueComparisonCondition | 'IS-NULL' | ( ItemComparisonCondition 'ITEM(path)') ) ) |
                     ('ID(values)') | ('OWNER-ID(values)')

 ValueComparisonCondition ::= 'EQ(value)' | 'GT(value)' | 'GE(value)' | 'LT(value)' | 'LE(value)' | 'STARTSWITH(value)' | 'ENDSWITH(value)' | 'CONTAINS(value)' | 'REF(value)' | 'ORG(value)'
 ItemComparisonCondition ::= 'EQ' | 'GT' | 'GE' | 'LT' | 'LE'

 *
 * It can be visualized e.g. using http://www.bottlecaps.de/rr/ui
 *
 * Individual keywords ('AND', 'OR', 'BLOCK', ...) are mapped to methods.
 * Connections between these keywords are mapped to interfaces.
 * It can be viewed as interfaces = states, keywords = transitions. (Or vice versa, but this is more natural.)
 * The interfaces have names starting with S_ (for "state").
 *
 * Interfaces are implemented by classes that aggregate state of the query being created. This is quite hacked for now... to be implemented more seriously.
 *
 * @author mederly
 */
public class QueryBuilder {

    final private Class<? extends Containerable> queryClass;
    final private ComplexTypeDefinition containerCTD;
    final private PrismContext prismContext;

    private QueryBuilder(Class<? extends Containerable> queryClass, PrismContext prismContext) {
        this.queryClass = queryClass;
        this.prismContext = prismContext;
        containerCTD = prismContext.getSchemaRegistry().findComplexTypeDefinitionByCompileTimeClass(queryClass);
        if (containerCTD == null) {
            throw new IllegalArgumentException("Couldn't find definition for complex type " + queryClass);
        }
    }

    public Class<? extends Containerable> getQueryClass() {
        return queryClass;
    }

    public PrismContext getPrismContext() {
        return prismContext;
    }

    public static S_FilterEntryOrEmpty queryFor(Class<? extends Containerable> queryClass, PrismContext prismContext) {
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
