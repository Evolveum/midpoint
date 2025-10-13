/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
