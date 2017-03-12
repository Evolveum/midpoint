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

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPathType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Path from the source object (focus) to the ultimate assignment that is being processed or referenced.
 * The path consists of a chain (list) of segments. Each segment corresponds to a single assignment or inducement.
 * The source of the first segment is the focus. Source of each following segment (i.e. assignment) is the target
 * of previous segment (i.e. assignment).
 *
 * @author semancik
 * @author mederly
 */
public interface AssignmentPath extends DebugDumpable {

	List<? extends AssignmentPathSegment> getSegments();

	AssignmentPathSegment first();

	boolean isEmpty();

	int size();

//	EvaluationOrder getEvaluationOrder();

	AssignmentPathSegment last();

	// beforeLast(0) means last()
	// beforeLast(1) means one before last()
	AssignmentPathSegment beforeLast(int n);

	boolean containsTarget(ObjectType target);

	/**
	 * Returns a "user understandable" part of this path. I.e. only those objects that are of "order 1" above the focal object.
	 * E.g. from chain of
	 *
	 * jack =(a)=> Engineer =(i)=> Employee =(a)=> PersonMetarole =(i2)=> Person =(i)=> Entity
	 *
	 * the result would be
	 *
	 * Engineer -> Employee -> Person -> Entity
	 *
	 * TODO find a better name
	 */
	@NotNull
	List<ObjectType> getFirstOrderChain();

	AssignmentPathType toAssignmentPathType();
}
