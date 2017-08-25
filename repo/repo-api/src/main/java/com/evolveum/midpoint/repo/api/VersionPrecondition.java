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

package com.evolveum.midpoint.repo.api;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * @author mederly
 */
public class VersionPrecondition<T extends ObjectType> implements ModificationPrecondition<T>, Serializable {

	@NotNull private final String expectedVersion;

	public VersionPrecondition(String expectedVersion) {
		this.expectedVersion = normalize(expectedVersion);
	}

	public VersionPrecondition(@NotNull PrismObject<T> object) {
		this.expectedVersion = normalize(object.getVersion());
	}

	@Override
	public boolean holds(PrismObject<T> object) throws PreconditionViolationException {
		String realVersion = normalize(object.getVersion());
		if (!expectedVersion.equals(realVersion)) {
			throw new PreconditionViolationException("Real version of the object (" + object.getVersion()
					+ ") does not match expected one (" + expectedVersion + ") for " + object);
		}
		return true;
	}

	private String normalize(String version) {
		// this is a bit questionable - actually, null version should not occur here
		return version != null ? version : "0";
	}
}
