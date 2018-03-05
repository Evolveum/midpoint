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

import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author semancik
 *
 */
public class NameItemPathSegment extends ItemPathSegment {

	public static final NameItemPathSegment WILDCARD = NameItemPathSegment.createWildcard();

	@NotNull private final QName name;
	private boolean isVariable = false;

	public NameItemPathSegment(@NotNull QName name) {
		this.name = name;
	}

	private static NameItemPathSegment createWildcard() {
		NameItemPathSegment segment = new NameItemPathSegment(new QName("*"));		// TODO
		segment.setWildcard(true);
		return segment;
	}

	public NameItemPathSegment(@NotNull QName name, boolean isVariable) {
		this.name = name;
		this.isVariable = isVariable;
	}

	@NotNull
	public QName getName() {
		return name;
	}

	@Override
	public boolean isVariable() {
		return isVariable;
	}

	@Override
	public String toString() {
		return (isVariable ? "$" : "") + (isWildcard() ? "*" : DebugUtil.formatElementName(name));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (isVariable ? 1231 : 1237);
        // if we need to compute hash from namespace-normalized name, we would use this one:
        // (in order for equals to work; if we decide to change equals in such a way later)
		// result = prime * result + ((name == null) ? 0 : name.getLocalPart().hashCode());

        // this version is for "precise" equals
        result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

    /**
     * More strict version of comparison: it requires exact matching of QNames (e.g. x:xyz and xyz are different in this respect).
     *
     * @param obj
     * @return
     */
	@Override
    public boolean equals(Object obj) {
        return equals(obj, false, false);
    }

    /**
     * Less strict version of comparison: it allows unqualified names to match fully qualified ones (e.g. x:xyz and xyz are the same).
     *
     * @param obj
     * @return
     */

    @Override
    public boolean equivalent(Object obj) {
        return equals(obj, true, true);
    }

	public boolean equals(Object obj, boolean allowUnqualified, boolean allowDifferentPrefixes) {
		if (this == obj) {
			return true;
        }
        if (!super.equals(obj)) {
			return false;
        }
        if (getClass() != obj.getClass()) {
			return false;
        }
        NameItemPathSegment other = (NameItemPathSegment) obj;
		if (isVariable != other.isVariable) {
			return false;
        }
        if (name == null) {
			return other.name != null;
		}

        if (allowUnqualified) {
            if (!allowDifferentPrefixes) {
                throw new UnsupportedOperationException("It is not possible to disallow different prefixes while allowing unqualified names");
            }
			return QNameUtil.match(name, other.name);
		} else {
		    if (!name.equals(other.name)) {         // compares namespace and local part
			    return false;
            }
            // in order to differentiate between x:name and name (when x is undefined) we will compare the prefixes as well
            if (!allowDifferentPrefixes && !normalizedPrefix(name).equals(normalizedPrefix(other.name))) {
                return false;
            }
            return true;
        }
	}

    private String normalizedPrefix(QName name) {
        if (name.getPrefix() == null) {
            return "";
        } else {
            return name.getPrefix();
        }
    }

    @Override
    public NameItemPathSegment clone() {
        NameItemPathSegment clone = new NameItemPathSegment(this.name, this.isVariable);
        clone.setWildcard(this.isWildcard());
        return clone;
    }

}
