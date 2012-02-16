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
package com.evolveum.midpoint.prism.delta;

import java.io.File;

import javax.xml.bind.JAXBElement;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public class DiffUtil {

	public static <T extends Objectable> ObjectDelta<T> diff(PrismObject<T> oldObject, PrismObject<T> newObject) {
		if (oldObject == null) {
			if (newObject == null) {
				return null;
			}
			ObjectDelta<T> objectDelta = new ObjectDelta<T>(newObject.getCompileTimeClass(), ChangeType.ADD);
			objectDelta.setOid(newObject.getOid());
			objectDelta.setObjectToAdd(newObject);
			return objectDelta;
		} else {
			return oldObject.compareTo(newObject);
		}
	}

	public static <T extends Objectable> ObjectDelta<T> diff(T oldObjectType, T newObjectType, Class<T> type, PrismSchema objectSchema) throws SchemaException {
		PrismObjectDefinition<T> objectDefinition = objectSchema.findObjectDefinitionByClass(type);
		PrismObject<T> oldObject = null;
		if (oldObjectType != null) {
			oldObject = objectDefinition.parseObjectType(oldObjectType);
		}
		PrismObject<T> newObject = null;
		if (newObjectType != null) {
			newObject = objectDefinition.parseObjectType(newObjectType);
		}
		return diff(oldObject, newObject);
	}

	
//	public static <T extends Objectable> ObjectDelta<T> diff(String oldXml, String newXml, Class<T> type, PrismContext prismContext) throws SchemaException {
//		ObjectTypeUtil.assertConcreteType(type);
//		PrismObject<T> oldObject = null;
//		if (oldXml != null) {
//			oldObject = prismContext.parseObject(oldXml);
//		}
//		PrismObject<T> newObject = null;
//		if (newXml != null) {
//			newObject = prismContext.parseObject(newXml);
//		}
//		return diff(oldObject, newObject);
//	}
//
//	public static <T extends Objectable> ObjectDelta<T> diff(File oldXmlFile, File newXmlFile, Class<T> type, PrismContext prismContext) throws SchemaException {
//		ObjectTypeUtil.assertConcreteType(type);
//		PrismObject<T> oldObject = null;
//		if (oldXmlFile != null) {
//			oldObject = prismContext.parseObject(oldXmlFile);
//		}
//		PrismObject<T> newObject = null;
//		if (newXmlFile != null) {
//			newObject = prismContext.parseObject(newXmlFile);
//		}
//		return diff(oldObject, newObject);
//	}

}
