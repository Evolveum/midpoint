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
package com.evolveum.midpoint.prism.xjc;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;

/**
 * @author semancik
 *
 */
public class JaxbTypeConverter {
	
	public static <T> T mapPropertyRealValueToJaxb(Object propertyRealValue) {
    	// TODO: check for type compatibility
    	if (propertyRealValue instanceof PolyString) {
    		return (T)toPolyStringType((PolyString)propertyRealValue);
    	}
    	return (T)propertyRealValue;
    }
	
	private static PolyStringType toPolyStringType(PolyString polyString) {
		PolyStringType polyStringType = new PolyStringType();
		polyStringType.setOrig(polyString.getOrig());
		polyStringType.setNorm(polyString.getNorm());
		return polyStringType;
	}

	public static Object mapJaxbToPropertyRealValue(Object jaxbObject) {
		if (jaxbObject instanceof PolyStringType) {
			return fromPolyStringType((PolyStringType)jaxbObject);
		}
		return jaxbObject;
	}

	private static PolyString fromPolyStringType(PolyStringType polyStringType) {
		PolyString polyString = new PolyString(polyStringType.getOrig(), polyStringType.getNorm());
		return polyString;
	}

}
