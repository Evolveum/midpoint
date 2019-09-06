/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.util;

import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author Radovan Semancik
 *
 */
public class DomVisitorUtil {

	public static void visitElements(Node node, DomElementVisitor visitor) {
		if (node instanceof Element) {
			visitor.visit((Element)node);
		}
		List<Element> childElements = DOMUtil.listChildElements(node);
		for (Element childElement: childElements) {
			visitElements(childElement, visitor);
		}
	}

}
