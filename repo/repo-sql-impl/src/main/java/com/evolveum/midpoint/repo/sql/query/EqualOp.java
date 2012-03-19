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

import com.evolveum.midpoint.schema.SchemaConstants;
import com.evolveum.midpoint.util.DOMUtil;
import org.apache.commons.lang.StringUtils;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;

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
        String conditionItem = condition.getLocalName();    //todo fix with mapping

        Criterion equal;
        if (pushNot) {
            equal = Restrictions.ne(conditionItem, condition.getTextContent());
        } else {
            equal = Restrictions.eq(conditionItem, condition.getTextContent());
        }

        return equal;
    }

    //todo createCriteria for path items with aliases, also save these in some map as <path, prefix>
    //check path with some schema stuff as well as check it against query annotations in data.common package
    //add this mappings to query context...
    private void updateQueryContext(Element path) throws QueryInterpreterException {

    }
}
