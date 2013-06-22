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

package com.evolveum.midpoint.wf.activiti;

import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * Used for testing.
 *
 * @author mederly
 */
public class TestAuthenticationInfoHolder {

    private static UserType userType;

    public static UserType getUserType() {
        return userType;
    }

    public static void setUserType(UserType userType) {
        TestAuthenticationInfoHolder.userType = userType;
    }
}
