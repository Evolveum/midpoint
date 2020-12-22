/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.input;

import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * @author skublik
 */
public class QNameIChoiceRenderer implements IChoiceRenderer<QName> {

    private static final long serialVersionUID = 1L;

    private String prefix;

    public QNameIChoiceRenderer(){}

    public QNameIChoiceRenderer(String prefix){
        this.prefix = prefix;
    }

    @Override
    public Object getDisplayValue(QName qname) {
        if (qname == null) {
            return null;
        }

        String realPrefix = prefix != null && !prefix.isEmpty() ? (prefix + ".") : "";
        String key = realPrefix + qname.getLocalPart();

        return new ResourceModel(key, qname.getLocalPart()).getObject();
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
