/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.prism.delta;

import java.io.File;
import java.io.IOException;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
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
			ObjectDelta<T> objectDelta = new ObjectDelta<T>(newObject.getCompileTimeClass(), ChangeType.ADD, getPrismContext(oldObject, newObject));
			objectDelta.setOid(newObject.getOid());
			objectDelta.setObjectToAdd(newObject);
			return objectDelta;
		} else {
			return oldObject.diff(newObject);
		}
	}

	private static <T extends Objectable> PrismContext getPrismContext(PrismObject<T>... objects) {
		for (PrismObject<T> object: objects) {
			if (object != null) {
				return object.getPrismContext();
			}
		}
		return null;
	}

	public static <T extends Objectable> ObjectDelta<T> diff(T oldObjectType, T newObjectType) throws SchemaException {
		PrismObject<T> oldObject = null;
		if (oldObjectType != null) {
			oldObject = oldObjectType.asPrismObject();
		}
		PrismObject<T> newObject = null;
		if (newObjectType != null) {
			newObject = newObjectType.asPrismObject();
		}
		return diff(oldObject, newObject);
	}


	public static <T extends Objectable> ObjectDelta<T> diff(String oldXml, String newXml, Class<T> type, PrismContext prismContext) throws SchemaException {
		PrismObject<T> oldObject = null;
		if (oldXml != null) {
			oldObject = prismContext.parseObject(oldXml);
		}
		PrismObject<T> newObject = null;
		if (newXml != null) {
			newObject = prismContext.parseObject(newXml);
		}
		return diff(oldObject, newObject);
	}

	public static <T extends Objectable> ObjectDelta<T> diff(File oldXmlFile, File newXmlFile, Class<T> type, PrismContext prismContext) throws SchemaException, IOException {
		PrismObject<T> oldObject = null;
		if (oldXmlFile != null) {
			oldObject = prismContext.parseObject(oldXmlFile);
		}
		PrismObject<T> newObject = null;
		if (newXmlFile != null) {
			newObject = prismContext.parseObject(newXmlFile);
		}
		return diff(oldObject, newObject);
	}

}
