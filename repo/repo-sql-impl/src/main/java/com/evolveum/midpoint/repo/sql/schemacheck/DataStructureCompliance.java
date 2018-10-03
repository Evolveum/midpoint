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
 * Compliance of the database structure. (Not regarding schema version in the metadata.)
 *
 * @author mederly
 */
class DataStructureCompliance {

	enum State {
		/**
		 * State is fully compliant (verified by hibernate).
		 */
		COMPLIANT,
		/**
		 * The database is empty - no tables present.
		 */
		NO_TABLES,
		/**
		 *  Some tables exist but the schema is not compliant.
		 */
		NOT_COMPLIANT
	}

	/**
	 * State of the tables.
	 */
	@NotNull final State state;

	/**
	 * Exception that was the result of the validation.
	 */
	final Exception validationException;

	DataStructureCompliance(@NotNull State state, Exception validationException) {
		this.state = state;
		this.validationException = validationException;
	}

	@Override
	public String toString() {
		return "DataStructureCompliance{" +
				"state=" + state +
				", exception=" + validationException +
				'}';
	}
}
