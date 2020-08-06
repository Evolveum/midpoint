/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.query.builder;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntryOrEmpty;

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
 * <p>
 * Individual keywords ('AND', 'OR', 'BLOCK', ...) are mapped to methods.
 * Connections between these keywords are mapped to interfaces.
 * It can be viewed as interfaces = states, keywords = transitions. (Or vice versa, but this is more natural.)
 * The interfaces have names starting with S_ (for "state").
 * <p>
 * Interfaces are implemented by classes that aggregate state of the query being created.
 * This is quite hacked for now... to be implemented more seriously.
 *
 * @author mederly
 */
public final class QueryBuilder {

    private final Class<? extends Containerable> queryClass;
    private final PrismContext prismContext;

    private QueryBuilder(Class<? extends Containerable> queryClass, PrismContext prismContext) {
        this.queryClass = queryClass;
        this.prismContext = prismContext;
        ComplexTypeDefinition containerCTD = prismContext.getSchemaRegistry().findComplexTypeDefinitionByCompileTimeClass(queryClass);
        if (containerCTD == null) {
            throw new IllegalArgumentException("Couldn't find definition for complex type " + queryClass);
        }
    }

    Class<? extends Containerable> getQueryClass() {
        return queryClass;
    }

    public PrismContext getPrismContext() {
        return prismContext;
    }

    public static S_FilterEntryOrEmpty queryFor(Class<? extends Containerable> queryClass, PrismContext prismContext) {
        return R_Filter.create(new QueryBuilder(queryClass, prismContext));
    }
}
