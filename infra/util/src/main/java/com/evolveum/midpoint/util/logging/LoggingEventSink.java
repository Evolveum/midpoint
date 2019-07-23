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

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class LoggingEventSink {

	private final List<LoggedEvent> events = new ArrayList<>();

	private final LoggingEventCollector collector;
	private final LoggingEventSink parent;

	public LoggingEventSink(LoggingEventCollector collector, LoggingEventSink parent) {
		this.collector = collector;
		this.parent = parent;
	}

	public List<LoggedEvent> getEvents() {
		return events;
	}

	public void add(String eventText) {
		events.add(new LoggedEvent(eventText));
	}

	public void collectEvents() {
		if (collector != null) {
			collector.collect(this);
		}
	}

	public LoggingEventSink getParent() {
		return parent;
	}
}
