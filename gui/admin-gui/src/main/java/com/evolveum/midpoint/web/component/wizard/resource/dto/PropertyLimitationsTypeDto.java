/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.wizard.resource.dto;

import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PropertyAccessType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PropertyLimitationsType;

import java.io.Serializable;

/**
 *  @author shood
 * */
public class PropertyLimitationsTypeDto implements Serializable{

    public static final String F_LIMITATION = "limitationObject";
    public static final String F_SCHEMA = "schema";
    public static final String F_MODEL = "model";
    public static final String F_PRESENTATION = "presentation";

    private PropertyLimitationsType limitationObject;
    private boolean schema = false;
    private boolean model = false;
    private boolean presentation = false;

    public PropertyLimitationsTypeDto(PropertyLimitationsType limitation){
        if(limitation == null){
            limitationObject = new PropertyLimitationsType();
        } else {
            this.limitationObject = limitation;
        }

        if(!limitationObject.getLayer().isEmpty()){
            for(LayerType l: limitationObject.getLayer()){
                if(l.equals(LayerType.MODEL)){
                    model = true;
                } else if(l.equals(LayerType.PRESENTATION)){
                    presentation = true;
                } else if(l.equals(LayerType.SCHEMA)){
                    schema = true;
                }
            }
        }

        if(limitationObject.getAccess() == null){
            limitationObject.setAccess(new PropertyAccessType());
        }
    }

    public PropertyLimitationsType prepareDtoForSave(){
        if (limitationObject == null) {
            limitationObject = new PropertyLimitationsType();
        } else {
            limitationObject.getLayer().clear();
        }

        if (schema) {
            limitationObject.getLayer().add(LayerType.SCHEMA);
        }
        if (model) {
            limitationObject.getLayer().add(LayerType.MODEL);
        }
        if (presentation) {
            limitationObject.getLayer().add(LayerType.PRESENTATION);
        }

        return limitationObject;
    }

    public PropertyLimitationsType getLimitationObject() {
        return limitationObject;
    }

    public void setLimitationObject(PropertyLimitationsType limitationObject) {
        this.limitationObject = limitationObject;
    }

    public boolean isSchema() {
        return schema;
    }

    public void setSchema(boolean schema) {
        this.schema = schema;
    }

    public boolean isModel() {
        return model;
    }

    public void setModel(boolean model) {
        this.model = model;
    }

    public boolean isPresentation() {
        return presentation;
    }

    public void setPresentation(boolean presentation) {
        this.presentation = presentation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PropertyLimitationsTypeDto)) return false;

        PropertyLimitationsTypeDto that = (PropertyLimitationsTypeDto) o;

        if (model != that.model) return false;
        if (presentation != that.presentation) return false;
        if (schema != that.schema) return false;
        if (limitationObject != null ? !limitationObject.equals(that.limitationObject) : that.limitationObject != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = limitationObject != null ? limitationObject.hashCode() : 0;
        result = 31 * result + (schema ? 1 : 0);
        result = 31 * result + (model ? 1 : 0);
        result = 31 * result + (presentation ? 1 : 0);
        return result;
    }
}
