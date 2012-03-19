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


import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.ClassMapper;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.w3c.dom.Element;

/**
 * @author lazyman
 */
public class QueryInterpreter {

    private PrismContext prismContext;
    private QueryContext context;
    private Class<? extends ObjectType> type;

    public QueryInterpreter(Session session, Class<? extends ObjectType> type, PrismContext prismContext) {
        this.prismContext = prismContext;
        this.type = type;

        Criteria criteria = session.createCriteria(ClassMapper.getHQLTypeClass(type));
        getContext().setCriteria(null, criteria);
    }

    public Criteria interpret(Element filter) {
        interpret(filter, false);

//        //only example
//        Criteria main = getContext().getCriteria(null);
//
//        Criteria attributes = main.createCriteria("attributes");
//        Criteria stringAttr = attributes.createCriteria("strings");
//        stringAttr.add(Restrictions.eq("elements", "uid=test,dc=example,dc=com"));
//
//        Criteria resourceRef = main.createCriteria("resourceRef");
//        resourceRef.add(Restrictions.eq("targetOid", "0001092019091029301923012"));

        //returning base criteria
        return getContext().getCriteria(null);
    }

    private void interpret(Element filter, boolean pushNot) {
        //todo fix operation choosing and initialization...
        Op operation = new LogicalOp(this);
        if (operation.canHandle(filter)) {
            operation.interpret(filter, pushNot);
        }
        operation = new EqualOp(this);
        if (operation.canHandle(filter)) {
            operation.interpret(filter, pushNot);
        }

        //todo throw exception
    }

    public Class<? extends ObjectType> getType() {
        return type;
    }

    public PrismContext getPrismContext() {
        return prismContext;
    }

    public QueryContext getContext() {
        if (context == null) {
            context = new QueryContext();
        }
        return context;
    }
}
