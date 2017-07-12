/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.util.DisplayableValue;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.converter.AbstractConverter;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;

/**
 * @author Viliam Repan (lazyman)
 */
public class DisplayableRenderer<T extends Serializable> extends AbstractConverter<DisplayableValue>
        implements IChoiceRenderer<DisplayableValue<T>> {

    private IModel<List<DisplayableValue>> allChoices;

    public DisplayableRenderer(IModel<List<DisplayableValue>> allChoices) {
        this.allChoices = allChoices;
    }

    @Override
    protected Class<DisplayableValue> getTargetType() {
        return DisplayableValue.class;
    }

    @Override
    public Object getDisplayValue(DisplayableValue<T> object) {
        if (object == null) {
            return null;
        }

        return object.getLabel();
    }

    @Override
    public String getIdValue(DisplayableValue<T> object, int index) {
        return Integer.toString(index);
    }

    @Override
    public DisplayableValue<T> getObject(String id, IModel<? extends List<? extends DisplayableValue<T>>> choices) {
        if (StringUtils.isEmpty(id)) {
            return null;
        }
        return choices.getObject().get(Integer.parseInt(id));
    }

    @Override
    public DisplayableValue<T> convertToObject(String value, Locale locale) throws ConversionException {
        if (value == null) {
            return null;
        }

        List<DisplayableValue> values = allChoices.getObject();
        for (DisplayableValue val : values) {
            if (value.equals(val.getLabel())) {
                return val;
            }
        }

        return null;
    }
}
