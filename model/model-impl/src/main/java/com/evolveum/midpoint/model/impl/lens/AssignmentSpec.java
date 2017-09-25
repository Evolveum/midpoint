/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.model.impl.lens;

import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Objects;

/**
 * A key for assignment:mode => modifications map (for policy state).
 *
 * @author mederly
 */
public class AssignmentSpec implements Serializable {

	@NotNull public final AssignmentType assignment;
	@NotNull public final PlusMinusZero mode;

	public AssignmentSpec(@NotNull AssignmentType assignment, @NotNull PlusMinusZero mode) {
		this.assignment = assignment;
		this.mode = mode;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof AssignmentSpec))
			return false;
		AssignmentSpec that = (AssignmentSpec) o;
		return Objects.equals(assignment, that.assignment) && mode == that.mode;
	}

	@Override
	public int hashCode() {
		return Objects.hash(assignment, mode);
	}

	@Override
	public String toString() {
		return mode + ":" + assignment;
	}
}
