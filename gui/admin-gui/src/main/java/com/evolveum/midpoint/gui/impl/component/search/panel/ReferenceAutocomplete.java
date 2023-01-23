/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search.panel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.IAutoCompleteRenderer;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.IConverter;

import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteTextPanel;
import com.evolveum.midpoint.gui.api.component.autocomplete.ReferenceConverter;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author honchar
 */
public class ReferenceAutocomplete extends AutoCompleteTextPanel<ObjectReferenceType> {
    private static final long serialVersionUID = 1L;

    private final PageBase pageBase;
    private final IModel<ObjectReferenceType> model;

    public ReferenceAutocomplete(String id, final IModel<ObjectReferenceType> model, IAutoCompleteRenderer<ObjectReferenceType> renderer, PageBase pageBase) {
        super(id, model, ObjectReferenceType.class, renderer);
        this.pageBase = pageBase;
        this.model = model;
    }

    @Override
    public Iterator<ObjectReferenceType> getIterator(String input) {
        FormComponent<ObjectReferenceType> inputField = getBaseFormComponent();
        String realInput = StringUtils.isEmpty(input) ?
                (!inputField.hasRawInput() ? inputField.getValue() : inputField.getRawInput())
                : input;
        if (StringUtils.isEmpty(realInput)) {
            return Collections.emptyIterator();
        }
        Class<ObjectType> type = getReferenceTargetObjectType();
        ObjectQuery query = pageBase.getPrismContext().queryFor(type)
                .item(ObjectType.F_NAME)
                .containsPoly(realInput)
                .matchingNorm()
                .build();
        query.setPaging(pageBase.getPrismContext().queryFactory().createPaging(0, getMaxRowsCount()));
        List<PrismObject<ObjectType>> objectsList = WebModelServiceUtils.searchObjects(type, query,
                new OperationResult("searchObjects"), pageBase);
        return ObjectTypeUtil.objectListToReferences(objectsList).iterator();
    }

    @Override
    protected <C> IConverter<C> getAutoCompleteConverter(Class<C> type, IConverter<C> originConverter) {
        IConverter<C> converter = super.getAutoCompleteConverter(type, originConverter);
        return (IConverter<C>) new ReferenceConverter((IConverter<ObjectReferenceType>) converter, new ArrayList<>(), getBaseFormComponent(), pageBase){
            @Override
            protected <O extends ObjectType> Class<O> getReferenceTargetObjectType() {
                return ReferenceAutocomplete.this.getReferenceTargetObjectType();
            }

            @Override
            protected boolean isAllowedNotFoundObjectRef() {
                return ReferenceAutocomplete.this.isAllowedNotFoundObjectRef();
            }
        };
    }

    protected <O extends ObjectType> Class<O> getReferenceTargetObjectType(){
        if (model.getObject() == null || model.getObject().getType() == null) {
            return (Class<O>) ObjectType.class;
        }
        return (Class<O>) WebComponentUtil.qnameToClass(pageBase.getPrismContext(), model.getObject().getType());
    }

    protected int getMaxRowsCount() {
        return 20;
    }

    protected boolean isAllowedNotFoundObjectRef(){
        return false;
    }
}
