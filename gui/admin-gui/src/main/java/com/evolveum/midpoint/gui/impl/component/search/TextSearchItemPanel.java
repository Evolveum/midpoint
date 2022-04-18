/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.equivalence.ParameterizedEquivalenceStrategy;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.EvaluationTimeType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ReferentialIntegrityType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.Map;

public class TextSearchItemPanel extends PropertySearchItemPanel<TextSearchItemWrapper> {

    public TextSearchItemPanel(String id, IModel<TextSearchItemWrapper> searchItemModel) {
        super(id, searchItemModel);
    }

    @Override
    protected Component initSearchItemField() {
        String valueEnumerationRefOid = getModelObject().getValueEnumerationRefOid();
        QName valueEnumerationRefType = getModelObject().getValueEnumerationRefType();
        PrismObject<LookupTableType> lookupTablePrism = null;
        if (StringUtils.isNotEmpty(valueEnumerationRefOid)) {
            ObjectReferenceType ort = new ObjectReferenceType();
            ort.setOid(valueEnumerationRefOid);
            ort.setType(valueEnumerationRefType);
            lookupTablePrism = WebComponentUtil.findLookupTable(ort.asReferenceValue(), getPageBase());
        }
        LookupTableType lookupTable = lookupTablePrism != null ? lookupTablePrism.asObjectable() : null;
        if (lookupTable != null) {
            return createAutoCompetePanel(ID_SEARCH_ITEM_FIELD, new PropertyModel<>(getModel(), TextSearchItemWrapper.F_VALUE), lookupTable);
        } else {
            return new TextPanel<String>(ID_SEARCH_ITEM_FIELD, new PropertyModel<>(getModel(), TextSearchItemWrapper.F_VALUE));
        }
    }
}
