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

package com.evolveum.midpoint.common;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.List;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.CollectionUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.holder.XPathSegment;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;

/**
 * 
 * @author Igor Farinic
 * @author Radovan Semancik
 * 
 */
public class Utils {

	private static final Trace LOGGER = TraceManager.getTrace(Utils.class);

	public static String getPropertyName(String name) {
		if (null == name) {
			return "";
		}
		return StringUtils.lowerCase(name);
	}

	public static Element fillPropertyReference(String resolve) {
		com.evolveum.midpoint.schema.holder.XPathHolder xpath = new com.evolveum.midpoint.schema.holder.XPathHolder(
				Utils.getPropertyName(resolve));
		return xpath.toElement(SchemaConstants.NS_C, "property");
	}

	/**
	 * Removing non-printable UTF characters from the string.
	 * 
	 * This is not really used now. It was done as a kind of prototype for
	 * filters. But may come handy and it in fact tests that the pattern is
	 * doing what expected, so it may be useful.
	 * 
	 * @param bad
	 *            string with bad chars
	 * @return string without bad chars
	 */
	public static String cleanupUtf(String bad) {

		StringBuilder sb = new StringBuilder(bad.length());

		for (int cp, i = 0; i < bad.length(); i += Character.charCount(cp)) {
			cp = bad.codePointAt(i);
			if (isValidXmlCodepoint(cp)) {
				sb.append(Character.toChars(cp));
			}
		}

		return sb.toString();
	}

	/**
	 * According to XML specification, section 2.2:
	 * http://www.w3.org/TR/REC-xml/
	 * 
	 * @param c
	 * @return
	 */
	public static boolean isValidXmlCodepoint(int cp) {
		return (cp == 0x0009 || cp == 0x000a || cp == 0x000d || (cp >= 0x0020 && cp <= 0xd7ff)
				|| (cp >= 0xe000 && cp <= 0xfffd) || (cp >= 0x10000 && cp <= 0x10FFFF));
	}
}
