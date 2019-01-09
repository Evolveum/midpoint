/*
 * Copyright (c) 2010-2018 Evolveum
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

package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.xml.ns._public.common.common_4.ObjectReferenceType;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.Arrays;

/**
 * @author Viliam Repan (lazyman)
 */
public class SearchValue<T extends Serializable> implements DisplayableValue<T>, Serializable {

    private static final long serialVersionUID = 1L;
    
	public static final String F_VALUE = "value";
    public static final String F_LABEL = "label";

    private T value;
    private String label;
    private String displayName;

    public SearchValue() {
        this(null, null);
    }

    public SearchValue(T value) {
        this.value = value;
    }

    public SearchValue(T value, String label) {
        this.label = label;
        this.value = value;
    }

    @Override
    public String getDescription() {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public T getValue() {
        return value;
    }

    @Override
    public String getLabel() {
        //the label for ObjectReferenceType should be reloaded according to the current attributes values
        if (value instanceof ObjectReferenceType) {
            String valueToShow = "";
            ObjectReferenceType ort = (ObjectReferenceType) value;
            if (ort.getOid() != null) {
                valueToShow += "oid=" + ort.getOid() + "/";
            }

            if (ort.getType() != null) {
                valueToShow += "type=" + ort.getType().getLocalPart() +"/";
            }

            if (ort.getRelation() != null && !ort.getRelation().equals(PrismConstants.Q_ANY)) {
                valueToShow += "relation=" + ort.getRelation().getLocalPart();
            }
            return valueToShow;
        }
        if (label == null){
        	if (displayName != null) {
        		return displayName;
        	} else if (value != null){
                return value.toString();
        	}
        }

        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setValue(T value) {
        this.value = value;
        this.label = null;

        if (value instanceof DisplayableValue) {
            DisplayableValue dv = (DisplayableValue) value;
            setLabel(dv.getLabel());
        }
    }

    public String getDisplayName() {
		return displayName;
	}

    public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("label", label)
                .append("value", value)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SearchValue<?> that = (SearchValue<?>) o;

        return !(value != null ? !value.equals(that.value) : that.value != null);

    }

    public void clear() {
        value = null;
        label = null;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{value, label});
    }
}
