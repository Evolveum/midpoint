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
package com.evolveum.midpoint.web.page.admin.resources.dto;

/**
 * @author lazyman
 */
public enum ResourceStatus {

    SUCCESS("fa fa-fw fa-check-circle fa-lg text-success"),

    WARNING("fa fa-fw fa-exclamation-circle fa-lg text-danger"),

    ERROR("fa fa-fw fa-minus-circle fa-lg text-info"),

    NOT_TESTED("fa fa-fw fa-question-circle fa-lg text-warning");

    private String icon;

    private ResourceStatus(String icon) {
        this.icon = icon;
    }

    public String getIcon() {
        return icon;
    }
}
