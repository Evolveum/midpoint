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

package com.evolveum.midpoint.web.page.admin.configuration.dto;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;

/**
 *  TODO - when mark AEP is implemented, add it here as well
 *
 *  @author shood
 * */
public enum AEPlevel {

    NONE("pageSystemConfiguration.assignmentPolicyEnforcement.value.none"),
    POSITIVE("pageSystemConfiguration.assignmentPolicyEnforcement.value.positive"),
    //MARK("pageSystemConfiguration.assignmentPolicyEnforcement.value.mark"),
    LEGALIZE("pageSystemConfiguration.assignmentPolicyEnforcement.value.legalize"),
    FULL("pageSystemConfiguration.assignmentPolicyEnforcement.value.full");

    private String localizationKey;

    AEPlevel(String localizationKey){
        this.localizationKey = localizationKey;
    }

    public String getLocalizationKey(){
        return localizationKey;
    }

    public static AssignmentPolicyEnforcementType toAEPValueType(AEPlevel level){
        if(level == null){
            return null;
        }

        switch (level){
            case NONE:
                return AssignmentPolicyEnforcementType.NONE;
            case POSITIVE:
                return AssignmentPolicyEnforcementType.POSITIVE;
            case LEGALIZE:
                return AssignmentPolicyEnforcementType.RELATIVE;
            case FULL:
                return AssignmentPolicyEnforcementType.FULL;
            default:
                return null;
        }
    }

    public static AEPlevel fromAEPLevelType(AssignmentPolicyEnforcementType type){
        if(type == null){
            return null;
        }

        switch(type){
            case NONE:
                return AEPlevel.NONE;
            case POSITIVE:
                return AEPlevel.POSITIVE;
            case RELATIVE:
                return AEPlevel.LEGALIZE;
            case FULL:
                return AEPlevel.FULL;
            default:
                return AEPlevel.NONE;
        }
    }
}
