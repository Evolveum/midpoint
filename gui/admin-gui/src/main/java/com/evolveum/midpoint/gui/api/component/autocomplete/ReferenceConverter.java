/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.autocomplete;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.query.builder.S_FilterExit;
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
 * Converter from Name of object (String) to ObjectReferenceType, includes supported types and custom filters
 *
 * @author honchar
 */
public class ReferenceConverter<R extends Referencable> implements IConverter<R> {

    private static final long serialVersionUID = 1L;
    private final FormComponent baseComponent;

    private final PageBase pageBase;

    public ReferenceConverter(FormComponent baseComponent, PageBase pageBase) {
        this.baseComponent = baseComponent;
        this.pageBase = pageBase;
    }

    @Override
    public R convertToObject(String value, Locale locale) throws ConversionException {
        Class<ObjectType> type = ObjectType.class;
        List<Class<ObjectType>> supportedTypes = getSupportedObjectTypes();
        if (supportedTypes.size() == 1) {
            type = supportedTypes.iterator().next();
        }

        S_FilterExit filter = PrismContext.get().queryFor(type)
                .item(ObjectType.F_NAME)
                .eq(value)
                .matchingNorm();

        if (supportedTypes.size() > 1) {
            for (Class<ObjectType> supportedType : supportedTypes) {
                filter.and().type(supportedType);
            }
        }
        ObjectQuery condition = createChooseQuery();
        if (condition != null) {
            filter = filter
                    .and()
                    .filter(condition.getFilter());
        }
        ObjectQuery query = filter.build();
        List<PrismObject<ObjectType>> objectsList = WebModelServiceUtils.searchObjects(
                type, query, new OperationResult("searchObjects"), pageBase);
        if (CollectionUtils.isNotEmpty(objectsList)) {
            if (objectsList.size() == 1) {
                return (R) ObjectTypeUtil.createObjectRefWithFullObject(objectsList.get(0));
            }
            pageBase.error("Couldn't specify one object by name '" + value + "' and types "
                    + (supportedTypes.isEmpty() ? type.getSimpleName() : supportedTypes.stream().map(Class::getSimpleName).collect(Collectors.joining(", ")))
                    + ". Please will try specific type of object.");
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
        return (R) ref;
    }

    /**
     * Create custom query for possible object for reference.
     */
    protected ObjectQuery createChooseQuery() {
        return null;
    }

    @Override
    public String convertToString(R ref, Locale arg1) {
        return ref != null && (ref.getTargetName() != null || ref.getObject() != null) ? WebComponentUtil.getName(ref) : "";
    }

    /**
     * Return supported types for possible object for reference.
     */
    protected <O extends ObjectType> List<Class<O>> getSupportedObjectTypes() {
        ArrayList<Class<O>> list = new ArrayList<>();
        list.add((Class<O>) AbstractRoleType.class);
        return list;
    }

    protected boolean isAllowedNotFoundObjectRef() {
        return false;
    }
}
