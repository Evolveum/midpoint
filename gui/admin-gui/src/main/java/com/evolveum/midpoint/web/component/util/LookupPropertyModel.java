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

import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableRowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;
import org.apache.wicket.Application;
import org.apache.wicket.Session;
import org.apache.wicket.core.util.lang.PropertyResolver;
import org.apache.wicket.core.util.lang.PropertyResolverConverter;
import org.apache.wicket.model.AbstractPropertyModel;

/**
 *  @author shood
 * */
public class LookupPropertyModel<T> extends AbstractPropertyModel<T> {

    protected final String expression;
    private LookupTableType lookupTable;

    public LookupPropertyModel(Object modelObject, String expression, LookupTableType lookupTable){
        super(modelObject);
        this.expression = expression;
        this.lookupTable = lookupTable;
    }

    /**
     * @see org.apache.wicket.model.AbstractPropertyModel#propertyExpression()
     */
    @Override
    protected String propertyExpression(){
        return expression;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T getObject() {

        final Object target = getInnermostModelOrObject();

        if (target != null){
            String key = (String)PropertyResolver.getValue(expression, target);

            if(key == null){
                return null;
            }

            for(LookupTableRowType row: lookupTable.getRow()){
                if(key.equals(row.getKey())){
                    return (T)WebMiscUtil.getOrigStringFromPoly(row.getLabel());
                }
            }
            return (T)key;
        }

        return null;
    }

    @Override
    public void setObject(T object) {
        final String expression = propertyExpression();

        PropertyResolverConverter prc;
        prc = new PropertyResolverConverter(Application.get().getConverterLocator(),
                Session.get().getLocale());

        if(object instanceof String){
            String label = (String) object;
            String key;

            if (label == null || label.trim().equals("")){
                PropertyResolver.setValue(expression, getInnermostModelOrObject(), null, prc);
            } else {
                for (LookupTableRowType row : lookupTable.getRow()) {
                    if (label.equals(WebMiscUtil.getOrigStringFromPoly(row.getLabel()))) {
                        key = row.getKey();

                        PropertyResolver.setValue(expression, getInnermostModelOrObject(), key, prc);
                    }
                }
            }
        } else if (object == null){
                PropertyResolver.setValue(expression, getInnermostModelOrObject(), object, prc);
        }
    }

    @Override
    public void detach() {}
}
