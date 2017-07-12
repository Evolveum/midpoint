/*
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.web.model;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableRowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Application;
import org.apache.wicket.Session;
import org.apache.wicket.core.util.lang.PropertyResolver;
import org.apache.wicket.core.util.lang.PropertyResolverConverter;
import org.apache.wicket.model.AbstractPropertyModel;

/**
 * @author shood
 *
 */
public class LookupPropertyModel<T> extends AbstractPropertyModel<T> {
	private static final long serialVersionUID = 1L;

	protected final String expression;
	protected final LookupTableType lookupTable;
	protected boolean isStrict = true; // if true, allow only values found in lookupTable, false - allow also input that is not in the lookupTable

    public LookupPropertyModel(Object modelObject, String expression, LookupTableType lookupTable) {
        super(modelObject);
        this.expression = expression;
        this.lookupTable = lookupTable;
    }

    public LookupPropertyModel(Object modelObject, String expression, LookupTableType lookupTable, boolean isStrict) {
        super(modelObject);
        this.expression = expression;
        this.lookupTable = lookupTable;
        this.isStrict = isStrict;
    }
    
    public boolean isSupportsDisplayName() {
		return false;
	}

    /**
     * @see org.apache.wicket.model.AbstractPropertyModel#propertyExpression()
     */
    @Override
    protected String propertyExpression() {
        return expression;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T getObject() {

        final Object target = getInnermostModelOrObject();
        if (target != null) {
        	
        	Object value = null;
        	if (isSupportsDisplayName()) {
        		 value = PropertyResolver.getValue("displayName", target);
        		 if (value != null) {
        			 return (T) value;
        		 }
        	}
        	
        	value = PropertyResolver.getValue(expression, target);
        	if (value == null) {
        		return null;
        	}
            String key = value.toString(); 

            if (lookupTable != null) {
                for (LookupTableRowType row : lookupTable.getRow()) {
                    if (key.equals(row.getKey())) {
                        return (T) WebComponentUtil.getOrigStringFromPoly(row.getLabel());
                    }
                }
            }
            return (T) key;
        }
    	return null;
    }
    
    @Override
    public void setObject(T object) {
        final String expression = propertyExpression();

        PropertyResolverConverter prc = new PropertyResolverConverter(Application.get().getConverterLocator(),
                Session.get().getLocale());

        if (object instanceof String) {
            String label = (String) object;
            String key;

            if (StringUtils.isBlank(label)) {
                PropertyResolver.setValue(expression, getInnermostModelOrObject(), null, prc);
            } else {
                if (!isStrict || lookupTable == null) { // set default value from input and overwrite later if key is found
                    PropertyResolver.setValue(expression, getInnermostModelOrObject(), label, prc);
                }
                if (lookupTable != null) {
	                for (LookupTableRowType row : lookupTable.getRow()) {
	                    if (label.equals(WebComponentUtil.getOrigStringFromPoly(row.getLabel()))) {
	                        key = row.getKey();
	                        PropertyResolver.setValue(expression, getInnermostModelOrObject(), key, prc);
	                        if (isSupportsDisplayName()) {
	                        	PropertyResolver.setValue("displayName", getInnermostModelOrObject(), label, prc);
	                        }
	                    }
	                }
                }
            }
        } else if (object == null) {
            PropertyResolver.setValue(expression, getInnermostModelOrObject(), object, prc);
        }
    }

    @Override
    public void detach() {
    }
}
