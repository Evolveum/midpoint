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

package com.evolveum.midpoint.web.component.wizard.resource.dto;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSynchronizationType;

import java.io.Serializable;

/**
 *  @author shood
 * */
public class ObjectSynchronizationTypeDto implements Serializable{

    public static final String F_SELECTED = "selected";
    public static final String F_SYNC_OBJECT = "syncType";

    private boolean selected = false;
    private ObjectSynchronizationType syncType;

    public ObjectSynchronizationTypeDto(){}

    public ObjectSynchronizationTypeDto(ObjectSynchronizationType syncType){
        this.syncType = syncType;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public ObjectSynchronizationType getSyncType() {
        return syncType;
    }

    public void setSyncType(ObjectSynchronizationType syncType) {
        this.syncType = syncType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ObjectSynchronizationTypeDto)) return false;

        ObjectSynchronizationTypeDto that = (ObjectSynchronizationTypeDto) o;

        if (selected != that.selected) return false;
        if (syncType != null ? !syncType.equals(that.syncType) : that.syncType != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (selected ? 1 : 0);
        result = 31 * result + (syncType != null ? syncType.hashCode() : 0);
        return result;
    }
}
