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

import com.evolveum.midpoint.util.DOMUtil;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;

/**
 * @author lazyman
 */
abstract class Op {

    private QueryInterpreter interpreter;

    public Op(QueryInterpreter interpreter) {
        this.interpreter = interpreter;
    }

    public boolean canHandle(Element filter) {
        if (canHandle() == null || filter == null) {
            return false;
        }

        for (QName qname : canHandle()) {
            if (DOMUtil.isElementName(filter, qname)) {
                return true;
            }
        }

        return false;
    }

    protected QueryContext getContext() {
        return interpreter.getContext();
    }

    protected abstract QName[] canHandle();

    public abstract void interpret(Element filter, boolean pushNot);
}
