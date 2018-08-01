/**
 * Copyright (c) 2018 Evolveum
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
package com.evolveum.midpoint.test.asserter;

import org.testng.AssertJUnit;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author semancik
 *
 */
public abstract class AbstractAsserter<R> {
	
	private String details;
	private R returnAsserter;
	private PrismContext prismContext;
	private ObjectResolver objectResolver;
	
	public AbstractAsserter() {
		this(null);
	}
	
	public AbstractAsserter(String details) {
		super();
		this.details = details;
	}
	
	public AbstractAsserter(R returnAsserter, String details) {
		super();
		this.returnAsserter = returnAsserter;
		this.details = details;
	}
	
	protected PrismContext getPrismContext() {
		return prismContext;
	}

	public void setPrismContext(PrismContext prismContext) {
		this.prismContext = prismContext;
	}

	protected ObjectResolver getObjectResolver() {
		return objectResolver;
	}

	public void setObjectResolver(ObjectResolver objectResolver) {
		this.objectResolver = objectResolver;
	}

	protected String getDetails() {
		return details;
	}

	protected void fail(String message) {
		AssertJUnit.fail(message);
	}

	protected String descWithDetails(Object o) {
		if (details == null) {
			return o.toString();
		} else {
			return o.toString()+" ("+details+")";
		}
	}
	
	public R end() {
		return returnAsserter;
	}
	
	protected <O extends ObjectType> PrismObject<O> resolveObject(Class<O> type, String oid) throws ObjectNotFoundException, SchemaException {
		if (objectResolver == null) {
			throw new IllegalStateException("Cannot resolve object "+type.getSimpleName()+" "+oid+" because there is no resolver");
		}
		ObjectReferenceType ref = new ObjectReferenceType();
		ref.setOid(oid);
		OperationResult result = new OperationResult("AbstractAsserter.resolveObject");
		O objectType = objectResolver.resolve(ref, type, null, desc(), null, result);
		return (PrismObject<O>) objectType.asPrismObject();
	}
	
	abstract protected String desc();
	
	protected <T> void copySetupTo(AbstractAsserter<T> other) {
		other.setPrismContext(getPrismContext());
		other.setObjectResolver(getObjectResolver());
	}
}
