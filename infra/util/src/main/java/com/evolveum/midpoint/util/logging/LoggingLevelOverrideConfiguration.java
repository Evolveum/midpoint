/*
 * Copyright (c) 2010-2019 Evolveum
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

package com.evolveum.midpoint.util.logging;

import ch.qos.logback.classic.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 *
 */
public class LoggingLevelOverrideConfiguration {

	private final List<Entry> entries = new ArrayList<>();

	public List<Entry> getEntries() {
		return entries;
	}

	public void addEntry(Entry entry) {
		entries.add(entry);
	}

	public static class Entry {
		private final Set<String> loggers;
		private final Level level;

		public Entry(Set<String> loggers, Level level) {
			this.loggers = loggers;
			this.level = level;
		}

		public Set<String> getLoggers() {
			return loggers;
		}

		public Level getLevel() {
			return level;
		}

		@Override
		public String toString() {
			return "Entry{" +
					"loggers=" + loggers +
					", level=" + level +
					'}';
		}
	}

	@Override
	public String toString() {
		return "LoggingLevelOverrideConfiguration{" +
				"entries=" + entries +
				'}';
	}
}
