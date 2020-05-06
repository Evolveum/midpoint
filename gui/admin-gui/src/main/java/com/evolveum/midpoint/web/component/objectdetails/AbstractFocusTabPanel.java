/*
 * Copyright (c) 2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.objectdetails;

import java.util.List;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.ShadowWrapper;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

/**
 * @author semancik
 */
public abstract class AbstractFocusTabPanel<F extends FocusType> extends AbstractObjectTabPanel<F> {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(AbstractFocusTabPanel.class);

    private LoadableModel<List<ShadowWrapper>> projectionModel;

    public AbstractFocusTabPanel(String id, Form<PrismObjectWrapper<F>> mainForm,
            LoadableModel<PrismObjectWrapper<F>> focusWrapperModel,
            LoadableModel<List<ShadowWrapper>> projectionModel) {
        super(id, mainForm, focusWrapperModel);
        this.projectionModel = projectionModel;
    }

    public LoadableModel<List<ShadowWrapper>> getProjectionModel() {
        return projectionModel;
    }

}
