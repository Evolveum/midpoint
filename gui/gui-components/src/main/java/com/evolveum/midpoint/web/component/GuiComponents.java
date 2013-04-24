/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component;

import org.apache.commons.lang.Validate;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

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

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(10);
    private static final String KEY_BOOLEAN_NULL = "Boolean.NULL";
    private static final String KEY_BOOLEAN_TRUE = "Boolean.TRUE";
    private static final String KEY_BOOLEAN_FALSE = "Boolean.FALSE";

    public static void destroy() {
        EXECUTOR.shutdownNow();
    }

    public static Future<?> submitRunnable(Runnable runnable) {
        Validate.notNull(runnable, "Runnable must not be null.");

        return EXECUTOR.submit(runnable);
    }

    public static <T> Future<T> submitCallable(Callable<T> callable) {
        Validate.notNull(callable, "Callable must not be null.");

        return EXECUTOR.submit(callable);
    }

    public static DropDownChoice createTriStateCombo(String id, IModel<Boolean> model) {
        final IChoiceRenderer renderer = new IChoiceRenderer() {

            @Override
            public Object getDisplayValue(Object object) {
                String key;
                if (object == null) {
                    key = KEY_BOOLEAN_NULL;
                } else {
                    Boolean b = (Boolean) object;
                    key = b ? KEY_BOOLEAN_TRUE : KEY_BOOLEAN_FALSE;
                }

                StringResourceModel model = new StringResourceModel(key, new Model<String>(), key);
                return model.getString();
            }

            @Override
            public String getIdValue(Object object, int index) {
                return Integer.toString(index);
            }
        };

        DropDownChoice dropDown = new DropDownChoice(id, model, createChoices(), renderer) {

            @Override
            protected CharSequence getDefaultChoice(String selectedValue) {
                StringResourceModel model = new StringResourceModel(KEY_BOOLEAN_NULL,
                        new Model<String>(), KEY_BOOLEAN_NULL);
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
