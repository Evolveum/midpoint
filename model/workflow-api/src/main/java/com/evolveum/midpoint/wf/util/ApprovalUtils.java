/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.wf.util;

/**
 * @author mederly
 */
public class ApprovalUtils {
    public static final String DECISION_APPROVED = "__APPROVED__";
    public static final String DECISION_REJECTED = "__REJECTED__";

    public static String approvalStringValue(Boolean approved) {
        if (approved == null) {
            return null;
        } else {
            return approved ? DECISION_APPROVED : DECISION_REJECTED;
        }
    }

    public static Boolean approvalBooleanValue(String decision) {
        if (DECISION_APPROVED.equals(decision)) {
            return true;
        } else if (DECISION_REJECTED.equals(decision)) {
            return false;
        } else {
            return null;
        }
    }

    public static boolean isApproved(String decision) {
        return DECISION_APPROVED.equals(decision);
    }
}
