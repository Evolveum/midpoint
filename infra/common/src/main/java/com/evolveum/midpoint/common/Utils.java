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

package com.evolveum.midpoint.common;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.List;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.CollectionUtils;
import org.w3c.dom.Node;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.schema.xpath.XPathSegment;
import com.evolveum.midpoint.schema.xpath.XPathHolder;
import com.evolveum.midpoint.util.constants.MidPointConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.schema.SchemaConstants;

/**
 * 
 * @author Igor Farinic
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

	public static String getPropertySilent(Object object, String property) {
		String result = null;
		try {
			result = BeanUtils.getProperty(object, property);
		} catch (IllegalAccessException ex) {
			LOGGER.warn("Failed to get property for instances {}, {}. Error message was {}", new Object[] {
					object.getClass().getName(), property, ex.getMessage() });
		} catch (InvocationTargetException ex) {
			LOGGER.warn("Failed to get property for instances {}, {}. Error message was {}", new Object[] {
					object.getClass().getName(), property, ex.getMessage() });
		} catch (NoSuchMethodException ex) {
			LOGGER.warn("Failed to get property for instances {}, {}. Error message was {}", new Object[] {
					object.getClass().getName(), property, ex.getMessage() });
		}
		return result;
	}

	public static void copyPropertiesSilent(Object target, Object source) {
		try {
			BeanUtils.copyProperties(target, source);

			// copy properties of type List, if target destination does not have
			// setter (e.g. JAXB java object)
			final Field fields[] = target.getClass().getDeclaredFields();
			for (int i = 0; i < fields.length; ++i) {
				if ("List".equals(fields[i].getType().getSimpleName())) {
					ParameterizedType type = (ParameterizedType) fields[i].getGenericType();
					if (null != type && type.getActualTypeArguments().length > 0
							&& "String".equals(((Class) type.getActualTypeArguments()[0]).getSimpleName())) {

						boolean existsSetter = true;
						try {
							Method targetSetterMethod = target.getClass().getDeclaredMethod(
									"set" + StringUtils.capitalise(fields[i].getName()), List.class);
							if (null == targetSetterMethod) {
								existsSetter = false;
							}
						} catch (NoSuchMethodException ex) {
							// if there is setter for the property on target
							// object, then
							existsSetter = false;
						}
						if (!existsSetter) {
							Method targetMethod = target.getClass().getDeclaredMethod(
									"get" + StringUtils.capitalise(fields[i].getName()));
							Method sourceMethod = source.getClass().getDeclaredMethod(
									"get" + StringUtils.capitalise(fields[i].getName()));
							if (null != targetMethod && null != sourceMethod) {
								List<String> targetList = (List) targetMethod.invoke(target);
								List<String> sourceList = (List) sourceMethod.invoke(source);
								if (targetList != null) {
									targetList.clear();
									if (sourceList != null) {
										for (Object str : sourceList) {
											if (str instanceof String) {
												targetList.add((String) str);
											}
										}
									}
								}
							}
						}
					}
				}
			}
		} catch (NoSuchMethodException ex) {
			LOGGER.warn("Failed to copy properties for instances {}, {}. Error message was {}", new Object[] {
					source, target, ex.getMessage() });
		} catch (SecurityException ex) {
			LOGGER.warn("Failed to copy properties for instances {}, {}. Error message was {}", new Object[] {
					source, target, ex.getMessage() });
		} catch (IllegalAccessException ex) {
			LOGGER.warn("Failed to copy properties for instances {}, {}. Error message was {}", new Object[] {
					source, target, ex.getMessage() });
		} catch (InvocationTargetException ex) {
			LOGGER.warn("Failed to copy properties for instances {}, {}. Error message was {}", new Object[] {
					source, target, ex.getMessage() });
		}
	}

	public static boolean haveToResolve(String propertyName, PropertyReferenceListType resolve) {
		for (PropertyReferenceType property : resolve.getProperty()) {
			XPathHolder xpath = new XPathHolder(property.getProperty());
			List<XPathSegment> segments = xpath.toSegments();
			if (CollectionUtils.isEmpty(segments)) {
				continue;
			}

			if (getPropertyName(propertyName).equals(segments.get(0).getQName().getLocalPart())) {
				return true;
			}
		}

		return false;
	}

	public static void unresolveResource(ResourceObjectShadowType shadow) {
		if (shadow == null || shadow.getResource() == null) {
			return;
		}

		ObjectReferenceType reference = new ObjectReferenceType();
		reference.setOid(shadow.getResource().getOid());
		reference.setType(SchemaConstants.I_RESOURCE_TYPE);
		shadow.setResourceRef(reference);
		shadow.setResource(null);
	}

	public static void unresolveResourceForAccounts(List<? extends ResourceObjectShadowType> shadows) {
		for (ResourceObjectShadowType shadow : shadows) {
			unresolveResource(shadow);
		}
	}

	public static PropertyReferenceType fillPropertyReference(String resolve) {
		PropertyReferenceType property = new PropertyReferenceType();
		com.evolveum.midpoint.schema.xpath.XPathHolder xpath = new com.evolveum.midpoint.schema.xpath.XPathHolder(
				Utils.getPropertyName(resolve));
		property.setProperty(xpath.toElement(SchemaConstants.NS_C, "property"));
		return property;
	}

	public static PropertyReferenceListType getResolveResourceList() {
		PropertyReferenceListType resolveListType = new PropertyReferenceListType();
		resolveListType.getProperty().add(Utils.fillPropertyReference("Account"));
		resolveListType.getProperty().add(Utils.fillPropertyReference("Resource"));
		return resolveListType;
	}

	public static String getNodeOid(Node node) {
		Node oidNode = null;
		if ((null == node.getAttributes())
				|| (null == (oidNode = node.getAttributes().getNamedItem(MidPointConstants.ATTR_OID_NAME)))
				|| (StringUtils.isEmpty(oidNode.getNodeValue()))) {
			return null;
		}
		String oid = oidNode.getNodeValue();
		return oid;
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
