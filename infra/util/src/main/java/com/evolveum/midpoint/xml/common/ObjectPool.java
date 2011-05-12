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

package com.evolveum.midpoint.xml.common;

import java.util.ArrayList;
import java.util.List;

/**
 * Sample Class Doc
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @since 1.0.0
 */
public abstract class ObjectPool<T> {

    public static final String code_id = "$Id$";

	private List<T> locked, unlocked;
	
	public ObjectPool() {
		locked = new ArrayList<T>();
		unlocked = new ArrayList<T>();
	}
	
	public ObjectPool(int initialSize) {
		this();
		
		for (int i = 0; i < initialSize; i++) {
			unlocked.add(create());
		}
	}
	
	public synchronized void dispose() {
		List<T> disposeList = new ArrayList<T>();
		disposeList.addAll(locked);
		disposeList.addAll(unlocked);
		locked.clear();
		unlocked.clear();
		for (T pooled : disposeList) {
			pooled = null;
		}
		
		disposeList.clear();
	}
	
//	protected void disposeObject(T pooled) {
//		pooled = null;
//	}

	/**
	 * Creates a new pool member.
	 * 
	 * @return
	 */
	protected abstract T create();
	
	/**
	 * Checkout a pooled object, or create one if none are available.
	 * 
	 * @return
	 */
	public synchronized T checkout() {
		T pooled;
		if (unlocked.size() > 0) {
			pooled = unlocked.remove(0);
			locked.add(pooled);
			return pooled;
		}
		
		// no object available, create one
		pooled = create();
		locked.add(pooled);
		return pooled;
	}
	
	/**
	 * Check-in a pooled object to the object pool.
	 * 
	 * @param pooled
	 */
	public synchronized void checkin(T pooled) {
		locked.remove(pooled);
		unlocked.add(pooled);
	}
	
	public int numLocked() {
		return locked.size();
	}
	
	public int numUnlocked() {
		return unlocked.size();
	}

}
