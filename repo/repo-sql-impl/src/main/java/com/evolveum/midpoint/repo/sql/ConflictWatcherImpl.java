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

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.ConflictWatcher;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author mederly
 */
public class ConflictWatcherImpl implements ConflictWatcher {

	@NotNull private final String oid;
	private boolean initialized;
	private boolean hasConflict;
	private int expectedVersion;
	private boolean objectDeleted;              // skip all future checks

	ConflictWatcherImpl(@NotNull String oid) {
		this.oid = oid;
	}

	<T extends ObjectType> void afterAddObject(@NotNull String oid, @NotNull PrismObject<T> object) {
		if (notRelevant(oid)) {
			return;
		}
		String version = object.getVersion();
		if (version == null) {
			throw new IllegalStateException("No version in object " + object);
		}
		expectedVersion = Integer.parseInt(version);
		initialized = true;
		//System.out.println(Thread.currentThread().getName() + ": afterAddObject: " + this);
	}

	void afterDeleteObject(String oid) {
		if (this.oid.equals(oid)) {
			objectDeleted = true;
		}
	}

	public <T extends ObjectType> void beforeModifyObject(PrismObject<T> object) {
		if (notRelevant(object.getOid())) {
			return;
		}
		checkExpectedVersion(object.getVersion());
	}

	void afterModifyObject(String oid) {
		if (notRelevant(oid)) {
			return;
		}
		expectedVersion++;
		//System.out.println(Thread.currentThread().getName() + ": afterModifyObject: " + this);
	}

	void afterGetVersion(String oid, String currentRepoVersion) {
		if (notRelevant(oid)) {
			return;
		}
		checkExpectedVersion(currentRepoVersion);
	}

	<T extends ObjectType> void afterGetObject(PrismObject<T> object) {
		if (notRelevant(object.getOid())) {
			return;
		}
		checkExpectedVersion(object.getVersion());
	}

	private boolean notRelevant(@NotNull String oid) {
		return objectDeleted || hasConflict || !this.oid.equals(oid);
	}

	private void checkExpectedVersion(@NotNull String currentRepoVersion) {
		int current = Integer.parseInt(currentRepoVersion);
		//System.out.println(Thread.currentThread().getName() + ": checkExpectedVersion: current=" + current + " in " + this);
		if (initialized) {
			if (current != expectedVersion) {
				hasConflict = true;
			}
		} else {
			initialized = true;
			expectedVersion = current;
		}
		//System.out.println(Thread.currentThread().getName() + ": checkExpectedVersion finishing: current=" + current + " in " + this);
	}

	@Override
	@NotNull
	public String getOid() {
		return oid;
	}

	@Override
	public boolean hasConflict() {
		return hasConflict;
	}

	@Override
	public void setExpectedVersion(String version) {
		if (initialized) {
			throw new IllegalStateException("Already initialized: " + this);
		}
		if (StringUtils.isNotEmpty(version)) {
			initialized = true;
			expectedVersion = Integer.parseInt(version);
		}
	}

	@Override
	public String toString() {
		return "ConflictWatcherImpl{" +
				"oid='" + oid + '\'' +
				", initialized=" + initialized +
				", expectedVersion=" + expectedVersion +
				", hasConflict=" + hasConflict +
				", objectDeleted=" + objectDeleted +
				'}';
	}
}
