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

import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.*;

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
    private String tooltipKey;
    private T capability;

    public CapabilityDto(T capability, String value, boolean nativeCapability) {
        this.capability = capability;
        this.value = value;
        this.nativeCapability = nativeCapability;
        this.tooltipKey = determineTooltipKey();
    }

    private String determineTooltipKey(){
        if(capability != null){
            if(capability instanceof ReadCapabilityType){
                return "CapabilityStep.capability.read.tooltip";
            } else if (capability instanceof UpdateCapabilityType){
                return "CapabilityStep.capability.update.tooltip";
            } else if (capability instanceof CreateCapabilityType){
                return "CapabilityStep.capability.create.tooltip";
            } else if (capability instanceof DeleteCapabilityType){
                return "CapabilityStep.capability.delete.tooltip";
            } else if (capability instanceof LiveSyncCapabilityType){
                return "CapabilityStep.capability.liveSync.tooltip";
            } else if (capability instanceof TestConnectionCapabilityType){
                return "CapabilityStep.capability.testConnection.tooltip";
            } else if (capability instanceof ActivationCapabilityType){
                return "CapabilityStep.capability.activation.tooltip";
            } else if (capability instanceof CredentialsCapabilityType){
                return "CapabilityStep.capability.credentials.tooltip";
            } else if (capability instanceof ScriptCapabilityType){
                return "CapabilityStep.capability.script.tooltip";
            }
        }

        return null;
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

    public String getTooltipKey() {
        return tooltipKey;
    }

    public void setTooltipKey(String tooltipKey) {
        this.tooltipKey = tooltipKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CapabilityDto)) return false;

        CapabilityDto that = (CapabilityDto) o;

        if (nativeCapability != that.nativeCapability) return false;
        if (selected != that.selected) return false;
        if (capability != null ? !capability.equals(that.capability) : that.capability != null) return false;
        if (tooltipKey != null ? !tooltipKey.equals(that.tooltipKey) : that.tooltipKey != null) return false;
        if (value != null ? !value.equals(that.value) : that.value != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (selected ? 1 : 0);
        result = 31 * result + (nativeCapability ? 1 : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (tooltipKey != null ? tooltipKey.hashCode() : 0);
        result = 31 * result + (capability != null ? capability.hashCode() : 0);
        return result;
    }
}
