/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.component.form;

import org.apache.wicket.model.IModel;

/**
 * Custom text area component which normalizes multi-line POST data submitted by browser.
 * It replaces `\r\n` with `\n` in the input data.
 *
 * This one should be used instead of the default Wicket {@link org.apache.wicket.markup.html.form.TextArea} component.
 *
 * See MID-9721 for more details.
 */
public class TextArea<T> extends org.apache.wicket.markup.html.form.TextArea<T> {

    public TextArea(String id) {
        super(id);
    }

    public TextArea(String id, IModel<T> model) {
        super(id, model);
    }

    @Override
    public String[] getInputAsArray() {
        String[] array = super.getInputAsArray();
        if (array == null) {
            return null;
        }

        for (int i = 0; i < array.length; i++) {
            array[i] = array[i].replaceAll("\r\n", "\n");
        }

        return array;
    }
}
