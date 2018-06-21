/**
 * Copyright (c) 2017-2018 Evolveum
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
package com.evolveum.midpoint.security.enforcer.impl;

import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.enforcer.api.PositiveNegativeItemPaths;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Helper class to SecurityEnforcer, used to evaluate item authorizations.
 *
 * @author semancik
 */
public class AutzItemPaths extends PositiveNegativeItemPaths {

	private static final Trace LOGGER = TraceManager.getTrace(AutzItemPaths.class);

	public void collectItems(Authorization autz) {
		collectItemPaths(autz.getItems(), autz.getExceptItems());
	}

}
