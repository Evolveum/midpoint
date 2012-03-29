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
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql.query;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.PropertyPathSegment;
import com.evolveum.midpoint.prism.dom.PrismDomProcessor;
import com.evolveum.midpoint.repo.sql.ClassMapper;
import com.evolveum.midpoint.repo.sql.data.common.RAnyConverter;
import com.evolveum.midpoint.repo.sql.data.common.RUtil;
import com.evolveum.midpoint.repo.sql.type.QNameType;
import com.evolveum.midpoint.schema.SchemaConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class SimpleOp extends Op {

    private static enum Operation {EQUAL}

    private static final Trace LOGGER = TraceManager.getTrace(SimpleOp.class);

    public SimpleOp(QueryInterpreter interpreter) {
        super(interpreter);
    }

    @Override
    protected QName[] canHandle() {
        return new QName[]{SchemaConstants.C_EQUAL};
    }

    private Operation getOperationType(Element filterPart) throws QueryException {
        if (DOMUtil.isElementName(filterPart, SchemaConstants.C_EQUAL)) {
            return Operation.EQUAL;
        }

        throw new QueryException("Unknown filter type '" + DOMUtil.getQNameWithoutPrefix(filterPart) + "'.");
    }

    @Override
    public Criterion interpret(Element filter, boolean pushNot) throws QueryException {
        LOGGER.debug("Interpreting '{}', pushNot '{}'", new Object[]{DOMUtil.getQNameWithoutPrefix(filter), pushNot});
        validate(filter);

        Element path = DOMUtil.getChildElement(filter, SchemaConstants.C_PATH);
        PropertyPath propertyPath = getInterpreter().createPropertyPath(path);
        if (path != null) {
            updateQueryContext(propertyPath);
        }

        Element value = DOMUtil.getChildElement(filter, SchemaConstants.C_VALUE);
        if (value == null || DOMUtil.listChildElements(value).isEmpty()) {
            throw new QueryException("Equal without value element, or without element in <value> not supported now.");
        }

        Element condition = DOMUtil.listChildElements(value).get(0);
        SimpleItem conditionItem = updateConditionItem(condition, propertyPath);

        LOGGER.trace("Condition item updated, updating value type.");
        Criterion criterion = null;
        if (!conditionItem.isReference) {
            Object testedValue = RAnyConverter.getRealValue(getInterpreter().getType(), propertyPath, condition);
            LOGGER.trace("Value updated to type '{}'",
                    new Object[]{(testedValue == null ? null : testedValue.getClass().getName())});

            criterion = createBaseCriteria(filter, pushNot, conditionItem.getQueryableItem(), testedValue);
        }

        if (conditionItem.isAny) {
            criterion = interpretAny(condition, conditionItem, path, criterion);
        } else if (conditionItem.isReference) {
            criterion = interpretReference(condition, conditionItem, filter, pushNot);
        }

        return criterion;
    }

    private Criterion createBaseCriteria(Element filter, boolean pushNot, String name, Object testedValue) throws
            QueryException {
        //will be used later to support other filters than <equal>
        Operation operation = getOperationType(filter);
        switch (operation) {
            case EQUAL:
                if (pushNot) {
                    return Restrictions.ne(name, testedValue);
                } else {
                    return Restrictions.eq(name, testedValue);
                }
        }

        throw new IllegalStateException("Couldn't create base criteria for filter '"
                + DOMUtil.getQNameWithoutPrefix(filter) + "' and pushNot '" + pushNot + "'.");
    }

    private Criterion interpretAny(Element condition, SimpleItem conditionItem, Element path, Criterion baseCriterion)
            throws QueryException {
        LOGGER.trace("Condition is type of any, creating conjunction for value and QName name, type");
        QName condQName = DOMUtil.getQNameWithoutPrefix(condition);
        ItemDefinition itemDefinition = getInterpreter().findDefinition(path, condQName);

        QName name, type;
        if (itemDefinition == null) {
            name = condQName;
            type = DOMUtil.resolveXsiType(condition);
        } else {
            name = itemDefinition.getName();
            type = itemDefinition.getTypeName();
        }

        if (name == null || type == null) {
            throw new QueryException("Couldn't get name or type for queried item '" + condQName + "'");
        }

        Conjunction conjunction = Restrictions.conjunction();
        conjunction.add(baseCriterion);
        conjunction.add(Restrictions.eq(conditionItem.alias + ".name", QNameType.optimizeQName(name)));
        conjunction.add(Restrictions.eq(conditionItem.alias + ".type", QNameType.optimizeQName(type)));

        return conjunction;
    }

    private Criterion interpretReference(Element condition, SimpleItem conditionItem, Element filter, boolean pushNot)
            throws QueryException {
        String targetOid = condition.getAttribute("oid");
        if (StringUtils.isEmpty(targetOid)) {
            throw new QueryException("Couldn't find target oid in reference in query value element.");
        }

        PrismDomProcessor domProcessor = getInterpreter().getPrismContext().getPrismDomProcessor();
        PrismReferenceValue refValue = domProcessor.parseReferenceValue(condition);
        QName type = refValue.getTargetType();
        Criterion criterion = createBaseCriteria(filter, pushNot, conditionItem.getQueryableItem(), targetOid);

        if (type != null) {
            Conjunction conjunction = Restrictions.conjunction();
            conjunction.add(criterion);
            conjunction.add(Restrictions.eq("type", ClassMapper.getHQLTypeForQName(type)));

            criterion = conjunction;
        }

        return criterion;
    }

    private SimpleItem updateConditionItem(Element condition, PropertyPath propertyPath) throws QueryException {
        QName conditionItem = DOMUtil.getQNameWithoutPrefix(condition);
        LOGGER.debug("Updating condition item '{}' on property path\n{}",
                new Object[]{conditionItem, propertyPath});
        SimpleItem item = new SimpleItem();
        EntityDefinition definition = findDefinition(getInterpreter().getType(), propertyPath);

        if (propertyPath != null) {
            if (definition.isAny()) {
                item.isAny = true;
                List<PropertyPathSegment> segments = propertyPath.getSegments();
                String anyTypeName = RAnyConverter.getAnySetType(getInterpreter().getType(), propertyPath, condition);
                segments.add(new PropertyPathSegment(new QName(RUtil.NS_SQL_REPO, anyTypeName)));

                propertyPath = new PropertyPath(segments);
                LOGGER.trace("Condition item is from 'any' container, adding new criteria based on any type '{}'",
                        new Object[]{anyTypeName});
                addNewCriteriaToContext(propertyPath, anyTypeName);
            }

            item.alias = getInterpreter().getAlias(propertyPath);
            LOGGER.trace("Found alias '{}' for path.", new Object[]{item.alias});
        }

        if (definition.isAny()) {
            item.item = "value";
        } else {
            Definition def = definition.findDefinition(conditionItem);
            if (def == null) {
                throw new QueryException("Couldn't find query definition for condition item '" + conditionItem + "'.");
            }
            if (def.isEntity()) {
                throw new QueryException("Can't query entity for value, only attribute can be queried for value.");
            }

            AttributeDefinition attrDef = (AttributeDefinition) def;
            if (!attrDef.isIndexed()) {
                LOGGER.warn("You're probably querying by attribute ('" + attrDef + "') which is not indexed.");
            }
            if (attrDef.isReference()) {
                PropertyPath propPath = propertyPath;
                String realName = attrDef.getRealName();
                if (propPath == null) {
                    //used in references from main criteria
                    propPath = new PropertyPath(new QName(RUtil.NS_SQL_REPO, realName));
                }
                addNewCriteriaToContext(propPath, realName);
                item.isReference = true;
                item.alias = getInterpreter().getAlias(propPath);
                LOGGER.trace("Found alias '{}' for path.", new Object[]{item.alias});
                item.item = "targetOid";
            } else {
                item.item = attrDef.getRealName();
            }
        }

        return item;
    }

    private EntityDefinition findDefinition(Class<? extends ObjectType> type, PropertyPath path) throws QueryException {
        EntityDefinition definition = getClassTypeDefinition(type);
        if (path == null) {
            return definition;
        }

        Definition def;
        for (PropertyPathSegment segment : path.getSegments()) {
            def = definition.findDefinition(segment.getName());
            if (!def.isEntity()) {
                throw new QueryException("Can't query attribute in attribute.");
            } else {
                definition = (EntityDefinition) def;
            }
        }

        return definition;
    }

    private EntityDefinition getClassTypeDefinition(Class<? extends ObjectType> type) throws QueryException {
        EntityDefinition definition = QueryRegistry.getInstance().findDefinition(type);
        if (definition == null) {
            throw new QueryException("Can't query, unknown type '" + type.getSimpleName() + "'.");
        }

        return definition;
    }

    private void updateQueryContext(PropertyPath path) throws QueryException {
        LOGGER.debug("Updating query context based on path\n{}", new Object[]{path.toString()});
        Class<? extends ObjectType> type = getInterpreter().getType();
        Definition definition = getClassTypeDefinition(type);

        List<PropertyPathSegment> segments = path.getSegments();

        List<PropertyPathSegment> propPathSegments = new ArrayList<PropertyPathSegment>();
        PropertyPath propPath;
        for (PropertyPathSegment segment : segments) {
            QName qname = segment.getName();
            //create new property path
            propPathSegments.add(new PropertyPathSegment(qname));
            propPath = new PropertyPath(propPathSegments);
            //get entity query definition
            definition = definition.findDefinition(qname);
            if (definition == null || !definition.isEntity()) {
                throw new QueryException("This definition '" + definition + "' is not entity definition, " +
                        "we can't query attribute in attribute. Please check your path in query, or query " +
                        "entity/attribute mappings.");
            }

            EntityDefinition entityDefinition = (EntityDefinition) definition;
            if (entityDefinition.isEmbedded()) {
                //for embedded relationships we don't have to create new criteria
                LOGGER.trace("Skipping segment '{}' because it's embedded, sub path\n{}",
                        new Object[]{qname, propPath.toString()});
                continue;
            }

            LOGGER.trace("Adding criteria '{}' to context based on sub path\n{}",
                    new Object[]{definition.getRealName(), propPath.toString()});
            addNewCriteriaToContext(propPath, definition.getRealName());
        }
    }

    private void addNewCriteriaToContext(PropertyPath propPath, String realName) {
        PropertyPath lastPropPath = propPath.allExceptLast();
        if (PropertyPath.EMPTY_PATH.equals(lastPropPath)) {
            lastPropPath = null;
        }
        // get parent criteria
        Criteria pCriteria = getInterpreter().getCriteria(lastPropPath);
        // create new criteria for this relationship
        String alias = getInterpreter().createAlias(propPath.last().getName());
        Criteria criteria = pCriteria.createCriteria(realName, alias);
        //save criteria and alias to our query context
        getInterpreter().setCriteria(propPath, criteria);
        getInterpreter().setAlias(propPath, alias);
    }

    private static class SimpleItem {

        String item;
        String alias;
        boolean isAny;
        boolean isReference;

        String getQueryableItem() {
            StringBuilder builder = new StringBuilder();
            if (StringUtils.isNotEmpty(alias)) {
                builder.append(alias);
                builder.append(".");
            }
            builder.append(item);
            return builder.toString();
        }
    }
}
