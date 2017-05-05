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
public enum RReferenceOwner {

    OBJECT_PARENT_ORG,          // 0

    USER_ACCOUNT,               // 1

    RESOURCE_BUSINESS_CONFIGURATON_APPROVER,    // 2

    ROLE_APPROVER,              // 3

    /**
     * @deprecated
     */
    @Deprecated
    SYSTEM_CONFIGURATION_ORG_ROOT,  // 4

    CREATE_APPROVER,            // 5

    MODIFY_APPROVER,            // 6

    INCLUDE,                    // 7

    ROLE_MEMBER,                // 8

    DELEGATED,                // 9

    PERSONA                     // 10
}
