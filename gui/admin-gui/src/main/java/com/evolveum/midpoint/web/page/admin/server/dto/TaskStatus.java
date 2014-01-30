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
package com.evolveum.midpoint.web.page.admin.server.dto;

import com.evolveum.midpoint.xml.ns._public.common.common_2a.OperationResultStatusType;

/**
 *  @author shood
 * */
public enum TaskStatus {

    UNKNOWN("fa fa-fw fa-question-circle fa-lg text-warning"),
    SUCCESS("fa fa-fw fa-check-circle fa-lg text-success"),
    WARNING("fa fa-fw fa-exclamation-circle fa-lg text-warning"),
    PARTIAL_ERROR("fa fa-fw fa-minus-circle fa-lg text-danger"),
    FATAL_ERROR("fa fa-fw fa-times-circle fa-lg text-danger"),
    HANDLED_ERROR("fa fa-fw fa-minus-circle fa-lg text-warning"),
    NOT_APPLICABLE("fa fa-fw fa-check-circle fa-lg text-muted"),
    IN_PROGRESS("fa fa-fw fa-clock-o fa-lg text-info");

    private String icon;

    private TaskStatus(String icon){
        this.icon = icon;
    }

    public String getIcon(){
        return icon;
    }

    public static TaskStatus parseOperationalResultStatus(OperationResultStatusType statusType){
        if (statusType == null) {
            return UNKNOWN;
        }

        switch (statusType) {
            case FATAL_ERROR:
                return FATAL_ERROR;
            case PARTIAL_ERROR:
                return PARTIAL_ERROR;
            case HANDLED_ERROR:
                return HANDLED_ERROR;
            case SUCCESS:
                return SUCCESS;
            case WARNING:
                return WARNING;
            case NOT_APPLICABLE:
                return NOT_APPLICABLE;
            case IN_PROGRESS:
                return IN_PROGRESS;
            default:
                return UNKNOWN;
        }
    }
}
