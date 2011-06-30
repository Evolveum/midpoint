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
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.model.filter;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import com.evolveum.midpoint.model.controller.Filter;

/**
 * 
 * @author Igor Farinic
 * 
 */
public abstract class AbstractFilter implements Filter {

	private List<Object> parameters;

	@Override
	public List<Object> getParameters() {
		if (parameters == null) {
			parameters = new ArrayList<Object>();
		}
		return parameters;
	}

	@Override
	public void setParameters(List<Object> parameters) {
		this.parameters = parameters;
	}

	protected String getValue(Node node) {
		String value = null;
		if (node.getNodeType() == Node.TEXT_NODE) {
			value = ((Text) node).getData();
		} else if (node.getNodeType() == Node.ELEMENT_NODE) {
			// Little bit simplistic
			// TODO: look inside the node
			value = ((Element) node).getTextContent();
		} else {
			throw new IllegalArgumentException(
					"PatternFilter can only work with text or element nodes, got node type "
							+ node.getNodeType() + " (" + node + ")");
		}

		return value;
	}
}
