/*
 * Copyright (c) 2010-2014 Evolveum
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

import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;

import java.io.Serializable;
import java.util.List;

/**
 * @author lazyman
 */
public class DebugConfDialogDto implements Serializable {

    public static enum Operation {
        DELETE_SELECTED, DELETE_ALL_TYPE, DELETE_ALL_IDENTITIES;
    }

    private Operation operation;
    private List<DebugObjectItem> objects;
    private Class<? extends ObjectType> type;

    public DebugConfDialogDto(Operation operation, List<DebugObjectItem> objects, Class<? extends ObjectType> type) {
        this.operation = operation;
        this.objects = objects;
        this.type = type;
    }

    public Operation getOperation() {
        return operation;
    }

    public List<DebugObjectItem> getObjects() {
        return objects;
    }

    public Class<? extends ObjectType> getType() {
        return type;
    }
}
