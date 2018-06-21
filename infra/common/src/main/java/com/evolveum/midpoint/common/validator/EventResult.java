/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.common.validator;

/**
 * @author semancik
 *
 */
public class  EventResult {

	public enum EventResultStatus {

		/**
		 * Continue normal processing.
		 */
		CONTINUE,

		/**
		 * Skip the rest of processing of this object, continue with the next object.
		 */
		SKIP_OBJECT,

		/**
		 * Stop processing.
		 */
		STOP;
	}

	private EventResultStatus status;
	private String reason;

	private EventResult(EventResultStatus status, String reason) {
		super();
		this.status = status;
		this.reason = reason;
	}

	public static EventResult cont() {
		return new EventResult(EventResultStatus.CONTINUE,null);
	}

	public static EventResult skipObject() {
		return new EventResult(EventResultStatus.SKIP_OBJECT,null);
	}

	public static EventResult skipObject(String reason) {
		return new EventResult(EventResultStatus.SKIP_OBJECT, reason);
	}

	public static EventResult stop() {
		return new EventResult(EventResultStatus.STOP,null);
	}

	public static EventResult stop(String reason) {
		return new EventResult(EventResultStatus.STOP,reason);
	}

	public EventResultStatus getStatus() {
		return status;
	}

	public String getReason() {
		return reason;
	}

	public boolean isCont() {
		return status==EventResultStatus.CONTINUE;
	}

	public boolean isStop() {
		return status==EventResultStatus.STOP;
	}

}
