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

package com.evolveum.midpoint.web.component.wizard.resource.dto;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;

import java.io.Serializable;

/**
 *  @author shood
 * */
public class ObjectClassDetailsDto implements Serializable{

    public static final String F_DISPLAY_NAME = "displayName";
    public static final String F_DESCRIPTION = "description";
    public static final String F_KIND = "kind";
    public static final String F_INTENT = "intent";
    public static final String F_NATIVE_OBJECT_CLASS = "nativeObjectClass";
    public static final String F_IS_DEFAULT = "isDefault";
    public static final String F_IS_KIND_DEFAULT = "isKindDefault";

    private static final String VALUE_NOT_SPECIFIED = " - ";

    private String displayName = VALUE_NOT_SPECIFIED;
    private String description = VALUE_NOT_SPECIFIED;
    private String kind = VALUE_NOT_SPECIFIED;
    private String intent = VALUE_NOT_SPECIFIED;
    private String nativeObjectClass = VALUE_NOT_SPECIFIED;
    private boolean isDefault;

    public ObjectClassDetailsDto(RefinedObjectClassDefinition definition){
        if(definition != null){
            displayName = definition.getDisplayName() != null ? definition.getDisplayName() : VALUE_NOT_SPECIFIED;
            description = definition.getDescription() != null ? definition.getDescription() : VALUE_NOT_SPECIFIED;

            if(definition.getKind() != null){
                kind = definition.getKind().value();
            } else

            intent = definition.getIntent() != null ? definition.getIntent() : VALUE_NOT_SPECIFIED;
            nativeObjectClass = definition.getNativeObjectClass() != null ? definition.getNativeObjectClass() : VALUE_NOT_SPECIFIED;
            isDefault = definition.isDefaultInAKind();
        }
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getKind() {
        return kind;
    }

    public String getIntent() {
        return intent;
    }

    public String getNativeObjectClass() {
        return nativeObjectClass;
    }

    public boolean isDefault() {
        return isDefault;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ObjectClassDetailsDto)) return false;

        ObjectClassDetailsDto that = (ObjectClassDetailsDto) o;

        if (isDefault != that.isDefault) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (displayName != null ? !displayName.equals(that.displayName) : that.displayName != null) return false;
        if (intent != null ? !intent.equals(that.intent) : that.intent != null) return false;
        if (kind != null ? !kind.equals(that.kind) : that.kind != null) return false;
        if (nativeObjectClass != null ? !nativeObjectClass.equals(that.nativeObjectClass) : that.nativeObjectClass != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = displayName != null ? displayName.hashCode() : 0;
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (kind != null ? kind.hashCode() : 0);
        result = 31 * result + (intent != null ? intent.hashCode() : 0);
        result = 31 * result + (nativeObjectClass != null ? nativeObjectClass.hashCode() : 0);
        result = 31 * result + (isDefault ? 1 : 0);
        return result;
    }
}
