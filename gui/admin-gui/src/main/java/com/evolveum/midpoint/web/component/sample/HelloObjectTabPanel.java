/*
 * Copyright (c) 2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.sample;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectTabPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Sample showing a custom object form that displays simple greeting.
 *
 * @author Radovan Semancik
 *
 */
public class HelloObjectTabPanel<F extends FocusType> extends AbstractObjectTabPanel<F> {

    private static final String ID_HELLO_LABEL = "helloLabel";

    public HelloObjectTabPanel(String id, Form mainForm, LoadableModel<PrismObjectWrapper<F>> focusModel) {
        super(id, mainForm, focusModel);

    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        add(new Label(ID_HELLO_LABEL, new Model<String>() {
            @Override
            public String getObject() {
                PrismObject<F> focus = getObjectWrapper().getObject();
                if (focus != null) {
                    PolyStringType name = focus.asObjectable().getName();
                    if (name != null) {
                        return "Hello "+name.getOrig()+"!";
                    }
                }
                return "Hello world!";
            }
        }));
    }

}
