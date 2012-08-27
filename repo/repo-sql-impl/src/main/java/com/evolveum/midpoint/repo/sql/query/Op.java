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

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.util.DOMUtil;
import org.hibernate.criterion.Criterion;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.util.Arrays;

/**
 * @author lazyman
 */
abstract class Op {

    private QueryInterpreter interpreter;

    public Op(QueryInterpreter interpreter) {
        this.interpreter = interpreter;
    }


    protected QueryInterpreter getInterpreter() {
        return interpreter;
    }

       
    public abstract Criterion interpret(ObjectFilter filter, boolean pushNot) throws QueryException;
}
