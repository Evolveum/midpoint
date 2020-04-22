/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.prism.Containerable;

/**
 * Created by honchar
 */
public abstract class MultivalueContainerDetailsWithSummaryPanel<C extends Containerable> extends MultivalueContainerDetailsPanel<C>{

    private static final long serialVersionUID = 1L;

    private static final String ID_SUMMARY_PANEL = "summaryPanel";

    public MultivalueContainerDetailsWithSummaryPanel(String id, IModel<PrismContainerValueWrapper<C>> model){
        super(id, model);
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initLayout();
    }

    private void initLayout(){
        WebMarkupContainer summaryPanel = getSummaryPanel();
        summaryPanel.setOutputMarkupId(true);
        add(summaryPanel);
    }

    protected abstract WebMarkupContainer getSummaryPanel();
}
