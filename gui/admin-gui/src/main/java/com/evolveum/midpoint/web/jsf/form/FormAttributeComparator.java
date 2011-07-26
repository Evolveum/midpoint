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
package com.evolveum.midpoint.web.jsf.form;

import java.io.Serializable;
import java.util.Comparator;

/**
 * 
 * @author lazyman
 *
 */
public class FormAttributeComparator implements Comparator<FormAttribute>, Serializable {

	private static final long serialVersionUID = -3537350724118181063L;

	@Override
	public int compare(FormAttribute o1, FormAttribute o2) {
		FormAttributeDefinition def1 = o1.getDefinition();
		FormAttributeDefinition def2 = o2.getDefinition();

		if (isMandatory(def1) && !isMandatory(def2)) {
			return -1;
		} else if (!isMandatory(def1) && isMandatory(def2)) {
			return 1;
		}

		return String.CASE_INSENSITIVE_ORDER.compare(def1.getDisplayName(), def2.getDisplayName());
	}

	private boolean isMandatory(FormAttributeDefinition def) {
		return def.getMinOccurs() > 0;
	}
}
