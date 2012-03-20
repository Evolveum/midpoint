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
public class EqualOp extends Op {

    public EqualOp(QueryInterpreter interpreter) {
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
    public Criterion interpret(Element filter, boolean pushNot) throws QueryInterpreterException {
        validate(filter);

        Element path = DOMUtil.getChildElement(filter, SchemaConstants.C_PATH);
        if (path != null && StringUtils.isNotEmpty(path.getTextContent())) {
            updateQueryContext(path);
        }

        Element value = DOMUtil.getChildElement(filter, SchemaConstants.C_VALUE);
        if (value == null || DOMUtil.listChildElements(value).isEmpty()) {
            throw new QueryInterpreterException("Equal without value element, or without element in <value> not supported now.");
        }

        Element condition = DOMUtil.listChildElements(value).get(0);
        String conditionItem = updateConditionItem(DOMUtil.getQNameWithoutPrefix(condition), path);

        Criterion equal;
        if (pushNot) {
            equal = Restrictions.ne(conditionItem, condition.getTextContent());
        } else {
            equal = Restrictions.eq(conditionItem, condition.getTextContent());
        }

        return equal;
    }
    
    private String updateConditionItem(QName conditionItem, Element path) {
        //todo fix with mapping
        StringBuilder item = new StringBuilder();
        
        if (path != null && StringUtils.isNotEmpty(path.getTextContent())) {
            XPathHolder holder = new XPathHolder(path);
            PropertyPath propertyPath = holder.toPropertyPath();
            
            String alias = getContext().getAlias(propertyPath);
            if (StringUtils.isNotEmpty(alias)) {
                item.append(alias);
            }
        }
        
        if (item.length() != 0) {
            item.append(".");
        }
        item.append(conditionItem.getLocalPart());

        return item.toString();
    }

    //todo createCriteria for path items with aliases, also save these in some map as <path, prefix>
    //check path with some schema stuff as well as check it against query annotations in data.common package
    //add this mappings to query context...
    private void updateQueryContext(Element path) throws QueryInterpreterException {
        XPathHolder holder = new XPathHolder(path);
        List<XPathSegment> segments = holder.toSegments();

        List<PropertyPathSegment> propPathSegments = new ArrayList<PropertyPathSegment>();
        PropertyPath lastPropPath = null;
        PropertyPath propPath;
        for (XPathSegment segment : segments) {
            QName qname = segment.getQName();
            propPathSegments.add(new PropertyPathSegment(qname));
            
            propPath = new PropertyPath(propPathSegments);
            
            Criteria pCriteria = getContext().getCriteria(lastPropPath);
            Criteria criteria = pCriteria.createCriteria(qname.getLocalPart(), 
                    Character.toString(qname.getLocalPart().charAt(0))); 
            getContext().setCriteria(propPath, criteria);   
            getContext().setAlias(propPath, createAlias(propPath.last()));
        }
    }
    
    private String createAlias(PropertyPathSegment segment) {
        //todo reimplement
        return Character.toString(segment.getName().getLocalPart().charAt(0));
    }
}
