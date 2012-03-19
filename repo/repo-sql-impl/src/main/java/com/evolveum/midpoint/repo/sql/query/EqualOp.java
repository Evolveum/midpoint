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
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
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

    @Override
    public void interpret(Element filter, boolean pushNot) {
        if (!canHandle(filter)) {
            throw new IllegalArgumentException("Can't handle filter.....todo");
        }

        List<Element> children = DOMUtil.listChildElements(filter);
        Element value = children.get(0);
        Element element = DOMUtil.listChildElements(value).get(0);
        String fullName = element.getLocalName();

        Criteria criteria = getContext().getCriteria(null);
        if (pushNot) {
            criteria.add(Restrictions.ne(fullName, element.getTextContent()));
        } else {
            criteria.add(Restrictions.eq(fullName, element.getTextContent()));
        }
    }
}
