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
package com.evolveum.midpoint.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;

/**
 * @author semancik
 *
 */
public class MiscUtil {
	
	public static <T> Collection<T> union(Collection<T>... sets) {
		Set<T> resultSet = new HashSet<T>();
		for (Collection<T> set: sets) {
			if (set != null) {
				resultSet.addAll(set);
			}
		}
		return resultSet;
	}
		
}
