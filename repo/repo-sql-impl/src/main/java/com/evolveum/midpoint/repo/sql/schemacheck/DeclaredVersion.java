/*
 * Copyright (c) 2010-2018 Evolveum
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

package com.evolveum.midpoint.repo.sql.schemacheck;

import org.jetbrains.annotations.NotNull;

/**
 * Version of the schema as declared in the metadata.
 *
 * @author mederly
 */
class DeclaredVersion {

	enum State {
		/**
		 * m_global_metadata table is missing (so the schema is unknown)
		 */
		METADATA_TABLE_MISSING,
		/**
		 * metadata table is present but the version value is not specified -- this indicates some corruption of the database
		 */
		VERSION_VALUE_MISSING,
		/**
		 * Everything is OK, version is present in the metadata.
		 */
		VERSION_VALUE_PRESENT,
		/**
		 * Version value was supplied externally (e.g. using configuration)
		 */
		VERSION_VALUE_EXTERNALLY_SUPPLIED
	}

	@NotNull final State state;
	final String version;

	DeclaredVersion(@NotNull State state, String version) {
		this.state = state;
		this.version = version;
	}

	@Override
	public String toString() {
		return "DeclaredVersion{" +
				"state=" + state +
				", version='" + version + '\'' +
				'}';
	}
}
