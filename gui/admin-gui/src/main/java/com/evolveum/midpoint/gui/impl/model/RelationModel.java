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

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.QNameUtil;
import org.apache.wicket.model.IModel;

import javax.xml.namespace.QName;

public class RelationModel implements IModel<String> {

    private IModel<QName> baseModel;

    public RelationModel(IModel<QName> baseModel) {
        this.baseModel = baseModel;
    }


    @Override
    public String getObject() {
        QName value = baseModel.getObject();
        if (value == null) {
            return null;
        }

        return QNameUtil.qNameToUri(value);
    }

    @Override
    public void setObject(String object) {
        QName newRelation = null;
        if (QNameUtil.isUri(object)) {
            newRelation = QNameUtil.uriToQName(object);
        } else {
            new ItemName(SchemaConstants.NS_ORG, object);
        }

        baseModel.setObject(newRelation);
    }
}
