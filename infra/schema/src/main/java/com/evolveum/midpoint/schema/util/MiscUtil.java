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

import com.evolveum.midpoint.schema.ResultArrayList;
import com.evolveum.midpoint.schema.ResultList;
import com.evolveum.midpoint.schema.XsdTypeConverter;
import com.evolveum.midpoint.xml.ns._public.common.common_1.CachingMetadata;
import com.evolveum.midpoint.xml.ns._public.common.common_1.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.CredentialsType.Password;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ImportOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ProtectedStringType;

/**
 * @author Radovan Semancik
 *
 */
public class MiscUtil {
	
	private static Random rnd = new Random();
	
	public static ObjectListType toObjectListType(List<? extends ObjectType> list) {
		ObjectListType listType = new ObjectListType();
		for (ObjectType o : list) {
			listType.getObject().add(o);
		}
		return listType;
	}
	
	public static <T extends ObjectType> List<T> toList(Class<T> type, ObjectListType listType) {
		List<T> list = new ArrayList<T>();
		for (ObjectType o : listType.getObject()) {
			list.add((T)o);
		}
		return list;
	}

	public static <T extends ObjectType> ResultList<T> toResultList(Class<T> type, ObjectListType listType) {
		ResultList<T> list = toResultList(type,listType.getObject());
		list.setTotalResultCount(listType.getCount());
		return list;
	}
	
	public static <T extends ObjectType> ResultList<T> toResultList(Class<T> type, List<? extends ObjectType> list) {
		ResultList<T> outList = new ResultArrayList<T>();
		for (ObjectType o : list) {
			outList.add((T)o);
		}
		return outList;
	}
	
	
	
	public static ImportOptionsType getDefaultImportOptions() {
		ImportOptionsType options = new ImportOptionsType();
		options.setOverwrite(false);
		options.setValidateStaticSchema(true);
		options.setValidateDynamicSchema(true);
		options.setEncryptProtectedValues(true);
		options.setFetchResourceSchema(false);
		return options;
	}

	public static CachingMetadata generateCachingMetadata() {
		CachingMetadata cmd = new CachingMetadata();
		XMLGregorianCalendar xmlGregorianCalendarNow = XsdTypeConverter.toXMLGregorianCalendar(System.currentTimeMillis());
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
		Password credPass = credentials.getPassword();
		if (credPass == null) {
			credPass = new Password();
			credentials.setPassword(credPass);
		}
		credPass.setProtectedString(password);
	}

	public static Collection<String> toCollection(String entry) {
		List<String> list = new ArrayList<String>(1);
		list.add(entry);
		return list;
	}

}
