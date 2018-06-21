/*
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.model.api.context;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationPolicyDecisionType;

/**
 * Describes what the policy "decides" about a specific account.
 *
 * @author Radovan Semancik
 *
 */
public enum SynchronizationPolicyDecision {

	/**
	 * New account that is going to be added (and linked)
	 */
	ADD,

	/**
	 * Existing account that is going to be deleted (and unlinked)
	 */
	DELETE,

	/**
	 * Existing account that is kept as it is (remains linked).
	 * Note: there still may be attribute or entitlement changes.
	 */
	KEEP,

	/**
	 * Existing account that is going to be unlinked (but NOT deleted)
	 */
	UNLINK,

	/**
	 * The projection is broken. I.e. there is some (fixable) state that
	 * prevents proper operations with the projection. This may be schema violation
	 * problem, security problem (access denied), misconfiguration, etc.
	 * Broken projections will be kept in the state that they are (maintaining
	 * status quo) until the problem is fixed. We will do no operations on broken
	 * projections and we will NOT unlink them or delete them.
	 */
	BROKEN,

	/**
	 * The account is not usable. Context was created, but the account will be skipped.
	 * this is used only for evaluation assigment and the assigment policies
	 */
	IGNORE;

    public SynchronizationPolicyDecisionType toSynchronizationPolicyDecisionType() {
        switch (this) {
            case ADD: return SynchronizationPolicyDecisionType.ADD;
            case DELETE: return SynchronizationPolicyDecisionType.DELETE;
            case KEEP: return SynchronizationPolicyDecisionType.KEEP;
            case UNLINK: return SynchronizationPolicyDecisionType.UNLINK;
            case BROKEN: return SynchronizationPolicyDecisionType.BROKEN;
            default: throw new AssertionError("Unknown value of SynchronizationPolicyDecision: " + this);
        }
    }

    public static SynchronizationPolicyDecision fromSynchronizationPolicyDecisionType(SynchronizationPolicyDecisionType value) {
        if (value == null) {
            return null;
        }
        switch (value) {
            case ADD: return ADD;
            case DELETE: return DELETE;
            case KEEP: return KEEP;
            case UNLINK: return UNLINK;
            case BROKEN: return BROKEN;
            default: throw new AssertionError("Unknown value of SynchronizationPolicyDecisionType: " + value);
        }
    }
}
