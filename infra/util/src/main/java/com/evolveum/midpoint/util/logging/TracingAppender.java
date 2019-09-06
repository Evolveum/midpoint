/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
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

	public static void terminateCollecting() {
		eventsThreadLocal.remove();
	}

	public static void openSink(LoggingEventCollector collector) {
		LoggingEventSink currentSink = eventsThreadLocal.get();
		if (currentSink != null) {
			currentSink.collectEvents();
		}
		eventsThreadLocal.set(new LoggingEventSink(collector, currentSink));
	}

	public static void closeCurrentSink() {
		LoggingEventSink currentSink = eventsThreadLocal.get();
		if (currentSink != null) {
			currentSink.collectEvents();
			eventsThreadLocal.set(currentSink.getParent());
		}
	}
}
