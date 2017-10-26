/**
 * Copyright (c) 2017 Evolveum
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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationLimitationsType;

/**
 * @author semancik
 *
 */
public class AuthorizationLimitationsCollector implements Consumer<Authorization>, Predicate<Authorization> {

	private boolean unlimited = false;
	private List<String> limitActions = new ArrayList<>();
	
	/**
	 * Parsing limitation from the authorization.
	 */
	@Override
	public void accept(Authorization autz) {
		if (unlimited) {
			return;
		}
		AuthorizationLimitationsType limitations = autz.getLimitations();
		if (limitations == null) {
			unlimited = true;
			return;
		}
		List<String> actions = limitations.getAction();
		if (actions.isEmpty()) {
			unlimited = true;
			return;
		}
		limitActions.addAll(actions);
	}

	/**
	 * Deciding whether authorization is acceptable
	 * (based on a value parsed before)
	 */
	@Override
	public boolean test(Authorization autz) {
		if (unlimited) {
			return true;
		}
		// TODO: improve this later. The authorization may be partially applicable.
		for (String autzAction: autz.getAction()) {
			if (!limitActions.contains(autzAction)) {
				return false;
			}
		}
		return true;
	}

}
