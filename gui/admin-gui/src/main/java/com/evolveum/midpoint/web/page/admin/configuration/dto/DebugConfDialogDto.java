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

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lazyman
 */
public class DebugConfDialogDto implements Serializable {

    public enum Operation {
        DELETE_SELECTED, DELETE_ALL_TYPE, DELETE_RESOURCE_SHADOWS;
    }

    private Operation operation;
    private List<DebugObjectItem> objects;
    private Class<? extends ObjectType> type;
    private Map<String, String> resourceFocusMap = new HashMap<>();

    public DebugConfDialogDto(){
        this(null, new ArrayList<DebugObjectItem>(), null);
    }

    public DebugConfDialogDto(Operation operation, List<DebugObjectItem> objects, Class<? extends ObjectType> type) {
        this.operation = operation;
        this.objects = objects;
        this.type = type;
    }

    public Map<String, String> getResourceFocusMap() {
        return resourceFocusMap;
    }

    public void setResourceFocusMap(Map<String, String> resourceFocusMap) {
        this.resourceFocusMap = resourceFocusMap;
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
