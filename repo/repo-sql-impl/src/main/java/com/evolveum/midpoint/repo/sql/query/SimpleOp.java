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
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.PropertyPathSegment;
import com.evolveum.midpoint.repo.sql.data.common.RUtil;
import com.evolveum.midpoint.repo.sql.type.QNameType;
import com.evolveum.midpoint.schema.SchemaConstants;
import com.evolveum.midpoint.util.DOMUtil;
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

    public SimpleOp(QueryInterpreter interpreter) {
        super(interpreter);
    }

    @Override
    protected QName[] canHandle() {
        return new QName[]{SchemaConstants.C_EQUAL};
    }

//        <c:equal>
//            <c:path>c:extension</c:path>
//            <c:value>
//                <s:foo xmlns:s="http://example.com/foo">bar</s:foo>
//            </c:value>
//        </c:equal>

    @Override
    public Criterion interpret(Element filter, boolean pushNot) throws QueryException {
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
        QName condQName = DOMUtil.getQNameWithoutPrefix(condition);
        SimpleItem conditionItem = updateConditionItem(condQName, propertyPath);

        Criterion criterion;
        if (pushNot) {
            criterion = Restrictions.ne(conditionItem.getQueryableItem(), condition.getTextContent());
        } else {
            criterion = Restrictions.eq(conditionItem.getQueryableItem(), condition.getTextContent());
        }

        if (conditionItem.isAny) {
            ItemDefinition itemDefinition = getInterpreter().findDefinition(path, condQName);
            QName name = itemDefinition.getName();
            QName type = itemDefinition.getTypeName();

            Conjunction conjunction = Restrictions.conjunction();
            conjunction.add(criterion);
            conjunction.add(Restrictions.eq(conditionItem.alias + ".name", QNameType.optimizeQName(name)));
            conjunction.add(Restrictions.eq(conditionItem.alias + ".type", QNameType.optimizeQName(type)));

            criterion = conjunction;
        }

        return criterion;
    }

    private SimpleItem updateConditionItem(QName conditionItem, PropertyPath propertyPath) throws QueryException {
        SimpleItem item = new SimpleItem();
        EntityDefinition definition = findDefinition(getInterpreter().getType(), propertyPath);

        if (propertyPath != null) {
            if (definition.isAny()) {
                item.isAny = true;

                List<PropertyPathSegment> segments = propertyPath.getSegments();
                //todo get from somewhere - from RAnyConverter, somehow
                //strings | longs | dates | clobs
                String anyTypeName = "strings";
                segments.add(new PropertyPathSegment(new QName(RUtil.NS_SQL_REPO, anyTypeName)));

                PropertyPath extPath = new PropertyPath(segments);
                addNewCriteriaToContext(extPath, anyTypeName);

                String alias = getInterpreter().getAlias(extPath);
                if (StringUtils.isNotEmpty(alias)) {
                    item.alias = alias;
                }
            } else {
                String alias = getInterpreter().getAlias(propertyPath);
                if (StringUtils.isNotEmpty(alias)) {
                    item.alias = alias;
                }
            }
        }

        if (definition.isAny()) {
            item.item = "value";
        } else {
            Definition attrDef = definition.findDefinition(conditionItem);
            if (attrDef == null) {
                throw new QueryException("Couldn't find query definition for condition item '" + conditionItem + "'.");
            }
            if (attrDef.isEntity()) {
                throw new QueryException("Can't query entity for value, only attribute can be queried for value.");
            }
            item.item = attrDef.getRealName();
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
                throw new QueryException("This definition is not entity definition, we can't query attribute " +
                        "in attribute. Please check your path in query, or query entity/attribute mappings.");
            }

            EntityDefinition entityDefinition = (EntityDefinition) definition;
            if (entityDefinition.isEmbedded()) {
                //for embedded relationships we don't have to create new criteria
                continue;
            }

            addNewCriteriaToContext(propPath, definition.getRealName());
        }
    }

    private void addNewCriteriaToContext(PropertyPath propPath, String realName) throws QueryException {
        PropertyPath lastPropPath = propPath.allExceptLast();
        if (PropertyPath.EMPTY_PATH.equals(lastPropPath)) {
            lastPropPath = null;
        }
        // get parent criteria
        Criteria pCriteria = getInterpreter().getCriteria(lastPropPath);
        // create new criteria for this relationship
        String alias = createAlias(propPath.last());
        Criteria criteria = pCriteria.createCriteria(realName, alias);
        //save criteria and alias to our query context
        getInterpreter().setCriteria(propPath, criteria);
        getInterpreter().setAlias(propPath, createAlias(propPath.last()));
    }

    private String createAlias(PropertyPathSegment segment) throws QueryException {
        String prefix = Character.toString(segment.getName().getLocalPart().charAt(0));
        int index = 0;

        String alias = prefix;
        while (getInterpreter().hasAlias(alias)) {
            alias = prefix + Integer.toString(index);
            index++;

            if (index > 20) {
                throw new QueryException("Alias index for segment '" + segment.getName()
                        + "' is more than 20? Should not happen.");
            }
        }

        return alias;
    }

    private static class SimpleItem {

        String item;
        String alias;
        boolean isAny;

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
