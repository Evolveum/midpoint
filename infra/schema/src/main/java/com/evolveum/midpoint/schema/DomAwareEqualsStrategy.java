/**
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.schema;

import java.util.List;

import javax.xml.bind.JAXBElement;

import org.jvnet.jaxb2_commons.lang.JAXBEqualsStrategy;
import org.jvnet.jaxb2_commons.locator.ObjectLocator;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.util.DOMUtil;

/**
 * @author semancik
 *
 */
public class DomAwareEqualsStrategy extends JAXBEqualsStrategy {

	@Override
	protected boolean equalsInternal(ObjectLocator leftLocator,
			ObjectLocator rightLocator, Object lhs, Object rhs) {
		if (lhs instanceof Element && rhs instanceof Element) {
			final Element left = (Element) lhs;
			final Element right = (Element) rhs;
			boolean result = DOMUtil.compareElement(left, right, false);
//			System.out.println("cmp: "+DebugUtil.prettyPrint(left)+"<=>"+DebugUtil.prettyPrint(right)+": "+result);
			return result;
		} else {
			return super.equalsInternal(leftLocator, rightLocator, lhs, rhs);
		}
	}
	
}
