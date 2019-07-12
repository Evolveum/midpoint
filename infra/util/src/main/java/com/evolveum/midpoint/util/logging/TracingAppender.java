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

import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.Layout;
import org.apache.commons.lang.StringUtils;

/**
 *  Used to collect log entries for tracing purposes.
 */
public class TracingAppender<E> extends AppenderBase<E> {

	private Layout<E> layout;

	private static ThreadLocal<LoggingEventSink> eventsThreadLocal = new ThreadLocal<>();

	@Override
	protected void append(E eventObject) {
		LoggingEventSink loggingEventSink = eventsThreadLocal.get();
		if (loggingEventSink != null) {
			String text = layout.doLayout(eventObject);
			String normalized = StringUtils.removeEnd(text, "\n");
			loggingEventSink.add(normalized);
		}
	}

	public Layout<E> getLayout() {
		return layout;
	}

	public void setLayout(Layout<E> layout) {
		this.layout = layout;
	}

	public static void resetCollectingForCurrentThread() {
		eventsThreadLocal.remove();
	}

	public static void startCollectingForCurrentThread(LoggingEventCollector collector) {
		LoggingEventSink currentSink = eventsThreadLocal.get();
		if (currentSink != null) {
			currentSink.collectEvents();
		}
		eventsThreadLocal.set(new LoggingEventSink(collector, currentSink));
	}

	public static void stopCollectingForCurrentThread() {
		LoggingEventSink currentSink = eventsThreadLocal.get();
		if (currentSink != null) {
			currentSink.collectEvents();
			eventsThreadLocal.set(currentSink.getParent());
		}
	}
}
