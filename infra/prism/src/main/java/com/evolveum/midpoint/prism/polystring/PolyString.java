/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.prism.polystring;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Dumpable;

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
public class PolyString implements Dumpable, DebugDumpable {

	private String orig;
	private String norm = null;
	
	public PolyString(String orig) {
		super();
		this.orig = orig;
	}
	
	public PolyString(String orig, String norm) {
		super();
		this.orig = orig;
		this.norm = norm;
	}

	public String getOrig() {
		return orig;
	}

	public String getNorm() {
		return norm;
	}
	
	public void recompute(PolyStringNormalizer normalizer) {
		norm = normalizer.normalize(orig);
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
	public String toString() {
		return orig;
	}
	
	@Override
	public String dump() {
		return debugDump();
	}

	@Override
	public String debugDump() {
		return debugDump(0);
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
	
}
