/*
 * Copyright (c) 2010-2019 Evolveum
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

package com.evolveum.midpoint.gui.impl.model;

import com.evolveum.midpoint.util.exception.SystemException;

import org.apache.commons.codec.binary.Base64;
import org.apache.wicket.model.IModel;

import java.io.UnsupportedEncodingException;

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

        try {
            return new String(obj, "utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new SystemException(e);
        }
    }

    @Override
    public void setObject(String object) {
        if (object == null) {
            model.setObject(null);
        }

        try {
            byte[] val = object.getBytes("utf-8");
            model.setObject(val);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void detach() {
    }
}
