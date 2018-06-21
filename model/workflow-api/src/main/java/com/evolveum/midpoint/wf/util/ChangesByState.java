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

package com.evolveum.midpoint.wf.util;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.ObjectTreeDeltas;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import org.jetbrains.annotations.NotNull;

/**
 * @author mederly
 */
public class ChangesByState<F extends FocusType> {

	@NotNull
	final private ObjectTreeDeltas<F> applied, beingApplied, waitingToBeApplied, waitingToBeApproved, rejected, canceled;

	public ChangesByState(PrismContext prismContext) {
		applied = new ObjectTreeDeltas<>(prismContext);
		beingApplied = new ObjectTreeDeltas<>(prismContext);
		waitingToBeApplied = new ObjectTreeDeltas<>(prismContext);
		waitingToBeApproved = new ObjectTreeDeltas<>(prismContext);
		rejected = new ObjectTreeDeltas<>(prismContext);
		canceled = new ObjectTreeDeltas<>(prismContext);
	}

	@NotNull
	public ObjectTreeDeltas<F> getApplied() {
		return applied;
	}

	@NotNull
	public ObjectTreeDeltas<F> getBeingApplied() {
		return beingApplied;
	}

	@NotNull
	public ObjectTreeDeltas<F> getWaitingToBeApplied() {
		return waitingToBeApplied;
	}

	@NotNull
	public ObjectTreeDeltas<F> getWaitingToBeApproved() {
		return waitingToBeApproved;
	}

	@NotNull
	public ObjectTreeDeltas<F> getRejected() {
		return rejected;
	}

	@NotNull
	public ObjectTreeDeltas<F> getCanceled() {
		return canceled;
	}
}
