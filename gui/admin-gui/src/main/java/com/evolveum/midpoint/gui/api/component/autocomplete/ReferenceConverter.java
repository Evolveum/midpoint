/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.autocomplete;

import java.util.List;
import java.util.Locale;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

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
        Class<ObjectType> type = getReferenceTargetObjectType();
        ObjectQuery query = pageBase.getPrismContext().queryFor(type)
                .item(ObjectType.F_NAME)
                .eq(value)
                .matchingNorm()
                .build();
        List<PrismObject<ObjectType>> objectsList = WebModelServiceUtils.searchObjects(
                type, query, new OperationResult("searchObjects"), pageBase);
        if (CollectionUtils.isNotEmpty(objectsList)) {
            if (objectsList.size() == 1) {
                return ObjectTypeUtil.createObjectRefWithFullObject(objectsList.get(0));
            }
            pageBase.error("Couldn't specify one object by name '" + value + "' and type " + type.getSimpleName() + ". Please will try specific type of object.");
        }
        ObjectReferenceType ref = null;
        if (isAllowedNotFoundObjectRef()) {
            ref = new ObjectReferenceType();
            ref.setTargetName(new PolyStringType(value));
            if (baseComponent != null && baseComponent.getModelObject() instanceof ObjectReferenceType) {
                if (((ObjectReferenceType) baseComponent.getModelObject()).getRelation() == null) {
                    ref.setRelation(pageBase.getPrismContext().getDefaultRelation());
                } else {
                    ref.setRelation(((ObjectReferenceType) baseComponent.getModelObject()).getRelation());
                }
                ref.setType(WebComponentUtil.classToQName(PrismContext.get(), type));
            }
        }
        return ref;
    }

    @Override
    public String convertToString(ObjectReferenceType ref, Locale arg1) {
        return ref != null && (ref.getTargetName() != null || ref.getObject() != null) ? WebComponentUtil.getName(ref) : "";
    }

    protected <O extends ObjectType> Class<O> getReferenceTargetObjectType() {
        return (Class<O>) AbstractRoleType.class;
    }

    protected boolean isAllowedNotFoundObjectRef() {
        return false;
    }
}
