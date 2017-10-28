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
package com.evolveum.midpoint.security.enforcer.api;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author semancik
 *
 */
public class AuthorizationParameters<O extends ObjectType, T extends ObjectType> implements ShortDumpable {
	
	@SuppressWarnings("rawtypes")
	public static final AuthorizationParameters<ObjectType,ObjectType> EMPTY = new AuthorizationParameters<>(null, null, null, null);
	
	private final PrismObject<O> object;
	private final ObjectDelta<O> delta;
	private final PrismObject<T> target;
	private final QName relation;
	
	private AuthorizationParameters(PrismObject<O> object, ObjectDelta<O> delta, PrismObject<T> target, QName relation) {
		super();
		this.object = object;
		this.delta = delta;
		this.target = target;
		this.relation = relation;
	}
	
	public PrismObject<O> getObject() {
		return object;
	}

	public boolean hasObject() {
		return object != null;
	}
	
	public ObjectDelta<O> getDelta() {
		return delta;
	}
	
	public boolean hasDelta() {
		return delta != null;
	}

	public PrismObject<T> getTarget() {
		return target;
	}

	public QName getRelation() {
		return relation;
	}

	@Override
	public String toString() {
		return "AuthorizationParameters(object=" + object + ", delta=" + delta + ", target=" + target
				+ ", relation=" + relation + ")";
	}
	
	@Override
	public void shortDump(StringBuilder sb) {
		shortDumpElement(sb, "object", object);
		shortDumpElement(sb, "delta", delta);
		shortDumpElement(sb, "target", target);
		shortDumpElement(sb, "relation", relation);
		sb.setLength(sb.length() - 2);
	}

	private void shortDumpElement(StringBuilder sb, String label, Object o) {
		if (o != null) {
			sb.append(label).append("=").append(o).append(", ");
		}
	}


	public static class Builder<O extends ObjectType, T extends ObjectType> {
		private PrismObject<O> object;
		private ObjectDelta<O> delta;
		private PrismObject<T> target;
		private QName relation;
		
		public Builder<O,T> object(PrismObject<O> object) {
			this.object = object;
			return this;
		}
		
		public Builder<O,T> delta(ObjectDelta<O> delta) {
			this.delta = delta;
			return this;
		}
		
		public Builder<O,T> target(PrismObject<T> target) {
			this.target = target;
			return this;
		}
		
		public Builder<O,T> relation(QName relation) {
			this.relation = relation;
			return this;
		}
		
		public AuthorizationParameters<O,T> build() {
			return new AuthorizationParameters<>(object, delta, target, relation);
		}
		
		public static <O extends ObjectType> AuthorizationParameters<O,ObjectType> buildObject(PrismObject<O> object) {
			return new AuthorizationParameters<>(object, null, null, null);
		}
		
		public static <O extends ObjectType> AuthorizationParameters<O,ObjectType> buildObjectDelta(PrismObject<O> object, ObjectDelta<O> delta) {
			return new AuthorizationParameters<>(object, delta, null, null);
		}
		
	}
	
}
