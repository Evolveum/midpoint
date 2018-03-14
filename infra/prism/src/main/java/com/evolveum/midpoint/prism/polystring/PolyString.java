/*
 * Copyright (c) 2010-2018 Evolveum
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
package com.evolveum.midpoint.prism.polystring;

import com.evolveum.midpoint.prism.Matchable;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.Recomputable;
import com.evolveum.midpoint.prism.Structured;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import java.io.Serializable;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;

/**
 * Polymorphic string. String that may have more than one representation at
 * the same time. The primary representation is the original version that is
 * composed of the full Unicode character set. The other versions may be
 * normalized to trim it, normalize character case, normalize spaces,
 * remove national characters or even transliterate the string.
 *
 * PolyString is (almost) immutable. The original value is immutable, but the
 * other normalized values can be changed. However the only way to change them
 * is to recompute them from the original value.
 *
 * @author Radovan Semancik
 */
public class PolyString implements Matchable<PolyString>, Recomputable, Structured, DebugDumpable, ShortDumpable, Serializable, Comparable<Object> {
	private static final long serialVersionUID = -5070443143609226661L;

	public static final QName F_ORIG = new QName(PrismConstants.NS_TYPES, "orig");
	public static final QName F_NORM = new QName(PrismConstants.NS_TYPES, "norm");

	private final String orig;
	private String norm = null;

	public PolyString(String orig) {
		super();
		if (orig == null) {
			throw new IllegalArgumentException("Cannot create PolyString with null orig");
		}
		this.orig = orig;
	}

	public PolyString(String orig, String norm) {
		super();
		if (orig == null) {
			throw new IllegalArgumentException("Cannot create PolyString with null orig");
		}
		this.orig = orig;
		this.norm = norm;
	}

	public String getOrig() {
		return orig;
	}

	public String getNorm() {
		return norm;
	}

	public boolean isEmpty() {
		if (orig == null) {
			return true;
		}
		return orig.isEmpty();
	}

	public void recompute(PolyStringNormalizer normalizer) {
		norm = normalizer.normalize(orig);
	}

	public boolean isComputed() {
		return !(norm == null);
	}

	@Override
	public Object resolve(ItemPath subpath) {
		if (subpath == null || subpath.isEmpty()) {
			return this;
		}
		if (subpath.size() > 1) {
			throw new IllegalArgumentException("Cannot resolve path "+subpath+" on polystring "+this+", the path is too deep");
		}
		if (!(subpath.first() instanceof NameItemPathSegment)) {
			throw new IllegalArgumentException("Cannot resolve non-name path "+subpath+" on polystring "+this);
		}
		QName itemName = ((NameItemPathSegment)subpath.first()).getName();
//		if (F_ORIG.equals(itemName)) {
//			return orig;
//		} else if (F_NORM.equals(itemName)) {
//			return norm;
//		} else {
//			throw new IllegalArgumentException("Unknown path segment "+itemName);
//		}
		if (QNameUtil.match(F_ORIG, itemName)) {
			return orig;
		} else if (QNameUtil.match(F_NORM, itemName)) {
			return norm;
		} else {
			throw new IllegalArgumentException("Unknown path segment "+itemName);
		}
	}

	// Groovy operator overload
	public PolyString plus(Object other) {
		if (other == null) {
			return this;
		}
		return new PolyString(this.orig + other.toString());
	}

	// Groovy operator overload
	public PolyString getAt(int index) {
		return new PolyString(this.orig.substring(index, index+1));
	}

	@Override
	public int compareTo(Object other) {
		if (other == null) {
			return 1;
		}
		String otherString = other.toString();
		return this.orig.compareTo(otherString);
	}

//	public PolyString getAt(Range at) {
//		// TODO
//	}
//
//	public PolyString getAt(IntRange at) {
//		// TODO
//	}

	public int length() {
		return orig.length();
	}

	public PolyString trim() {
		return new PolyString(orig.trim(), norm.trim());
	}

	public String substring(int from, int to) {
		return this.orig.substring(from,to);
	}

	/**
	 * Helper function that checks whether this original string begins with the specified value.
	 *
	 * @param value the value
	 * @return the string
	 */
	public boolean startsWith(String value) {
		return this.orig.startsWith(value);
	}

	/**
	 * Helper function that checks whether this original string ends with the specified value.
	 *
	 * @param value the value
	 * @return the string
	 */
	public boolean endsWith(String value) {
		return this.orig.endsWith(value);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((norm == null) ? 0 : norm.hashCode());
		result = prime * result + ((orig == null) ? 0 : orig.hashCode());
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
		PolyString other = (PolyString) obj;
		if (norm == null) {
			if (other.norm != null)
				return false;
		} else if (!norm.equals(other.norm))
			return false;
		if (orig == null) {
			if (other.orig != null)
				return false;
		} else if (!orig.equals(other.orig))
			return false;
		return true;
	}

	@Override
	public boolean equalsOriginalValue(Recomputable obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PolyString other = (PolyString) obj;
		if (orig == null) {
			if (other.orig != null)
				return false;
		} else if (!orig.equals(other.orig))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return orig;
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("PolyString(");
		sb.append(orig);
		if (norm != null) {
			sb.append(",");
			sb.append(norm);
		}
		sb.append(")");
		return sb.toString();
	}
	
	@Override
	public void shortDump(StringBuilder sb) {
		sb.append(orig);
	}

    public static String getOrig(PolyString s) {
        return s != null ? s.getOrig() : null;
    }

    public static String getOrig(PolyStringType s) {
        return s != null ? s.getOrig() : null;
    }

	@Override
	public boolean match(PolyString other) {
		if (this == other)
			return true;
		if (other == null)
			return false;

		if (norm == null) {
			if (other.norm != null)
				return false;
		} else if (!norm.equals(other.norm))
			return false;
		return true;
	}

	@Override
	public boolean matches(String regex) {
		return Pattern.matches(regex, norm) || Pattern.matches(regex, orig);
	}

	@Override
	public void checkConsistence() {
		if (orig == null) {
			throw new IllegalStateException("Null orig");
		}
		if (norm == null) {
			throw new IllegalStateException("Null norm");
		}
	}

	public static PolyString toPolyString(PolyStringType value) {
		return value != null ? value.toPolyString() : null;
	}

	public static PolyStringType toPolyStringType(PolyString value) {
		return value != null ? new PolyStringType(value) : null;
	}

	public static PolyString fromOrig(String orig) {
		return new PolyString(orig);
	}
}
