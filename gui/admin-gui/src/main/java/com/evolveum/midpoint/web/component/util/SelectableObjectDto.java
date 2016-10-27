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

package com.evolveum.midpoint.web.component.util;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author lazyman
 */
public class SelectableObjectDto<O extends ObjectType> extends Selectable {

    public static final String F_VALUE = "value";

    private String icon;
    private O object;

    public SelectableObjectDto() {
    }

    public SelectableObjectDto(O object) {
        this.object = object;
    }
    
    public SelectableObjectDto(O object, String icon) {
        this.object = object;
        this.icon = icon;
    }

    public O getObject() {
        return object;
    }

    public void setObject(O object) {
        this.object = object;
    }
    
    public String getIcon() {
		return icon;
	}
    
    public void setIcon(String icon) {
		this.icon = icon;
	}
    
   
}
