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
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * @author lazyman
 */
public class LogicalOp extends Op {

    private static enum Operation {AND, OR, NOT}

    public LogicalOp(QueryInterpreter interpreter) {
        super(interpreter);
    }

    @Override
    public void interpret(Element filterPart, boolean pushNot) {
        if (!canHandle(filterPart)) {
            //todo exception
        }
        Operation operation = getOperationType(filterPart);
        if (operation == null) {
            return;
        }

        List<Element> elements = DOMUtil.listChildElements(filterPart);
        switch (elements.size()) {
            case 0:
                return;
            case 1:
                if (Operation.NOT.equals(operation)) {
                    //todo Restrictions.not(elements.get(0));
                }
                //todo other only interpret
            default:
                if (Operation.NOT.equals(operation)) {
                    //todo throw exception
                }
                //todo do and or on interpretation results
        }
    }

    private Operation getOperationType(Element filterPart) {
        if (DOMUtil.isElementName(filterPart, SchemaConstants.C_AND)) {
            return Operation.AND;
        } else if (DOMUtil.isElementName(filterPart, SchemaConstants.C_OR)) {
            return Operation.OR;
        } else if (DOMUtil.isElementName(filterPart, SchemaConstants.C_NOT)) {
            return Operation.NOT;
        }

        return null;
    }

    @Override
    protected QName[] canHandle() {
        return new QName[]{SchemaConstants.C_AND, SchemaConstants.C_OR, SchemaConstants.C_NOT};
    }
}
