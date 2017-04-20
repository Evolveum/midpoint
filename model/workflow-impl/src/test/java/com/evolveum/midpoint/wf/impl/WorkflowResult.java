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

package com.evolveum.midpoint.wf.impl;

import com.evolveum.midpoint.wf.util.ApprovalUtils;

/**
 * @author mederly
 */
public enum WorkflowResult {

    REJECTED, APPROVED, UNKNOWN, OTHER;

    public static WorkflowResult fromNiceWfAnswer(String answer) {
		return fromWfAnswer(answer);
    }

	private static WorkflowResult fromWfAnswer(String answer) {
		if (answer == null) {
			return UNKNOWN;
		} else {
			Boolean booleanValue = ApprovalUtils.approvalBooleanValueNice(answer);
			if (booleanValue == null) {
				return OTHER;
			} else if (booleanValue) {
				return APPROVED;
			} else {
				return REJECTED;
			}
		}
	}

}
