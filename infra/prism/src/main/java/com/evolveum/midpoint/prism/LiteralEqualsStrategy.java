/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.util.DOMUtil;
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
public class LiteralEqualsStrategy extends JAXBEqualsStrategy {

	public static EqualsStrategy INSTANCE = new LiteralEqualsStrategy();

	@Override
	protected boolean equalsInternal(ObjectLocator leftLocator,
			ObjectLocator rightLocator, Object lhs, Object rhs) {
//		System.out.println("DomAwareEqualsStrategy: "+PrettyPrinter.prettyPrint(lhs)+"<=>"+PrettyPrinter.prettyPrint(rhs));
		if (lhs instanceof String && rhs instanceof String) {
            // this is questionable (but seems ok)
			return DOMUtil.compareTextNodeValues((String)lhs, (String)rhs);
		} else if (lhs instanceof Element && rhs instanceof Element) {
            // this is perhaps obsolete
			final Element left = (Element) lhs;
			final Element right = (Element) rhs;
			boolean result = DOMUtil.compareElement(left, right, true);
//			System.out.println("cmp: "+PrettyPrinter.prettyPrint(left)+"<=>"+PrettyPrinter.prettyPrint(right)+": "+result);
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
