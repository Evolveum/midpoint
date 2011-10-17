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
package com.evolveum.midpoint.model;

import java.util.ArrayList;
import java.util.Collection;

import com.evolveum.midpoint.xml.ns._public.common.common_1.AssignmentType;

/**
 * Describes relative change.
 * 
 * @author Radovan Semancik
 *
 */
public class Delta<T> {

	private DeltaType type;
	private Collection<T> change;

	public Delta() {
		super();
		// Construct list so it will maintain ordering
		// but we don't guarantee that. This is more-or-less just
		// for esthetic reasons (e.g. debug output).
		change = new ArrayList<T>();
	}

	public Delta(DeltaType type) {
		super();
		// Construct list so it will maintain ordering
		// but we don't guarantee that. This is more-or-less just
		// for esthetic reasons (e.g. debug output).
		change = new ArrayList<T>();
		this.type = type;
	}

	public DeltaType getType() {
		return type;
	}

	public void setType(DeltaType type) {
		this.type = type;
	}

	public Collection<T> getChange() {
		return change;
	}
	
	public void add(T object) {
		change.add(object);
	}
	
	@Override
	public String toString() {
		return "Delta(" + type + ",[" + change + "])";
	}

	public enum DeltaType {
		ADD, DELETE;
	}

}
