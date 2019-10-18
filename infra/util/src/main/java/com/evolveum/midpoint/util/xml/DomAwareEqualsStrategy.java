/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.util.xml;

import com.evolveum.midpoint.util.PrettyPrinter;
import org.jvnet.jaxb2_commons.lang.JAXBEqualsStrategy;
import org.jvnet.jaxb2_commons.locator.ObjectLocator;
import org.w3c.dom.Element;

import com.evolveum.midpoint.util.DOMUtil;

/**
 * Strategy for equals() methods used in JAXB generated code. The strategy is using our DOMUtil to
 * compare DOM elements. The comparison is quite liberal when it comes to namespaces.
 *
 * @author Radovan Semancik
 *
 */
public class DomAwareEqualsStrategy extends JAXBEqualsStrategy {

    public static DomAwareEqualsStrategy INSTANCE = new DomAwareEqualsStrategy();

    private static final boolean ENABLE_BRUTAL_DEBUGGING = false;       // keep false to avoid unnecessary code execution
    private boolean traceAll = false;
    private boolean traceNotEqual = false;

    public boolean isTraceAll() {
        return traceAll;
    }

    public void setTraceAll(boolean traceAll) {
        this.traceAll = traceAll;
    }

    public boolean isTraceNotEqual() {
        return traceNotEqual;
    }

    public void setTraceNotEqual(boolean traceNotEqual) {
        this.traceNotEqual = traceNotEqual;
    }

    @Override
    protected boolean equalsInternal(ObjectLocator leftLocator,
            ObjectLocator rightLocator, Object lhs, Object rhs) {
        if (ENABLE_BRUTAL_DEBUGGING && traceAll) {
            System.out.println("DomAwareEqualsStrategy: "+ PrettyPrinter.prettyPrint(lhs)+"<=>"+PrettyPrinter.prettyPrint(rhs));
        }
        boolean result;
        if (lhs instanceof String && rhs instanceof String) {
            result = DOMUtil.compareTextNodeValues((String)lhs, (String)rhs);
        } else if (lhs instanceof Element && rhs instanceof Element) {
            final Element left = (Element) lhs;
            final Element right = (Element) rhs;
            result = DOMUtil.compareElement(left, right, false);
        } else {
            result = super.equalsInternal(leftLocator, rightLocator, lhs, rhs);
        }
        if (ENABLE_BRUTAL_DEBUGGING && (traceAll || traceNotEqual && !result)) {
            System.out.println("cmp: "+PrettyPrinter.prettyPrint(lhs)+"<=>"+PrettyPrinter.prettyPrint(rhs)+": "+result);
        }
        return result;
    }

}
