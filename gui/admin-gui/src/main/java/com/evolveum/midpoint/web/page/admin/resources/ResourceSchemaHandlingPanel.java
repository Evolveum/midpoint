/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.resources;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaHandlingType;

import org.apache.wicket.model.IModel;

public class ResourceSchemaHandlingPanel extends BasePanel<PrismContainerWrapper<SchemaHandlingType>> {

    public ResourceSchemaHandlingPanel(String id, IModel<PrismContainerWrapper<SchemaHandlingType>> model) {
        super(id, model);
    }


}
