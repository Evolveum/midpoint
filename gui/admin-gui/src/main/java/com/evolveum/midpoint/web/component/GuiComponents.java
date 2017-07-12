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

package com.evolveum.midpoint.web.component;

import org.apache.commons.lang.Validate;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.page.PageBase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author lazyman
 */
public class GuiComponents {

    private static ExecutorService EXECUTOR;

    private static final String KEY_BOOLEAN_NULL = "Boolean.NULL";
    private static final String KEY_BOOLEAN_TRUE = "Boolean.TRUE";
    private static final String KEY_BOOLEAN_FALSE = "Boolean.FALSE";

    public static void destroy() {
        EXECUTOR.shutdownNow();
    }

    public static void init() {
        EXECUTOR = Executors.newFixedThreadPool(10);
    }

    public static <T> Future<T> submitCallable(Callable<T> callable) {
        Validate.notNull(callable, "Callable must not be null.");

        return EXECUTOR.submit(callable);
    }

    public static <T> DropDownChoice createTriStateCombo(String id, IModel<Boolean> model) {
        final IChoiceRenderer<T> renderer = new IChoiceRenderer<T>() {
        	
        
        	@Override
        	public T getObject(String id, IModel<? extends List<? extends T>> choices) {
        		return id != null ? choices.getObject().get(Integer.parseInt(id)) : null;
        	}

            @Override
            public String getDisplayValue(T object) {
                String key;
                if (object == null) {
                    key = KEY_BOOLEAN_NULL;
                } else {
                    Boolean b = (Boolean) object;
                    key = b ? KEY_BOOLEAN_TRUE : KEY_BOOLEAN_FALSE;
                }

                StringResourceModel model = PageBase.createStringResourceStatic(null, key);
//                
                return model.getString();
            }

            @Override
            public String getIdValue(T object, int index) {
                return Integer.toString(index);
            }


		
        };

        DropDownChoice dropDown = new DropDownChoice(id, model, createChoices(), renderer) {

            @Override
            protected CharSequence getDefaultChoice(String selectedValue) {
            	StringResourceModel model = PageBase.createStringResourceStatic(null, KEY_BOOLEAN_NULL);

                return model.getString();
            }
        };
        dropDown.setNullValid(true);

        return dropDown;
    }

    private static IModel<List<Boolean>> createChoices() {
        return new AbstractReadOnlyModel<List<Boolean>>() {

            @Override
            public List<Boolean> getObject() {
                List<Boolean> list = new ArrayList<Boolean>();
                list.add(null);
                list.add(Boolean.TRUE);
                list.add(Boolean.FALSE);

                return list;
            }
        };
    }
}
