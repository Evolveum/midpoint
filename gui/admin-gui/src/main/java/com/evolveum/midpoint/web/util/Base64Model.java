/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.util;

import org.apache.commons.codec.binary.Base64;
import org.apache.wicket.model.IModel;

import java.nio.charset.StandardCharsets;

/**
 * @author lazyman
 */
public class Base64Model implements IModel<String> {

    private IModel<byte[]> model;

    public Base64Model(IModel<byte[]> model) {
        this.model = model;
    }

    @Override
    public String getObject() {
        byte[] obj = model.getObject();
        if (obj == null) {
            return null;
        }

        return new String(Base64.decodeBase64(obj), StandardCharsets.UTF_8);
    }

    @Override
    public void setObject(String object) {
        if (object == null) {
            model.setObject(null);
            return;
        }

        byte[] val = Base64.encodeBase64(object.getBytes(StandardCharsets.UTF_8));
        model.setObject(val);
    }

    @Override
    public void detach() {
    }
}
