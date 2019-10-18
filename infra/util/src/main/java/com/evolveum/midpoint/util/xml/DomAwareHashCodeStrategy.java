/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.util.xml;

import org.jvnet.jaxb2_commons.lang.HashCodeStrategy;
import org.jvnet.jaxb2_commons.lang.JAXBHashCodeStrategy;
import org.jvnet.jaxb2_commons.locator.ObjectLocator;
import org.w3c.dom.Element;


/**
 * Strategy for hashCode() methods used in JAXB generated code. The strategy is just returning
 * a constant. This makes the hashing somehow less efficient if the JAXB objects differ just in
 * the DOM parts. This this is quite unlikely under usual circumstances. However the main reason
 * for this is to avoid namespace-related problems.
 *
 * @author Radovan Semancik
 *
 */

public class DomAwareHashCodeStrategy extends JAXBHashCodeStrategy {

    public static HashCodeStrategy INSTANCE = new DomAwareHashCodeStrategy();

    @Override
    protected int hashCodeInternal(ObjectLocator locator, int hashCode, Object value) {
        if (value instanceof Element) {
            // Ignore DOM elements in hashcode.
            return 1;
        } else {
            return super.hashCodeInternal(locator, hashCode, value);
        }
    }

}
