/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
        this(null, new ArrayList<>(), null);
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
