/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.autocomplete;

import java.util.List;
import java.util.Locale;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.IConverter;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author honchar
 */
public class ReferenceConverter implements IConverter<ObjectReferenceType> {

    private static final long serialVersionUID = 1L;
    private IConverter<ObjectReferenceType> originConverter;
    private List<ObjectReferenceType> referenceList = null;
    private FormComponent baseComponent;

    private PageBase pageBase;

    public ReferenceConverter(IConverter<ObjectReferenceType> originConverter, List<ObjectReferenceType> referenceList, FormComponent baseComponent, PageBase pageBase) {
        this.originConverter = originConverter;
        this.referenceList = referenceList;
        this.baseComponent = baseComponent;
        this.pageBase = pageBase;
    }

    @Override
    public ObjectReferenceType convertToObject(String value, Locale locale) throws ConversionException {
        ObjectQuery query = pageBase.getPrismContext().queryFor(AbstractRoleType.class)
                .item(ObjectType.F_NAME)
                .eq(value)
                .matchingOrig()
                .build();
        List<PrismObject<AbstractRoleType>> objectsList = WebModelServiceUtils.searchObjects(
                AbstractRoleType.class, query, new OperationResult("searchObjects"), pageBase);
        if (CollectionUtils.isNotEmpty(objectsList)) {
            return ObjectTypeUtil.createObjectRefWithFullObject(
                    objectsList.get(0), pageBase.getPrismContext());
        }
        return null;

    }

    @Override
    public String convertToString(ObjectReferenceType ref, Locale arg1) {
        return ref != null ? WebComponentUtil.getName(ref) : "";
    }
}
