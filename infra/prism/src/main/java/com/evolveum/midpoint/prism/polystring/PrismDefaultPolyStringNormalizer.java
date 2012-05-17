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
package com.evolveum.midpoint.prism.polystring;

import java.text.Normalizer;

import org.apache.commons.lang.StringUtils;

/**
 * @author semancik
 *
 */
public class PrismDefaultPolyStringNormalizer implements PolyStringNormalizer {

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.polystring.PolyStringNormalizer#normalize(java.lang.String)
	 */
	@Override
	public String normalize(String orig) {
		// TODO Auto-generated method stub
		if (orig == null) {
			return null;
		}
		String s = StringUtils.trim(orig);
		s = Normalizer.normalize(s, Normalizer.Form.NFKD);
		s = s.replaceAll("\\s+", " ");
		s = s.replaceAll("[^\\w\\s\\d]", "");
		return StringUtils.lowerCase(s);
	}

}
