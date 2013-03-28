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
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.QueryContext;
import com.evolveum.midpoint.repo.sql.query2.QueryDefinitionRegistry;
import com.evolveum.midpoint.repo.sql.query2.definition.AnyDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.CollectionDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.Definition;
import com.evolveum.midpoint.repo.sql.query2.definition.EntityDefinition;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import org.apache.commons.lang.Validate;
import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public abstract class ItemRestriction<T extends ValueFilter> extends Restriction<T> {

    private static final Trace LOGGER = TraceManager.getTrace(ItemRestriction.class);

    public static enum Operation {
        EQ, GT, GE, LT, LE, NULL, NOT_NULL, SUBSTRING, SUBSTRING_IGNORE_CASE;
    }

    @Override
    public boolean canHandle(ObjectFilter filter, QueryContext context) throws QueryException {
        Validate.notNull(filter, "Object filter must not be null.");
        if (!(filter instanceof ValueFilter)) {
            return false;
        }
        return true;
    }

    @Override
    public Criterion interpret(T filter, ObjectQuery query, QueryContext context, Restriction parent)
            throws QueryException {

        ItemPath path = filter.getParentPath();
        if (path != null) {
            // at first we build criterias with aliases
            updateQueryContext(path, context);
        }

        return interpretInternal(filter, query, context, parent);
    }

    public abstract Criterion interpretInternal(T filter, ObjectQuery query, QueryContext context, Restriction parent)
            throws QueryException;

    //todo reimplement, use DefinitionHandlers or maybe another great concept
    private void updateQueryContext(ItemPath path, QueryContext context) throws QueryException {
        LOGGER.trace("Updating query context based on path\n{}", new Object[]{path.toString()});
        Class<? extends ObjectType> type = context.getType();
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
                    addNewCriteriaToContext(propPath, entityDef.getJpaName(), context);
                } else {
                    //add dot with jpaName to property path

                }
            } else if (childDef instanceof AnyDefinition) {
                LOGGER.trace("Adding criteria '{}' to context based on sub path\n{}",
                        new Object[]{childDef.getJpaName(), propPath.toString()});
                addNewCriteriaToContext(propPath, childDef.getJpaName(), context);
            } else if (childDef instanceof CollectionDefinition) {
                throw new QueryException("not implemented yet.");
            } else {
                //todo throw something here [lazyman]
                throw new QueryException("not implemented yet.");
            }
        }
    }

    private void addNewCriteriaToContext(ItemPath path, String realName, QueryContext context) {
        ItemPath lastPropPath = path.allExceptLast();
        if (ItemPath.EMPTY_PATH.equals(lastPropPath)) {
            lastPropPath = null;
        }

        // get parent criteria
        Criteria pCriteria = context.getCriteria(lastPropPath);
        // create new criteria and alias for this relationship

        String alias = context.addAlias(path);
        Criteria criteria = pCriteria.createCriteria(realName, alias);
        context.addCriteria(path, criteria);
    }

    protected String createPropertyName() {
        //todo implement
        return null;
    }

    protected Object createComparableRealValue() {
        //todo implement
        return null;
    }

    protected Criterion createCriterion(Operation operation) throws QueryException {
        String propertyName = createPropertyName();
        Object value = (operation == Operation.NOT_NULL
                || operation == Operation.NULL) ?
                null : createComparableRealValue();

        switch (operation) {
            case EQ:
                return Restrictions.eq(propertyName, value);
            case GT:
                return Restrictions.gt(propertyName, value);
            case GE:
                return Restrictions.ge(propertyName, value);
            case LT:
                return Restrictions.lt(propertyName, value);
            case LE:
                return Restrictions.le(propertyName, value);
            case NOT_NULL:
                return Restrictions.isNotNull(propertyName);
            case NULL:
                return Restrictions.isNull(propertyName);
            case SUBSTRING:
                return Restrictions.like(propertyName, "%" + value + "%");
            case SUBSTRING_IGNORE_CASE:
                return Restrictions.like(propertyName, "%" + value + "%").ignoreCase();
        }

        throw new QueryException("Unknown operation.");
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
