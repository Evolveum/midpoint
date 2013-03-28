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

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.QueryContext;
import com.evolveum.midpoint.repo.sql.query2.QueryDefinitionRegistry;
import com.evolveum.midpoint.repo.sql.query2.QueryInterpreter;
import com.evolveum.midpoint.repo.sql.query2.definition.AnyDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.CollectionDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.Definition;
import com.evolveum.midpoint.repo.sql.query2.definition.EntityDefinition;
import com.evolveum.midpoint.repo.sql.query2.matcher.Matcher;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import org.apache.commons.lang.Validate;
import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public abstract class ItemRestriction<T extends ValueFilter> extends Restriction<T> {

    private static final Trace LOGGER = TraceManager.getTrace(ItemRestriction.class);

    @Override
    public boolean canHandle(ObjectFilter filter, QueryContext context) throws QueryException {
        Validate.notNull(filter, "Object filter must not be null.");
        if (!(filter instanceof ValueFilter)) {
            return false;
        }
        return true;
    }

    @Override
    public Criterion interpret(T filter) throws QueryException {

        ItemPath path = filter.getParentPath();
        if (path != null) {
            // at first we build criterias with aliases
            updateQueryContext(path);
        }

        return interpretInternal(filter);
    }

    public abstract Criterion interpretInternal(T filter) throws QueryException;

    //todo reimplement, use DefinitionHandlers or maybe another great concept
    private void updateQueryContext(ItemPath path) throws QueryException {
        LOGGER.trace("Updating query context based on path\n{}", new Object[]{path.toString()});
        Class<? extends ObjectType> type = getContext().getType();
        QueryDefinitionRegistry registry = QueryDefinitionRegistry.getInstance();
        EntityDefinition definition = registry.findDefinition(type, null, EntityDefinition.class);

        List<ItemPathSegment> segments = path.getSegments();

        List<ItemPathSegment> propPathSegments = new ArrayList<ItemPathSegment>();
        ItemPath propPath;
        for (ItemPathSegment segment : segments) {
            QName qname = ItemPath.getName(segment);
            // create new property path
            propPathSegments.add(new NameItemPathSegment(qname));
            propPath = new ItemPath(propPathSegments);
            // get entity query definition

            Definition childDef = definition.findDefinition(qname, Definition.class);
            if (childDef == null) {
                throw new QueryException("Definition '" + definition + "' doesn't contain child definition '"
                        + qname + "'. Please check your path in query, or query entity/attribute mappings. "
                        + "Full path was '" + path + "'.");
            }

            //todo change this if instanceof and use DefinitionHandler [lazyman]
            if (childDef instanceof EntityDefinition) {
                EntityDefinition entityDef = (EntityDefinition) childDef;
                if (!entityDef.isEmbedded()) {
                    //create new criteria
                    LOGGER.trace("Adding criteria '{}' to context based on sub path\n{}",
                            new Object[]{entityDef.getJpaName(), propPath.toString()});
                    addNewCriteriaToContext(propPath, entityDef.getJpaName());
                } else {
                    //add dot with jpaName to property path

                }
            } else if (childDef instanceof AnyDefinition) {
                LOGGER.trace("Adding criteria '{}' to context based on sub path\n{}",
                        new Object[]{childDef.getJpaName(), propPath.toString()});
                addNewCriteriaToContext(propPath, childDef.getJpaName());
            } else if (childDef instanceof CollectionDefinition) {
                throw new QueryException("not implemented yet.");
            } else {
                //todo throw something here [lazyman]
                throw new QueryException("not implemented yet.");
            }
        }
    }

    protected void addNewCriteriaToContext(ItemPath path, String realName) {
        ItemPath lastPropPath = path.allExceptLast();
        if (ItemPath.EMPTY_PATH.equals(lastPropPath)) {
            lastPropPath = null;
        }

        // get parent criteria
        Criteria pCriteria = getContext().getCriteria(lastPropPath);
        // create new criteria and alias for this relationship

        String alias = getContext().addAlias(path);
        Criteria criteria = pCriteria.createCriteria(realName, alias);
        getContext().addCriteria(path, criteria);
    }

    protected Criterion createCriterion(String propertyName, Object value, ValueFilter filter) throws QueryException {
        ItemRestrictionOperation operation;
        if (filter instanceof EqualsFilter) {
            operation = ItemRestrictionOperation.EQ;
        } else if (filter instanceof GreaterFilter) {
            operation = ItemRestrictionOperation.GT;
        } else if (filter instanceof LessFilter) {
            operation = ItemRestrictionOperation.LT;
        } else if (filter instanceof SubstringFilter) {
            operation = ItemRestrictionOperation.SUBSTRING;
        } else {
            throw new QueryException("Can't translate filter '" + filter + "' to operation.");
        }

        QueryContext context = getContext();
        QueryInterpreter interpreter = context.getInterpreter();
        Matcher matcher = interpreter.findMatcher(value);

        return matcher.match(operation, propertyName, value, null);    //todo get matcher from filter
    }

    protected List<Definition> createDefinitionPath(ItemPath path, QueryContext context) throws QueryException {
        List<Definition> definitions = new ArrayList<Definition>();
        if (path == null) {
            return definitions;
        }

        QueryDefinitionRegistry registry = QueryDefinitionRegistry.getInstance();

        EntityDefinition lastDefinition = registry.findDefinition(context.getType(), null, EntityDefinition.class);
        for (ItemPathSegment segment : path.getSegments()) {
            if (lastDefinition == null) {
                break;
            }

            if (!(segment instanceof NameItemPathSegment)) {
                continue;
            }

            NameItemPathSegment named = (NameItemPathSegment) segment;
            Definition def = lastDefinition.findDefinition(named.getName(), Definition.class);
            definitions.add(def);

            if (def instanceof EntityDefinition) {
                lastDefinition = (EntityDefinition) def;
            } else {
                lastDefinition = null;
            }
        }

        return definitions;
    }
}
