/*
 * Copyright (c) 2010-2016 Evolveum
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

/**
 *  @author mederly
 */
public enum ApprovalOutcomeIcon {

    UNKNOWN("fa fa-fw fa-question fa-lg text-warning"),
    APPROVED("fa fa-fw fa-check fa-lg text-success"),
    REJECTED("fa fa-fw fa-times fa-lg text-danger"),
    IN_PROGRESS("fa fa-fw fa-clock-o fa-lg text-info");

    private String icon;

    ApprovalOutcomeIcon(String icon){
        this.icon = icon;
    }

    public String getIcon(){
        return icon;
    }

}
