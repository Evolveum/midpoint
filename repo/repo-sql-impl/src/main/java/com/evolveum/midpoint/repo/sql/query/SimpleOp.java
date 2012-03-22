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

import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.PropertyPathSegment;
import com.evolveum.midpoint.schema.SchemaConstants;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.holder.XPathSegment;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
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
        if (path != null && StringUtils.isNotEmpty(path.getTextContent())) {
            updateQueryContext(path);
        }

        Element value = DOMUtil.getChildElement(filter, SchemaConstants.C_VALUE);
        if (value == null || DOMUtil.listChildElements(value).isEmpty()) {
            throw new QueryException("Equal without value element, or without element in <value> not supported now.");
        }

        Element condition = DOMUtil.listChildElements(value).get(0);
        String conditionItem = updateConditionItem(DOMUtil.getQNameWithoutPrefix(condition), path);

        //todo if we're querying extension value create conjunction on qname name and type and value also get type right

        Criterion equal;
        if (pushNot) {
            equal = Restrictions.ne(conditionItem, condition.getTextContent());
        } else {
            equal = Restrictions.eq(conditionItem, condition.getTextContent());
        }

        return equal;
    }

    private String updateConditionItem(QName conditionItem, Element path) throws QueryException {
        PropertyPath propertyPath = null;
        if (path != null && StringUtils.isNotEmpty(path.getTextContent())) {
            propertyPath = new XPathHolder(path).toPropertyPath();
        }

        EntityDefinition definition = findDefinition(getInterpreter().getType(), propertyPath);

        StringBuilder item = new StringBuilder();
        if (propertyPath != null) {
            if (definition.isAny()) {
                String anyTypeName = "";
                addNewCriteriaToContext(propertyPath, anyTypeName);
            } else {
                String alias = getContext().getAlias(propertyPath);
                if (StringUtils.isNotEmpty(alias)) {
                    item.append(alias);
                }
            }
        }

        if (item.length() != 0) {
            item.append(".");
        }

        if (definition.isAny()) {
            item.append("value");
        } else {
            item.append(definition.findDefinition(conditionItem));
        }

        return item.toString();
    }

    private EntityDefinition findDefinition(Class<? extends ObjectType> type, PropertyPath path) {
        //todo implement
        return null;
    }

    private void updateQueryContext(Element path) throws QueryException {
        Class<? extends ObjectType> type = getInterpreter().getType();
        Definition definition = QueryRegistry.getInstance().findDefinition(type);
        if (definition == null) {
            throw new QueryException("Can't query, unknown type '" + type.getSimpleName() + "'.");
        }

        List<XPathSegment> segments = new XPathHolder(path).toSegments();

        List<PropertyPathSegment> propPathSegments = new ArrayList<PropertyPathSegment>();
        PropertyPath propPath;
        for (XPathSegment segment : segments) {
            QName qname = segment.getQName();
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

//            if (entityDefinition.isAny()) {
            //todo we have to create special criterias...
//            } else {
            //todo createCriteria for path items with aliases, also save these in some map as <path, prefix>
            //check path with some schema stuff as well as check it against query annotations in data.common package
            //add this mappings to query context...

            // todo check if propPath is extension (RAnyContainer association) and than we have to
            // create one more criteria based on definition to string/long/date (clob throws exception).
            // Also add it as new property path to context

            addNewCriteriaToContext(propPath, definition.getRealName());
//            }
        }
    }

    private void addNewCriteriaToContext(PropertyPath propPath, String realName) throws QueryException {
        PropertyPath lastPropPath = propPath.allExceptLast();
        if (PropertyPath.EMPTY_PATH.equals(lastPropPath)) {
            lastPropPath = null;
        }
        // get parent criteria
        Criteria pCriteria = getContext().getCriteria(lastPropPath);
        // create new criteria for this relationship
        String alias = createAlias(propPath.last());
        Criteria criteria = pCriteria.createCriteria(realName, alias);
        //save criteria and alias to our query context
        getContext().setCriteria(propPath, criteria);
        getContext().setAlias(propPath, createAlias(propPath.last()));
    }

    private String createAlias(PropertyPathSegment segment) throws QueryException {
        String prefix = Character.toString(segment.getName().getLocalPart().charAt(0));
        int index = 0;

        String alias = prefix;
        while (getContext().hasAlias(alias)) {
            alias = prefix + Integer.toString(index);
            index++;

            if (index > 20) {
                throw new QueryException("Alias index for segment '" + segment.getName()
                        + "' is more than 20? Should not happen.");
            }
        }

        return alias;
    }
}
