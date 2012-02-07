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

import org.jvnet.jaxb2_commons.lang.JAXBHashCodeStrategy;
import org.jvnet.jaxb2_commons.locator.ObjectLocator;
import org.w3c.dom.Element;

import com.evolveum.midpoint.schema.util.SchemaDebugUtil;

/**
 * @author semancik
 *
 */
public class DomAwareHashCodeStrategy extends JAXBHashCodeStrategy {

	@Override
	protected int hashCodeInternal(ObjectLocator locator, int hashCode, Object value) {
		// System.out.println("hash: "+DebugUtil.prettyPrint(value));
		if (value instanceof Element) {
			// Ignore DOM elements in hashcode.
			return 1;
		} else {
			return super.hashCodeInternal(locator, hashCode, value);
		}
	}

}
