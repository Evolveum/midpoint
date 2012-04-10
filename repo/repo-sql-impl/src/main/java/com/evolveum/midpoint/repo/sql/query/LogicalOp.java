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
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.hibernate.criterion.*;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * @author lazyman
 */
public class LogicalOp extends Op {

    private static enum Operation {AND, OR, NOT}

    private static final Trace LOGGER = TraceManager.getTrace(LogicalOp.class);

    public LogicalOp(QueryInterpreter interpreter) {
        super(interpreter);
    }

    @Override
    public Criterion interpret(Element filterPart, boolean pushNot) throws QueryException {
        LOGGER.debug("Interpreting '{}', pushNot '{}'",
                new Object[]{DOMUtil.getQNameWithoutPrefix(filterPart), pushNot});
        validate(filterPart);

        Operation operation = getOperationType(filterPart);
        List<Element> elements = DOMUtil.listChildElements(filterPart);
        LOGGER.debug("It's {} with {} sub elements.", new Object[]{operation, elements.size()});

        switch (elements.size()) {
            case 0:
                throw new QueryException("Can't have logical filter '"
                        + DOMUtil.getQNameWithoutPrefix(filterPart) + "' without filter children.");
            case 1:
                boolean newPushNot = pushNot;
                if (Operation.NOT.equals(operation)) {
                    newPushNot = !newPushNot;
                }
                return getInterpreter().interpret(elements.get(0), newPushNot);
            default:
                switch (operation) {
                    case NOT:
                        throw new QueryException("Can't create filter NOT (unary) with more than one element.");
                    case AND:
                        Conjunction conjunction = Restrictions.conjunction();
                        updateJunction(elements, pushNot, conjunction);

                        return conjunction;
                    case OR:
                        Disjunction disjunction = Restrictions.disjunction();
                        updateJunction(elements, pushNot, disjunction);

                        return disjunction;
                }
        }

        throw new QueryException("Unknown state in logical filter.");
    }

    private Junction updateJunction(List<Element> elements, boolean pushNot, Junction junction) throws QueryException {
        for (Element element : elements) {
            if (SchemaConstants.Q_TYPE.equals(DOMUtil.getQNameWithoutPrefix(element))) {
                LOGGER.warn("Unsupported (unused) element '" + SchemaConstants.Q_TYPE + "' in logical filter in query.");
                continue;
            }
            junction.add(getInterpreter().interpret(element, pushNot));
        }

        return junction;
    }

    private Operation getOperationType(Element filterPart) throws QueryException {
        if (DOMUtil.isElementName(filterPart, SchemaConstants.Q_AND)) {
            return Operation.AND;
        } else if (DOMUtil.isElementName(filterPart, SchemaConstants.Q_OR)) {
            return Operation.OR;
        } else if (DOMUtil.isElementName(filterPart, SchemaConstants.Q_OR)) {
            return Operation.NOT;
        }

        throw new QueryException("Unknown filter type '" + DOMUtil.getQNameWithoutPrefix(filterPart) + "'.");
    }

    @Override
    protected QName[] canHandle() {
        return new QName[]{SchemaConstants.Q_AND, SchemaConstants.Q_OR, SchemaConstants.Q_NOT};
    }
}
