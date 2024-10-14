/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.data.provider;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic.ResourceTemplate;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic.ResourceTemplate.TemplateType;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringTranslationType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Collection;

/**
 * @author lskublik
 */
public abstract class TemplateTileProvider<T extends Serializable, O extends ObjectType>
        extends ObjectDataProvider<TemplateTile<T>, O> {

    private static final String DOT_CLASS = TemplateTileProvider.class.getName() + ".";
    private static final String OPERATION_GET_DISPLAY = DOT_CLASS + "getDisplay";

    public TemplateTileProvider(Component component, IModel<Search> search) {
        super(component, (IModel) search);
    }

    // Here we apply the distinct option. It is easier and more reliable to apply it here than to do at all the places
    // where options for this provider are defined.
    protected Collection<SelectorOptions<GetOperationOptions>> getOptionsToUse() {
        @NotNull Collection<SelectorOptions<GetOperationOptions>> rawOption = getOperationOptionsBuilder().raw().build();
        return GetOperationOptions.merge(getOptions(), getDistinctRelatedOptions(), rawOption);
    }

    @Override
    public ObjectPaging createPaging(long offset, long pageSize) {
        setSort(getDefaultSortParam(), getDefaultSortOrder());
        return super.createPaging(offset, pageSize);
    }

    @Override
    public TemplateTile<T> createDataObjectWrapper(PrismObject<O> obj) {
        return createTileObject(obj);
    }

    protected abstract TemplateTile<T> createTileObject(PrismObject<O> obj);
}
