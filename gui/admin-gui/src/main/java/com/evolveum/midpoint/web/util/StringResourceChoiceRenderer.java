/*
 * Copyright (C) 2015-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.util;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
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
        return new ResourceModel(keyPrefix + "." + object, object).getObject();
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
