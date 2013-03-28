/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql.query2.restriction;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.QueryContext;
import com.evolveum.midpoint.repo.sql.query2.QueryDefinitionRegistry;
import com.evolveum.midpoint.repo.sql.query2.definition.AnyDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.Definition;
import com.evolveum.midpoint.repo.sql.query2.definition.PropertyDefinition;
import org.hibernate.criterion.Criterion;

import java.util.Iterator;
import java.util.List;

/**
 * @author lazyman
 */
public class AnyPropertyRestriction extends ItemRestriction<ValueFilter> {

    @Override
    public boolean canHandle(ObjectFilter filter, QueryContext context) throws QueryException {
        if (!super.canHandle(filter, context)) {
            return false;
        }

        ValueFilter valFilter = (ValueFilter) filter;
        ItemPath fullPath = createFullPath(valFilter);

        List<Definition> defPath = createDefinitionPath(fullPath, context);
        return containsAnyDefinition(defPath);
    }

    private boolean containsAnyDefinition(List<Definition> definitions) {
        for (Definition definition : definitions) {
            if (definition instanceof AnyDefinition) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Criterion interpretInternal(ValueFilter filter, ObjectQuery query, QueryContext context, Restriction parent)
            throws QueryException {
        //todo implement
        return null;
    }
}
