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

import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.CapabilityType;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class CapabilityDto<T extends CapabilityType> implements Serializable {

    public static final String F_NATIVE_CAPABILITY = "nativeCapability";
    public static final String F_LABEL = "label";
    public static final String F_CAPABILITY = "capability";

    private boolean nativeCapability;
    private String label;
    private T capability;

    public CapabilityDto(T capability, String label, boolean nativeCapability) {
        this.nativeCapability = nativeCapability;
        this.label = label;
        this.capability = capability;
    }

    public boolean isNativeCapability() {
        return nativeCapability;
    }

    public void setNativeCapability(boolean nativeCapability) {
        this.nativeCapability = nativeCapability;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public T getCapability() {
        return capability;
    }

    public void setCapability(T capability) {
        this.capability = capability;
    }
}
