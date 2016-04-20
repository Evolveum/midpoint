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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.web.component.data.column.InlineMenuable;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;

/**
 * @author lazyman
 */
public class SelectableBean<T extends Serializable> extends Selectable implements InlineMenuable{

    public static final String F_VALUE = "value";

    private T value;
    
    private List<InlineMenuItem> menuItems;

    public SelectableBean() {
    }

    public SelectableBean(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }
    
    public List<InlineMenuItem> getMenuItems() {
    	if (menuItems == null) {
    		menuItems = new ArrayList<InlineMenuItem>();
    	}
		return menuItems;
	}
    
    @Override
    public boolean equals(Object obj) {
    	if (!(obj instanceof SelectableBean)){
    		return false;
    	}
    	
    	T object = ((SelectableBean<T>) obj).getValue();
    	return object.equals(value);
    }
    
    @Override
    public int hashCode() {
    	int result = (value != null ? value.hashCode() : 0);
    	return result;
    }
}
