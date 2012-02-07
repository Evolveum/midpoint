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
package com.evolveum.midpoint.schema.processor;

import java.io.File;

import javax.xml.bind.JAXBElement;

import com.evolveum.midpoint.schema.delta.ObjectDelta;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * @author semancik
 *
 */
public class DiffUtil {

	public static <T extends ObjectType> ObjectDelta<T> diff(MidPointObject<T> oldObject, MidPointObject<T> newObject) {
		if (oldObject == null) {
			if (newObject == null) {
				return null;
			}
			ObjectTypeUtil.assertConcreteType(newObject.getJaxbClass());
			ObjectDelta<T> objectDelta = new ObjectDelta<T>(newObject.getJaxbClass(), ChangeType.ADD);
			objectDelta.setOid(newObject.getOid());
			objectDelta.setObjectToAdd(newObject);
			return objectDelta;
		} else {
			ObjectTypeUtil.assertConcreteType(oldObject.getJaxbClass());
			return oldObject.compareTo(newObject);
		}
	}

	public static <T extends ObjectType> ObjectDelta<T> diff(T oldObjectType, T newObjectType, Class<T> type, Schema objectSchema) throws SchemaException {
		ObjectDefinition<T> objectDefinition = objectSchema.findObjectDefinition(type);
		MidPointObject<T> oldObject = null;
		if (oldObjectType != null) {
			oldObject = objectDefinition.parseObjectType(oldObjectType);
		}
		MidPointObject<T> newObject = null;
		if (newObjectType != null) {
			newObject = objectDefinition.parseObjectType(newObjectType);
		}
		return diff(oldObject, newObject);
	}

	
	public static <T extends ObjectType> ObjectDelta<T> diff(String oldXml, String newXml, Class<T> type, Schema objectSchema) throws SchemaException {
		ObjectTypeUtil.assertConcreteType(type);
		MidPointObject<T> oldObject = null;
		if (oldXml != null) {
			oldObject = objectSchema.parseObject(oldXml, type);
		}
		MidPointObject<T> newObject = null;
		if (newXml != null) {
			newObject = objectSchema.parseObject(newXml, type);
		}
		return diff(oldObject, newObject);
	}

	public static <T extends ObjectType> ObjectDelta<T> diff(File oldXmlFile, File newXmlFile, Class<T> type, Schema objectSchema) throws SchemaException {
		ObjectTypeUtil.assertConcreteType(type);
		MidPointObject<T> oldObject = null;
		if (oldXmlFile != null) {
			oldObject = objectSchema.parseObject(oldXmlFile, type);
		}
		MidPointObject<T> newObject = null;
		if (newXmlFile != null) {
			newObject = objectSchema.parseObject(newXmlFile, type);
		}
		return diff(oldObject, newObject);
	}

}
