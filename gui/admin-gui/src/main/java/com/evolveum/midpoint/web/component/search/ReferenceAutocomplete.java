/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteTextPanel;

import com.evolveum.midpoint.gui.api.component.autocomplete.ReferenceConverter;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.commons.collections.iterators.ArrayIterator;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.IAutoCompleteRenderer;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.IConverter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * @author honchar
 */
public abstract class ReferenceAutocomplete extends AutoCompleteTextPanel<ObjectReferenceType> {
    private static final long serialVersionUID = 1L;

    private PageBase pageBase;

    public ReferenceAutocomplete(String id, final IModel<ObjectReferenceType> model, IAutoCompleteRenderer<ObjectReferenceType> renderer, PageBase pageBase) {
        super(id, model, ObjectReferenceType.class, renderer);
        this.pageBase = pageBase;
    }


    @Override
    public Iterator<ObjectReferenceType> getIterator(String input) {
        FormComponent<ObjectReferenceType> inputField = getBaseFormComponent();
        String realInput = StringUtils.isEmpty(input) ? inputField.getRawInput() : input;
        if (StringUtils.isEmpty(realInput)){
            return new ArrayIterator();
        }
        ObjectQuery query = pageBase.getPrismContext().queryFor(AbstractRoleType.class)
                .item(ObjectType.F_NAME)
                .containsPoly(realInput)
                .matchingNorm()
                .build();
        query.setPaging(pageBase.getPrismContext().queryFactory().createPaging(0, getMaxRowsCount()));
        List<PrismObject<AbstractRoleType>> objectsList = WebModelServiceUtils.searchObjects(AbstractRoleType.class, query,
                new OperationResult("searchObjects"), pageBase);
        return ObjectTypeUtil.objectListToReferences(objectsList).iterator();
    }

    @Override
    protected <C> IConverter<C> getAutoCompleteConverter(Class<C> type, IConverter<C> originConverter){
        IConverter<C> converter = super.getAutoCompleteConverter(type, originConverter);
        return (IConverter<C>) new ReferenceConverter((IConverter<ObjectReferenceType>)converter, new ArrayList<>(), getBaseFormComponent(), pageBase);
    }

    protected abstract <O extends ObjectType> Class<O> getReferenceTargetObjectType();

    protected int getMaxRowsCount(){
        return 20;
    }
}
