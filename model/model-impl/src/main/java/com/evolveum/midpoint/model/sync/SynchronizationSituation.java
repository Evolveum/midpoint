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
package com.evolveum.midpoint.model.sync;

import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.xml.ns._public.common.common_2a.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SynchronizationSituationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * 
 * @author lazyman
 * 
 */
public class SynchronizationSituation {

	private FocusType user;
	private SynchronizationSituationType situation;

	SynchronizationSituation(FocusType user, SynchronizationSituationType situation) {
		Validate.notNull(situation, "Synchronization situation must not be null.");
		this.user = user;
		this.situation = situation;
	}

	public FocusType getUser() {
		return user;
	}

	public SynchronizationSituationType getSituation() {
		return situation;
	}
}
