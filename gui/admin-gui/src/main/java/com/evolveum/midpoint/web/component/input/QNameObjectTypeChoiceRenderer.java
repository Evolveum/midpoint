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

package com.evolveum.midpoint.web.component.input;

import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;

import javax.xml.namespace.QName;
import java.util.List;

public class QNameObjectTypeChoiceRenderer implements IChoiceRenderer<QName> {

    @Override
    public Object getDisplayValue(QName qname) {
        if (qname == null) {
            return null;
        }

        String key = "ObjectType." + qname.getLocalPart();

        return new ResourceModel(key, key).getObject();
    }

    @Override
    public String getIdValue(QName object, int index) {
        return Integer.toString(index);
    }

    @Override
    public QName getObject(String id, IModel<? extends List<? extends QName>> choices) {
        if (id == null) {
            return null;
        }

        int i = Integer.parseInt(id);

        return choices.getObject().get(i);
    }
}
