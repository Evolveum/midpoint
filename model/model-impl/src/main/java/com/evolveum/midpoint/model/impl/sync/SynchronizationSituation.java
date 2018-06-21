/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.model.impl.sync;

import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType;

/**
 *
 * @author lazyman
 *
 */
public class SynchronizationSituation<F extends FocusType> {

	private F currentOwner;
	private F correlatedOwner;
	private SynchronizationSituationType situation;

	public SynchronizationSituation(F currentOwner, F correlatedOwner, SynchronizationSituationType situation) {
		Validate.notNull(situation, "Synchronization situation must not be null.");
		this.currentOwner = currentOwner;
		this.correlatedOwner = correlatedOwner;
		this.situation = situation;
	}

	public F getCurrentOwner() {
		return currentOwner;
	}

	public F getCorrelatedOwner() {
		return correlatedOwner;
	}

	public SynchronizationSituationType getSituation() {
		return situation;
	}
}
