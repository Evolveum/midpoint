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
package com.evolveum.midpoint.common.expression;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.polystring.PolyString;

/**
 * Library of standard midPoint functions. These functions are made available to all
 * midPoint expressions.
 * 
 * @author Radovan Semancik
 *
 */
public class MidPointFunctions {
	
	public static final String NAME_SEPARATOR = " ";
	
	private PrismContext prismContext;

	public MidPointFunctions(PrismContext prismContext) {
		super();
		this.prismContext = prismContext;
	}
	
	/**
	 * Convert string to lower case.
	 */
	public static String lc(String orig) {
		return StringUtils.lowerCase(orig);
	}

	/**
	 * Convert string to upper case.
	 */
	public static String uc(String orig) {
		return StringUtils.upperCase(orig);
	}
	
	/**
	 * Remove whitespaces at the beginning and at the end of the string.
	 */
	public static String trim(String orig) {
		return StringUtils.trim(orig);
	}

	/**
	 * Concatenates the arguments to create a name.
	 * Each argument is trimmed and the result is concatenated by spaces.
	 */
	public String concatName(String... components) {
		if (components == null || components.length == 0) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < components.length; i++) {
			String component = components[i];
			if (component == null) {
				continue;
			}
			sb.append(trim(component));
			if (i < (components.length - 1)) {
				sb.append(NAME_SEPARATOR);
			}
		}
		return sb.toString();
	}

	/**
	 * Normalize a string value. It follows the default normalization algorithm
	 * used for PolyString values.
	 * 
	 * @param orig original value to normalize
	 * @return normalized value
	 */
	public String norm(String orig) {
		PolyString polyString = new PolyString(orig);
		polyString.recompute(prismContext.getDefaultPolyStringNormalizer());
		return polyString.getNorm();
	}

}
