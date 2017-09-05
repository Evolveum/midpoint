/*
 * Copyright (c) 2010-2015 Evolveum
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
package com.evolveum.midpoint.prism.util;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.prism.DefinitionImpl;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.lang.SerializationUtils;

import com.evolveum.midpoint.prism.Definition;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;
import org.springframework.util.ClassUtils;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

/**
 * @author semancik
 *
 */
public class CloneUtil {

	private static final Trace PERFORMANCE_ADVISOR = TraceManager.getPerformanceAdvisorTrace();

	public static <T> T clone(T orig) {
		if (orig == null) {
			return null;
		}
		Class<? extends Object> origClass = orig.getClass();
		if (ClassUtils.isPrimitiveOrWrapper(origClass)) {
			return orig;
		}
		if (origClass.isArray()) {
			return cloneArray(orig);
		}
		if (orig instanceof PolyString) {
			// PolyString is immutable
			return orig;
		}
        if (orig instanceof String) {
            // ...and so is String
            return orig;
        }
		if (orig instanceof QName) {
			// the same here
			return orig;
		}
		if (origClass.isEnum()) {
			return orig;
		}
//        if (orig.getClass().equals(QName.class)) {
//            QName origQN = (QName) orig;
//            return (T) new QName(origQN.getNamespaceURI(), origQN.getLocalPart(), origQN.getPrefix());
//        }
        if (orig instanceof RawType){
			return (T) ((RawType) orig).clone();
		}
		if (orig instanceof Item<?,?>) {
			return (T) ((Item<?,?>)orig).clone();
		}
		if (orig instanceof PrismValue) {
			return (T) ((PrismValue)orig).clone();
		}
		if (orig instanceof ObjectDelta<?>) {
			return (T) ((ObjectDelta<?>)orig).clone();
		}
		if (orig instanceof ObjectDeltaType) {
			return (T) ((ObjectDeltaType) orig).clone();
		}
		if (orig instanceof ItemDelta<?,?>) {
			return (T) ((ItemDelta<?,?>)orig).clone();
		}
		if (orig instanceof DefinitionImpl) {
			return (T) ((DefinitionImpl)orig).clone();
		}
		/*
		 * In some environments we cannot clone XMLGregorianCalendar because of this:
		 * Error when cloning class org.apache.xerces.jaxp.datatype.XMLGregorianCalendarImpl, will try serialization instead.
         * java.lang.IllegalAccessException: Class com.evolveum.midpoint.prism.util.CloneUtil can not access a member of
         * class org.apache.xerces.jaxp.datatype.XMLGregorianCalendarImpl with modifiers "public"
		 */
		if (orig instanceof XMLGregorianCalendar) {
			return (T) XmlTypeConverter.createXMLGregorianCalendar((XMLGregorianCalendar) orig);
		}
		if (orig instanceof Cloneable) {
			T clone = javaLangClone(orig);
			if (clone != null) {
				return clone;
			}
		}
		if (orig instanceof Serializable) {
			// Brute force
			if (PERFORMANCE_ADVISOR.isDebugEnabled()) {
				PERFORMANCE_ADVISOR.debug("Cloning a Serializable ({}). It could harm performance.", orig.getClass());
			}
			return (T)SerializationUtils.clone((Serializable)orig);
		}
		throw new IllegalArgumentException("Cannot clone "+orig+" ("+origClass+")");
	}

	public static <T> List<T> cloneCollectionMembers(Collection<T> collection) {
		if (collection == null) {
			return null;
		}
		List<T> clonedCollection = new ArrayList<>(collection.size());
		for (T element : collection) {
			clonedCollection.add(clone(element));
		}
		return clonedCollection;
	}

	public static <T> List<T> cloneListMembers(List<T> list) {
		List<T> clonedCollection = new ArrayList<>(list.size());
		for (T element : list) {
			clonedCollection.add(clone(element));
		}
		return clonedCollection;
	}

	private static <T> T cloneArray(T orig) {
		int length = Array.getLength(orig);
		T clone = (T) Array.newInstance(orig.getClass().getComponentType(), length);
		System.arraycopy(orig, 0, clone, 0, length);
		return clone;
	}

	public static <T> T javaLangClone(T orig) {
		try {
			Method cloneMethod = orig.getClass().getMethod("clone");
			Object clone = cloneMethod.invoke(orig);
			return (T) clone;
		} catch (NoSuchMethodException|IllegalAccessException|InvocationTargetException|RuntimeException e) {
			if (PERFORMANCE_ADVISOR.isDebugEnabled()) {
				PERFORMANCE_ADVISOR.debug("Error when cloning {}, will try serialization instead.", orig.getClass(), e);
			}
			return null;
		}
	}

}
