/*
 * Copyright (c) 2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.progressbar;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * @author semancik
 */
public class ProgressbarPanel extends Panel{
    private static final long serialVersionUID = 1L;

    private static final String ID_PROGRESS_BAR = "progressBar";

    public ProgressbarPanel(String id, IModel<Integer> model) {
        super(id, model);
        initLayout(model);
    }

    private void initLayout(final IModel<Integer> model){

        WebMarkupContainer progressBar = new WebMarkupContainer(ID_PROGRESS_BAR);
        IModel<String> styleAttributeModel = new Model<String>(){
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                return "width: " + model.getObject() + "%";
            }
        };
        progressBar.add(new AttributeModifier("style", styleAttributeModel));
        add(progressBar);
    }


}
