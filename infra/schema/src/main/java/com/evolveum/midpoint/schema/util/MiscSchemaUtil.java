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
 * "Portions Copyrighted 2011 [name of copyright owner]"
 * 
 */
package com.evolveum.midpoint.schema.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import javax.xml.datatype.XMLGregorianCalendar;

import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.ObjectOperationOption;
import com.evolveum.midpoint.schema.ObjectOperationOptions;
import com.evolveum.midpoint.schema.ObjectSelector;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ImportOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectOperationOptionType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectOperationOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectSelectorType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.OperationOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.CachingMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ProtectedStringType;

/**
 * @author Radovan Semancik
 *
 */
public class MiscSchemaUtil {
	
	private static Random rnd = new Random();
	
	public static ObjectListType toObjectListType(List<PrismObject<? extends ObjectType>> list) {
		ObjectListType listType = new ObjectListType();
		for (PrismObject<? extends ObjectType> o : list) {
			listType.getObject().add(o.asObjectable());
		}
		return listType;
	}
	
	public static <T extends ObjectType> List<PrismObject<T>> toList(Class<T> type, ObjectListType listType) {
		List<PrismObject<T>> list = new ArrayList<PrismObject<T>>();
		for (ObjectType o : listType.getObject()) {
			list.add(((T)o).asPrismObject());
		}
		return list;
	}
	
	public static ImportOptionsType getDefaultImportOptions() {
		ImportOptionsType options = new ImportOptionsType();
		options.setOverwrite(false);
		options.setValidateStaticSchema(true);
		options.setValidateDynamicSchema(true);
		options.setEncryptProtectedValues(true);
		options.setFetchResourceSchema(false);
		options.setSummarizeErrors(true);
		options.setSummarizeSucceses(true);
		return options;
	}

	public static CachingMetadataType generateCachingMetadata() {
		CachingMetadataType cmd = new CachingMetadataType();
		XMLGregorianCalendar xmlGregorianCalendarNow = XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis());
		cmd.setRetrievalTimestamp(xmlGregorianCalendarNow);
		cmd.setSerialNumber(generateSerialNumber());
		return cmd;
	}

	private static String generateSerialNumber() {
		return Long.toHexString(rnd.nextLong())+"-"+Long.toHexString(rnd.nextLong());
	}

	public static boolean isNullOrEmpty(ProtectedStringType ps) {
		return (ps == null || (ps.getClearValue() == null && ps.getEncryptedData() == null));
	}

	public static void setPassword(CredentialsType credentials, ProtectedStringType password) {
		PasswordType credPass = credentials.getPassword();
		if (credPass == null) {
			credPass = new PasswordType();
			credentials.setPassword(credPass);
		}
		credPass.setProtectedString(password);
	}

	public static Collection<String> toCollection(String entry) {
		List<String> list = new ArrayList<String>(1);
		list.add(entry);
		return list;
	}

	public static Collection<PropertyPath> itemReferenceListTypeToItemPathList(PropertyReferenceListType resolve) {
		Collection<PropertyPath> itemPathList = new ArrayList<PropertyPath>(resolve.getProperty().size());
		for (Element itemXPathElement: resolve.getProperty()) {
			XPathHolder itemXPath = new XPathHolder(itemXPathElement);
			itemPathList.add(itemXPath.toPropertyPath());
		}
		return itemPathList;
	}

	public static Collection<ObjectOperationOptions> optionsTypeToOptions(OperationOptionsType optionsType) {
		if (optionsType == null) {
			return null;
		}
		List<ObjectOperationOptionsType> objectOptionsTypeList = optionsType.getObjectOption();
		Collection<ObjectOperationOptions> optionsList = new ArrayList<ObjectOperationOptions>(objectOptionsTypeList.size());
		for (ObjectOperationOptionsType objectOptionsType: objectOptionsTypeList) {
			optionsList.add(objectOptionsTypeToOptions(objectOptionsType));
		}
		return optionsList;
	}

	private static ObjectOperationOptions objectOptionsTypeToOptions(ObjectOperationOptionsType objectOptionsType) {
		ObjectSelector selector = selectorTypeToSelector(objectOptionsType.getSelector());
		Collection<ObjectOperationOption> options = optionsTypeToOptions(objectOptionsType.getOption());
		return new ObjectOperationOptions(selector, options );
	}

	private static Collection<ObjectOperationOption> optionsTypeToOptions(List<ObjectOperationOptionType> optionTypeList) {
		Collection<ObjectOperationOption> options = new ArrayList<ObjectOperationOption>(optionTypeList.size());
		for (ObjectOperationOptionType optionType: optionTypeList) {
			options.add(optionTypeToOption(optionType));
		}
		return options;
	}

	private static ObjectOperationOption optionTypeToOption(ObjectOperationOptionType optionType) {
		if (optionType == ObjectOperationOptionType.RESOLVE) {
			return ObjectOperationOption.RESOLVE;
		}
		if (optionType == ObjectOperationOptionType.NO_FETCH) {
			return ObjectOperationOption.NO_FETCH;
		}
		if (optionType == ObjectOperationOptionType.FORCE) {
			return ObjectOperationOption.FORCE;
		}
		throw new IllegalArgumentException("Unknown value "+optionType);
	}

	private static ObjectSelector selectorTypeToSelector(ObjectSelectorType selectorType) {
		if (selectorType == null) {
			return null;
		}
		XPathHolder itemXPath = new XPathHolder(selectorType.getPath());
		return new ObjectSelector(itemXPath.toPropertyPath());
	}

}
