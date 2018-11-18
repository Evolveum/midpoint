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
package com.evolveum.midpoint.prism.path;

import java.io.Serializable;

/**
 * @author semancik
 *
 */
public abstract class ItemPathSegment implements Serializable, Cloneable {

	private boolean wildcard = false;

	public boolean isWildcard() {
		return wildcard;
	}

	protected void setWildcard(boolean wildcard) {
		this.wildcard = wildcard;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (wildcard ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ItemPathSegment other = (ItemPathSegment) obj;
		if (wildcard != other.wildcard)
			return false;
		return true;
	}

    public abstract boolean equivalent(Object obj);

    public abstract ItemPathSegment clone();

	public boolean isVariable() {
		return false;
	}
}
