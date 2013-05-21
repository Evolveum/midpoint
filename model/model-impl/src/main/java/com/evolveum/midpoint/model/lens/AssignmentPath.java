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
package com.evolveum.midpoint.model.lens;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentType;

/**
 * @author semancik
 *
 */
public class AssignmentPath {
	
	private List<AssignmentPathSegment> segments;

	public AssignmentPath() {
		segments = createNewSegments();
	}
	
	AssignmentPath(AssignmentType assignmentType) {
		this.segments = createNewSegments();
		segments.add(new AssignmentPathSegment(assignmentType, null));
	}

	private List<AssignmentPathSegment> createNewSegments() {
		return new ArrayList<AssignmentPathSegment>();
	}

	public List<AssignmentPathSegment> getSegments() {
		return segments;
	}
	
	public void add(AssignmentPathSegment segment) {
		segments.add(segment);
	}
	
	public void remove(AssignmentPathSegment segment) {
		segments.remove(segment);
	}

	
	public AssignmentType getFirstAssignment() {
		return segments.get(0).getAssignmentType();
	}
	
	public boolean isEmpty() {
		return segments.isEmpty();
	}
	
	/**
	 * Shallow clone.
	 */
	public AssignmentPath clone() {
		AssignmentPath clone = new AssignmentPath();
		clone.segments.addAll(this.segments);
		return clone;
	}

	@Override
	public String toString() {
		return "AssignmentPath(" + segments + ")";
	}
	
	

}
