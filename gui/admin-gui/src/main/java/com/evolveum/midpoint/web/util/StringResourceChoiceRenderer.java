/*
 * Copyright (c) 2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.util;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;

/**
 * @author semancik
 */
public class StringResourceChoiceRenderer implements IChoiceRenderer<String> {
    private static final long serialVersionUID = 1L;

    String keyPrefix;

    public StringResourceChoiceRenderer(String keyPrefix) {
        super();
        this.keyPrefix = keyPrefix;
    }

    @Override
    public Object getDisplayValue(String object) {
        return new ResourceModel(keyPrefix+"."+object, object).getObject();
    }

    @Override
    public String getIdValue(String object, int index) {
        return String.valueOf(index);
    }

    @Override
    public String getObject(String id, IModel<? extends List<? extends String>> choices) {
        return StringUtils.isNotBlank(id) ? choices.getObject().get(Integer.parseInt(id)) : null;
    }

}
