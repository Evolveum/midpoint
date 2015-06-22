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

import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class DebugSearchDto implements Serializable {

    public static final String F_TEXT = "text";
    public static final String F_TYPE = "type";
    public static final String F_RESOURCE_OID = "resource";

    private String text;
    private ObjectTypes type;
    private ObjectViewDto resource;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public ObjectTypes getType() {
        if (type == null) {
            return ObjectTypes.SYSTEM_CONFIGURATION;
        }
        return type;
    }

    public void setType(ObjectTypes type) {
        this.type = type;
    }

    public ObjectViewDto getResource() {
        return resource;
    }

    public void setResource(ObjectViewDto resource) {
        this.resource = resource;
    }
}
