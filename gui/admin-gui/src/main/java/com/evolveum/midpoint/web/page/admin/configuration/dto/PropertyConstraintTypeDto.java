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

import com.evolveum.midpoint.xml.ns._public.common.common_3.PropertyConstraintType;

import java.io.Serializable;

/**
 *  @author shood
 * */
public class PropertyConstraintTypeDto implements Serializable{

 	private static final long serialVersionUID = 1L;
 	
	public static final String F_PROPERTY_PATH = "propertyPath";
    public static final String F_OID_BOUND = "oidBound";

    private String propertyPath;
    private boolean oidBound;

    public PropertyConstraintTypeDto(PropertyConstraintType property){
        if(property != null){
            if(property.getPath() != null && property.getPath().getItemPath() != null){
                propertyPath = property.getPath().getItemPath().toString();
            }

            if(property.isOidBound() != null){
                oidBound = property.isOidBound();
            }
        }
    }

    public String getPropertyPath() {
        return propertyPath;
    }

    public void setPropertyPath(String propertyPath) {
        this.propertyPath = propertyPath;
    }

    public boolean isOidBound() {
        return oidBound;
    }

    public void setOidBound(boolean oidBound) {
        this.oidBound = oidBound;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PropertyConstraintTypeDto)) return false;

        PropertyConstraintTypeDto that = (PropertyConstraintTypeDto) o;

        if (oidBound != that.oidBound) return false;
        if (propertyPath != null ? !propertyPath.equals(that.propertyPath) : that.propertyPath != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = propertyPath != null ? propertyPath.hashCode() : 0;
        result = 31 * result + (oidBound ? 1 : 0);
        return result;
    }
}
