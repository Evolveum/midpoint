/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.input;

import java.util.List;
import javax.xml.namespace.QName;

import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.schema.constants.ObjectTypes;

public class QNameObjectTypeChoiceRenderer implements IChoiceRenderer<QName> {

    private static final long serialVersionUID = 1L;

    @Override
    public Object getDisplayValue(QName qname) {
        if (qname == null) {
            return null;
        }

        ObjectTypes ot = ObjectTypes.getObjectTypeFromTypeQName(qname);
        String key = WebComponentUtil.createEnumResourceKey(ot);
        return new StringResourceModel(key).setDefaultValue(key);
    }

    @Override
    public String getIdValue(QName object, int index) {
        return Integer.toString(index);
    }

    @Override
    public QName getObject(String id, IModel<? extends List<? extends QName>> choices) {
        if (id == null || id.trim().equals("")) {
            return null;
        }

        int i = Integer.parseInt(id);

        return choices.getObject().get(i);
    }
}
