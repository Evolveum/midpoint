/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.data.column.icon;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.self.requestAccess.RoleCatalogPanel;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.IResource;

public class RoundedImageObjectColumn<F extends FocusType, S> extends RoundedIconColumn<SelectableBean<F>, S>{

    private final PageBase pageBase;

    public RoundedImageObjectColumn(IModel<String> title, PageBase pageBase) {
        super(title);
        this.pageBase = pageBase;
    }

    @Override
    protected IModel<IResource> createPreferredImage(IModel<SelectableBean<F>> model) {
        return RoundedImageObjectColumn.this.createImage(() -> model.getObject().getValue());
    }

    private IModel<IResource> createImage(IModel<ObjectType> model) {
        return new LoadableModel<>(false) {
            @Override
            protected IResource load() {
                ObjectType object = model.getObject();

                return WebComponentUtil.createJpegPhotoResource((FocusType) object);
            }
        };
    }

    @Override
    protected DisplayType createDisplayType(IModel<SelectableBean<F>> model) {
        OperationResult result = new OperationResult("getIcon");
        return GuiDisplayTypeUtil.getDisplayTypeForObject(model.getObject().getValue(), result, pageBase);
    }
}
