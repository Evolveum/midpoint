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

import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityType;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class CapabilityDto<T extends CapabilityType> implements Serializable {

    public static final String F_NATIVE_CAPABILITY = "nativeCapability";
    public static final String F_VALUE = "value";
    public static final String F_CAPABILITY = "capability";
    public static final String F_SELECTED = "selected";

    private boolean selected = false;
    private boolean nativeCapability;
    private String value;
    private T capability;

    public CapabilityDto(T capability, String value, boolean nativeCapability) {
        this.nativeCapability = nativeCapability;
        this.value = value;
        this.capability = capability;
    }

    public boolean isNativeCapability() {
        return nativeCapability;
    }

    public void setNativeCapability(boolean nativeCapability) {
        this.nativeCapability = nativeCapability;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public T getCapability() {
        return capability;
    }

    public void setCapability(T capability) {
        this.capability = capability;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (!(o instanceof CapabilityDto))
            return false;

        CapabilityDto that = (CapabilityDto) o;

        if (nativeCapability != that.nativeCapability)
            return false;
        if (selected != that.selected)
            return false;
        if (!capability.equals(that.capability))
            return false;
        if (!value.equals(that.value))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (selected ? 1 : 0);
        result = 31 * result + (nativeCapability ? 1 : 0);
        result = 31 * result + value.hashCode();
        result = 31 * result + capability.hashCode();
        return result;
    }
}
