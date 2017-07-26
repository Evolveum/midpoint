/**
 * Copyright (c) 2017 Evolveum
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
package com.evolveum.midpoint.web.security;

import java.util.List;

import javax.xml.bind.JAXBElement;

import org.apache.wicket.core.util.objects.checker.IObjectChecker;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * @author semancik
 *
 */
public class MidPointObjectChecker implements IObjectChecker {
	
	private static final Trace LOGGER = TraceManager.getTrace(MidPointObjectChecker.class);
	
	private String label;

	public MidPointObjectChecker() {
		this(null);
	}
	
	public MidPointObjectChecker(String label) {
		super();
		this.label = label;
	}
	
	/* (non-Javadoc)
	 * @see org.apache.wicket.core.util.objects.checker.IObjectChecker#check(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Result check(Object object) {
		
		if (label != null) {
			LOGGER.info("CHECK: {}: {}", label, object);
		}
		
		if (object instanceof PrismObject<?>) {
			return checkObject((PrismObject<? extends ObjectType>)object);
		} else if (object instanceof ObjectType) {
			return checkObject(((ObjectType)object).asPrismObject());
		} else if (object instanceof Document) {
			return new Result( Result.Status.FAILURE, "Storage of DOM documents not allowed: "+object);
//			LOGGER.warn("Attempt to serialize DOM element: {}", DOMUtil.getQName((Element)object));
		} else if (object instanceof Element) {
			return new Result( Result.Status.FAILURE, "Storage of DOM elements not allowed: "+object);
//			LOGGER.warn("Attempt to serialize DOM element: {}", DOMUtil.getQName((Element)object));
			
		// JAXBElement: expression evaluator in expressions, it is JAXBElement even in prism objects
//		} else if (object instanceof JAXBElement) {
//			return new Result( Result.Status.FAILURE, "Storage of JAXB elements not allowed: "+object);
//			LOGGER.warn("Attempt to serialize DOM element: {}", DOMUtil.getQName((Element)object));
		}
		
		return Result.SUCCESS;
	}

	
	
	private <O extends ObjectType> Result checkObject(PrismObject<O> object) {
		
		LOGGER.info("Serializing prism object: {}", object);
		if (object.canRepresent(ResourceType.class)) {
			return new Result( Result.Status.FAILURE, "Storage of ResourceType objects not allowed: "+object);
		}
		
		return Result.SUCCESS;
	}



	/* (non-Javadoc)
	 * @see org.apache.wicket.core.util.objects.checker.IObjectChecker#getExclusions()
	 */
	@Override
	public List<Class<?>> getExclusions() {
		return null;
	}

}
