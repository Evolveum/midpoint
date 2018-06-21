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
package com.evolveum.midpoint.model.impl.lens;

import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationIntentType;

/**
 * @author semancik
 *
 */
public enum SynchronizationIntent {

	/**
	 * New account that should be added (and linked)
	 */
	ADD,

	/**
	 * Existing account that should be deleted (and unlinked)
	 */
	DELETE,

	/**
	 * Existing account that is kept as it is (remains linked).
	 */
	KEEP,

	/**
	 * Existing account that should be unlinked (but NOT deleted)
	 */
	UNLINK,

	/**
	 * Existing account that belongs to the user and needs to be synchronized.
	 * This may include deleting, archiving or disabling the account.
	 */
	SYNCHRONIZE;

	public SynchronizationPolicyDecision toSynchronizationPolicyDecision() {
		if (this == ADD) {
			return SynchronizationPolicyDecision.ADD;
		}
		if (this == DELETE) {
			return SynchronizationPolicyDecision.DELETE;
		}
		if (this == KEEP) {
			return SynchronizationPolicyDecision.KEEP;
		}
		if (this == UNLINK) {
			return SynchronizationPolicyDecision.UNLINK;
		}
		if (this == SYNCHRONIZE) {
			return null;
		}
		throw new IllegalStateException("Unexpected value "+this);
	}

    public SynchronizationIntentType toSynchronizationIntentType() {
        switch(this) {
            case ADD: return SynchronizationIntentType.ADD;
            case DELETE: return SynchronizationIntentType.DELETE;
            case KEEP: return SynchronizationIntentType.KEEP;
            case UNLINK: return SynchronizationIntentType.UNLINK;
            case SYNCHRONIZE: return SynchronizationIntentType.SYNCHRONIZE;
            default: throw new AssertionError("Unknown value of SynchronizationIntent: " + this);
        }
    }

    public static SynchronizationIntent fromSynchronizationIntentType(SynchronizationIntentType value) {
        if (value == null) {
            return null;
        }
        switch (value) {
            case ADD: return ADD;
            case DELETE: return DELETE;
            case KEEP: return KEEP;
            case UNLINK: return UNLINK;
            case SYNCHRONIZE: return SYNCHRONIZE;
            default: throw new AssertionError("Unknown value of SynchronizationIntentType: " + value);
        }
    }
}
