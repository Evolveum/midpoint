/*
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
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.model.test.util.equal;

import java.util.List;
import javax.xml.namespace.QName;

/**
 * 
 * @author lazyman
 */
public abstract class Equals<T> {

	public abstract boolean areEqual(T o1, T o2);

	public static boolean areBooleanEqual(boolean aThis, boolean aThat) {
		return aThis == aThat;
	}

	static public boolean areCharEqual(char aThis, char aThat) {
		return aThis == aThat;
	}

	public static boolean areLongEqual(long aThis, long aThat) {
		return aThis == aThat;
	}

	public static boolean areFloatEqual(float aThis, float aThat) {
		return Float.floatToIntBits(aThis) == Float.floatToIntBits(aThat);
	}

	public static boolean areDoubleEqual(double aThis, double aThat) {
		return Double.doubleToLongBits(aThis) == Double.doubleToLongBits(aThat);
	}

	public static boolean areStringEqual(String aThis, String aThat) {
		return aThis == null ? aThat == null : aThis.equals(aThat);
	}

	public static boolean areObjectEqual(Object aThis, Object aThat) {
		return aThis == null ? aThat == null : aThis.equals(aThat);
	}

	public static boolean areQNameEqual(QName q1, QName q2) {
		return q1 == null ? q2 == null : q1.equals(q2);
	}

	@SuppressWarnings("rawtypes")
	public static boolean areListsEqual(List l1, List l2) {
		return l1 == null ? l2 == null : l1.equals(l2);
	}

	public static <T> boolean areEqual(T o1, T o2, Equals<T> comparator) {
		if (comparator == null) {
			throw new IllegalArgumentException("Comparator must not be null.");
		}

		boolean equal = o1 == null ? o2 == null : o2 != null;
		if (!equal) {
			return false;
		}

		return comparator.areEqual(o1, o2);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static boolean areListsEqual(List l1, List l2, Equals comparator) {
		if (comparator == null) {
			throw new IllegalArgumentException("Comparator must not be null.");
		}

		boolean equal = l1 == null ? l2 == null : l2 != null;
		if (!equal) {
			return false;
		}
		if (l1.size() != l2.size()) {
			return false;
		}

		for (int i = 0; i < l1.size(); i++) {
			if (!comparator.areEqual(l1.get(i), l2.get(i))) {
				return false;
			}
		}

		return true;
	}
}
