/*
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
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.model.filter;

import java.text.Normalizer;
import java.text.Normalizer.Form;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * 
 * @author lazyman
 * 
 */
public class DiacriticsFilter extends AbstractFilter {

	@Override
	public Node apply(Node node) {
		Validate.notNull(node, "Node must not be null.");
		String value = getValue(node);
		if (StringUtils.isEmpty(value)) {
			return node;
		}

		String newValue = Normalizer.normalize(value, Form.NFD).replaceAll(
				"\\p{InCombiningDiacriticalMarks}+", "");

		Node newNode = node.cloneNode(false);
		if (node.getNodeType() == Node.TEXT_NODE) {
			newNode.setTextContent(newValue);
		} else {
			// Element Node
			((Element) newNode).setTextContent(newValue);
		}

		return newNode;
	}
}
