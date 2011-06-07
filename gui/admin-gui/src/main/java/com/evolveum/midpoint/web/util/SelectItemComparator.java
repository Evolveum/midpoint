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
package com.evolveum.midpoint.web.util;

import java.io.Serializable;
import java.util.Comparator;

import javax.faces.model.SelectItem;

import org.apache.commons.lang.StringUtils;

/**
 * 
 * @author lazyman
 *
 */
public class SelectItemComparator implements Comparator<SelectItem>, Serializable {

	private static final long serialVersionUID = -5008123879560484515L;

	@Override
	public int compare(SelectItem o1, SelectItem o2) {
		String label1 = StringUtils.isEmpty(o1.getLabel()) ? o1.getValue().toString() : o1.getLabel();
		String label2 = StringUtils.isEmpty(o2.getLabel()) ? o2.getValue().toString() : o2.getLabel();

		return String.CASE_INSENSITIVE_ORDER.compare(label1, label2);
	}
}
