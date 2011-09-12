/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
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
