/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.server.handlers;

import com.evolveum.midpoint.gui.api.page.PageBase;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerPanel;
import com.evolveum.midpoint.web.page.admin.server.handlers.dto.GenericHandlerDto;

/**
 * @author mederly
 */
public class GenericHandlerPanel extends BasePanel<GenericHandlerDto> {
    private static final long serialVersionUID = 1L;

    public static final String ID_CONTAINER = "container";

    public GenericHandlerPanel(String id, IModel<GenericHandlerDto> model, PageBase parentPage) {
        super(id, model);
        initLayout();
        setOutputMarkupId(true);
    }

    //TODO refactor to use registry
    private void initLayout() {
        PrismContainerPanel containerPanel = new PrismContainerPanel(
                ID_CONTAINER, new PropertyModel<>(getModel(), GenericHandlerDto.F_CONTAINER), null);
        add(containerPanel);

    }

}
