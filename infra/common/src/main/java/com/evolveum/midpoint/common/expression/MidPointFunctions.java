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
	
	private PrismContext prismContext;

	public MidPointFunctions(PrismContext prismContext) {
		super();
		this.prismContext = prismContext;
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
