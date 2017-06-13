/*
 * Copyright (c) 2010-2016 Evolveum
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

package com.evolveum.midpoint.model.api.context;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPathSegmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.jetbrains.annotations.NotNull;

/**
 * Single assignment in an assignment path. In addition to the AssignmentType, it contains resolved target:
 * full object, resolved from targetRef. If targetRef resolves to multiple objects, in the path segment
 * one of them is stored: the one that participates in the particular assignment path.
 *
 * @author semancik
 * @author mederly
 */
public interface AssignmentPathSegment extends DebugDumpable {

	// Returns version of the assignment (old/new) that was evaluated
	AssignmentType getAssignment();

	AssignmentType getAssignment(boolean evaluateOld);

	// Returns 'assignment new' - i.e. the analogous to getAssignment(false)
	// Until 2017-06-13 the name of this method was 'getAssignment()'
	// TODO its use is a bit questionable; it might return null, when evaluating negative-mode assignments
	AssignmentType getAssignmentNew();

	/**
	 * True if the segment corresponds to assignment. False if it's an inducement.
	 */
	boolean isAssignment();

	ObjectType getSource();

	ObjectType getTarget();

	QName getRelation();

	/**
	 *  Whether this assignment/inducement matches the focus level, i.e. if we should collect constructions,
	 *  focus mappings, focus policy rules and similar items from it.
	 */
	boolean isMatchingOrder();

	/**
	 *  Whether this assignment/inducement matches the target level, i.e. if we should collect target
	 *  policy rules from it.
	 */
	boolean isMatchingOrderForTarget();

	/**
	 * True if the relation is a delegation one.
	 */
	boolean isDelegation();

	@NotNull
	AssignmentPathSegmentType toAssignmentPathSegmentType();
}
