/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
