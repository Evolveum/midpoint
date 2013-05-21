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
package com.evolveum.midpoint.model.migrator;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;

/**
 * @author semancik
 *
 */
public class TypedPrismObject<O extends ObjectType> {
	
	private Class<O> type;
	private PrismObject<O> object;
	
	public TypedPrismObject(Class<O> type, PrismObject<O> object) {
		super();
		this.object = object;
		this.type = type;
	}

	public PrismObject<O> getObject() {
		return object;
	}

	public void setObject(PrismObject<O> object) {
		this.object = object;
	}

	public Class<O> getType() {
		return type;
	}

	public void setType(Class<O> type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return "TypedPrismObject(" + type + ": " + object + ")";
	}
	
}
