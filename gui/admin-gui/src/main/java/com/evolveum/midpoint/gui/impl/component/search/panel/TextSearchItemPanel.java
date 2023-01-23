/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search.panel;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.TextSearchItemWrapper;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import javax.xml.namespace.QName;

public class TextSearchItemPanel extends PropertySearchItemPanel<TextSearchItemWrapper> {

    public TextSearchItemPanel(String id, IModel<TextSearchItemWrapper> searchItemModel) {
        super(id, searchItemModel);
    }

    @Override
    protected Component initSearchItemField(String id) {
        String valueEnumerationRefOid = getModelObject().getValueEnumerationRefOid();
//        QName valueEnumerationRefType = getModelObject().getValueEnumerationRefType();
//        PrismObject<LookupTableType> lookupTablePrism = null;
//        if (StringUtils.isNotEmpty(valueEnumerationRefOid)) {
//            ObjectReferenceType ort = new ObjectReferenceType();
//            ort.setOid(valueEnumerationRefOid);
//            ort.setType(valueEnumerationRefType);
//            lookupTablePrism = WebComponentUtil.findLookupTable(ort.asReferenceValue(), getPageBase());
//        }
//        LookupTableType lookupTable = lookupTablePrism != null ? lookupTablePrism.asObjectable() : null;
        if (valueEnumerationRefOid != null) {
            return createAutoCompetePanel(id, new PropertyModel<>(getModel(), TextSearchItemWrapper.F_VALUE), valueEnumerationRefOid);
        } else {
            return new TextPanel<String>(id, new PropertyModel<>(getModel(), TextSearchItemWrapper.F_VALUE));
        }
    }
}
