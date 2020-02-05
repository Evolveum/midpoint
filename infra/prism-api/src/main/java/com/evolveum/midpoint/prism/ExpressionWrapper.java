/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism;

import java.io.Serializable;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.util.PrettyPrinter;

/**
 * Contains the expression that can be part of e.g. prism filters (or other data).
 */
public class ExpressionWrapper implements Cloneable, Serializable, Freezable {
    private static final long serialVersionUID = 1L;

    /**
     * Name of the expression root element (e.g. "expression").
     */
    private final QName elementName;

    /**
     * Content of the expression.
     * TODO specify more precisely
     */
    private final Object expression;

    public ExpressionWrapper(QName elementName, Object expression) {
        super();
        this.elementName = elementName;
        this.expression = expression;
    }

    public QName getElementName() {
        return elementName;
    }

    public Object getExpression() {
        return expression;
    }

    public ExpressionWrapper clone() {
        // todo call super.clone?
        Object expressionClone = CloneUtil.clone(expression);
        return new ExpressionWrapper(elementName, expressionClone);
    }

    @Override
    public String toString() {
        return "ExpressionWrapper(" + PrettyPrinter.prettyPrint(elementName) + ":" + PrettyPrinter.prettyPrint(expression);
    }

    @Override
    public boolean isImmutable() {
        return (expression instanceof Freezable) && ((Freezable) expression).isImmutable();
    }

    @Override
    public void freeze() {
        if (expression instanceof Freezable) {
            ((Freezable) expression).freeze();
        }
    }
}
