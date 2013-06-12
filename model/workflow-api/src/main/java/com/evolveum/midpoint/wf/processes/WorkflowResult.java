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

package com.evolveum.midpoint.wf.processes;

/**
 * @author mederly
 */
public enum WorkflowResult {

    REJECTED, APPROVED, UNKNOWN;

    public static WorkflowResult fromBoolean(Boolean answer) {

        if (answer == null) {
            return UNKNOWN;
        } else if (answer) {
            return APPROVED;
        } else {
            return REJECTED;
        }
    }

    public static String fromBooleanAsString(Boolean answer) {
        return String.valueOf(fromBoolean(answer));
    }
}
