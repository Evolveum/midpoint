/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.model;

import com.evolveum.midpoint.util.exception.SystemException;

import org.apache.commons.codec.binary.Base64;
import org.apache.wicket.model.IModel;

import java.nio.charset.StandardCharsets;

/**
 * @author skublik
 */
public class JasperTemplateModel implements IModel<String> {

    private IModel<byte[]> model;

    public JasperTemplateModel(IModel<byte[]> model) {
        this.model = model;
    }

    @Override
    public String getObject() {
        if (model.getObject() == null) {
            return null;
        }
        byte[] obj;
        if (Base64.isBase64(model.getObject())) {
            obj = Base64.decodeBase64(model.getObject());
        } else {
            obj = model.getObject();
        }

        return new String(obj, StandardCharsets.UTF_8);
    }

    @Override
    public void setObject(String object) {
        if (object == null) {
            model.setObject(null);
            return;
        }

        byte[] val = object.getBytes(StandardCharsets.UTF_8);
        model.setObject(val);
    }

    @Override
    public void detach() {
    }
}
