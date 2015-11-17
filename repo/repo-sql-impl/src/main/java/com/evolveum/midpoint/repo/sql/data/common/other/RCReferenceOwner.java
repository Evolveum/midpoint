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

package com.evolveum.midpoint.repo.sql.data.common.other;

/**
 * This is just helper enumeration for different types of reference entities
 * used in many relationships.
 *
 * @author lazyman
 */
public enum RCReferenceOwner {

    CREATE_APPROVER,        // 0

    MODIFY_APPROVER,        // 1

    CASE_REVIEWER;          // 2
}
