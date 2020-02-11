/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl;

import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import org.apache.commons.lang.StringUtils;
import org.jvnet.jaxb2_commons.lang.EqualsStrategy;
import org.jvnet.jaxb2_commons.lang.JAXBEqualsStrategy;
import org.jvnet.jaxb2_commons.locator.ObjectLocator;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;

/**
 * Equals strategy that takes namespace prefixes into account.
 * (Used for diff with literal=true.)
 *
 * EXPERIMENTAL
 *
 * @author semancik
 * @author mederly
 *
 */
@Experimental
public class LiteralEqualsStrategy extends JAXBEqualsStrategy {

    public static final EqualsStrategy INSTANCE = new LiteralEqualsStrategy();

    @Override
    protected boolean equalsInternal(ObjectLocator leftLocator,
            ObjectLocator rightLocator, Object lhs, Object rhs) {
//        System.out.println("DomAwareEqualsStrategy: "+PrettyPrinter.prettyPrint(lhs)+"<=>"+PrettyPrinter.prettyPrint(rhs));
        if (lhs instanceof String && rhs instanceof String) {
            // this is questionable (but seems ok)
            return DOMUtil.compareTextNodeValues((String)lhs, (String)rhs);
        } else if (lhs instanceof Element && rhs instanceof Element) {
            // this is perhaps obsolete
            final Element left = (Element) lhs;
            final Element right = (Element) rhs;
            boolean result = DOMUtil.compareElement(left, right, true);
//            System.out.println("cmp: "+PrettyPrinter.prettyPrint(left)+"<=>"+PrettyPrinter.prettyPrint(right)+": "+result);
            return result;
        } else if (lhs instanceof QName && rhs instanceof QName) {
            QName l = (QName) lhs;
            QName r = (QName) rhs;
            if (!l.equals(r)) {
                return false;
            }
            return StringUtils.equals(l.getPrefix(), r.getPrefix());
        } else if (lhs instanceof ItemPathType && rhs instanceof ItemPathType) {
            // ItemPathType's equals is already working literally
            return ((ItemPathType) lhs).equals((ItemPathType) rhs);
        } else {
            return super.equalsInternal(leftLocator, rightLocator, lhs, rhs);
        }
    }

}
