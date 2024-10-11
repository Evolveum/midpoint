/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.data.provider;

import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

/**
 * @author lskublik
 */
public class ObjectTileProvider
        extends TemplateTileProvider<String, ObjectType> {

    private static final String DOT_CLASS = ObjectTileProvider.class.getName() + ".";
    private static final String OPERATION_GET_DISPLAY = DOT_CLASS + "getDisplay";

    public ObjectTileProvider(Component component, IModel<Search> search) {
        super(component, search);
    }

    @Override
    public ObjectPaging createPaging(long offset, long pageSize) {
        setSort(getDefaultSortParam(), getDefaultSortOrder());
        return super.createPaging(offset, pageSize);
    }

    @Override
    protected TemplateTile<String> createTileObject(PrismObject<ObjectType> obj) {
        String title = WebComponentUtil.getDisplayNameOrName(obj);

        OperationResult result = new OperationResult(OPERATION_GET_DISPLAY);

        DisplayType display =
                GuiDisplayTypeUtil.getDisplayTypeForObject(obj, result, getPageBase());
        return new TemplateTile(
                GuiDisplayTypeUtil.getIconCssClass(display),
                title,
                obj.getOid())
                .description(obj.asObjectable().getDescription());
    }
}
