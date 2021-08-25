/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.component.FocusOperationalButtonsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.component.OperationalButtonsPanel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.List;

public abstract class PageFocusDetails<F extends FocusType> extends PageAssignmentHolderDetails<F> {

    public PageFocusDetails(PageParameters pageParameters) {
        super(pageParameters);
    }

    @Override
    protected OperationalButtonsPanel createButtonsPanel(String id, LoadableModel<PrismObjectWrapper<F>> wrapperModel) {
        return new FocusOperationalButtonsPanel(id, wrapperModel) {

            @Override
            protected void savePerformed(AjaxRequestTarget target) {
                PageFocusDetails.this.savePerformed(target);
            }

            @Override
            protected void previewPerformed(AjaxRequestTarget target) {
                PageFocusDetails.this.previewPerformed(target);
            }
        };
    }
}
