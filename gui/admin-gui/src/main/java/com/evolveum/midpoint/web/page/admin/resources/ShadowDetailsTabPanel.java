/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.resources;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.ShadowPanel;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectTabPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ShadowDetailsTabPanel extends AbstractObjectTabPanel<ShadowType> {

    private static final long serialVersionUID = 1L;

    private static final String ID_ACCOUNT = "account";


    public ShadowDetailsTabPanel(String id, Form<PrismObjectWrapper<ShadowType>> mainForm,
                                 LoadableModel<PrismObjectWrapper<ShadowType>> objectWrapperModel) {
        super(id, mainForm, objectWrapperModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }



    private void initLayout() {
        ShadowPanel shadowPanel = new ShadowPanel(ID_ACCOUNT, (IModel) getObjectWrapperModel());
        add(shadowPanel);
    }


}
