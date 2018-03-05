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

/**
 * @author semancik
 *
 */
public class IdItemPathSegment extends ItemPathSegment {

	public static final IdItemPathSegment WILDCARD = IdItemPathSegment.createWildcard();

	private Long id;

	public IdItemPathSegment() {
		this.id = null;
	}

	private static IdItemPathSegment createWildcard() {
		IdItemPathSegment segment = new IdItemPathSegment();
		segment.setWildcard(true);
		return segment;
	}

	public IdItemPathSegment(Long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}

	@Override
	public String toString() {
		return "[" + ( isWildcard() ? "*" : id ) + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		IdItemPathSegment other = (IdItemPathSegment) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

    @Override
    public boolean equivalent(Object obj) {
        return equals(obj);
    }

    @Override
    public IdItemPathSegment clone() {
        IdItemPathSegment clone = new IdItemPathSegment();
        clone.id = this.id;
        return clone;
    }

}
