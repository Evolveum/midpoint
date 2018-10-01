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
 * An action to be executed against the database schema.
 *
 * @author mederly
 */
abstract class SchemaAction {

	static class None extends SchemaAction {
		// nothing to do
		@Override
		public String toString() {
			return "None{}";
		}
	}

	static class Warn extends SchemaAction {
		@NotNull final String message;              // what to show

		Warn(@NotNull String message) {
			this.message = message;
		}

		@Override
		public String toString() {
			return "Warn{message='" + message + '\'' + '}';
		}
	}

	static class Stop extends SchemaAction {
		@NotNull final String message;              // what to show
		final Exception cause;                      // what cause to put into the exception thrown

		public Stop(@NotNull String message, Exception cause) {
			this.message = message;
			this.cause = cause;
		}

		@Override
		public String toString() {
			return "Stop{message='" + message + '\'' + ", cause=" + cause + '}';
		}
	}

	static class CreateSchema extends SchemaAction {
		@NotNull final String script;

		CreateSchema(@NotNull String script) {
			this.script = script;
		}

		@Override
		public String toString() {
			return "CreateSchema{script='" + script + '\'' + '}';
		}
	}

	static class UpgradeSchema extends SchemaAction {
		@NotNull final String script;
		@NotNull final String from;
		@NotNull final String to;

		UpgradeSchema(@NotNull String script, @NotNull String from, @NotNull String to) {
			this.script = script;
			this.from = from;
			this.to = to;
		}

		@Override
		public String toString() {
			return "UpgradeSchema{script='" + script + '\'' + '}';
		}
	}
}
