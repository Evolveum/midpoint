/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.ResourceConfigurationPanel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * Minimal page hosting the resource details "connector configuration" panel so the
 * grouping behavior can be tested in isolation.
 */
public class TestResourceConfigurationGroupingPage extends PageBase {

    public static final String PARAM_RESOURCE_OID = "resourceOid";

    public TestResourceConfigurationGroupingPage(PageParameters parameters) {
        super(parameters);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        try {
            initTestLayout();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot initialize " + getClass().getSimpleName(), e);
        }
    }

    private void initTestLayout() throws Exception {
        String oid = getPageParameters().get(PARAM_RESOURCE_OID).toString();
        Task task = createSimpleTask("loadResource");
        OperationResult result = task.getResult();
        PrismObject<ResourceType> resource = getModelService().getObject(ResourceType.class, oid, null, task, result);

        ResourceDetailsModel detailsModel = new ResourceDetailsModel(createObjectModel(resource), this);
        MidpointForm<?> mainForm = new MidpointForm<>("mainForm");
        mainForm.add(new ResourceConfigurationPanel("configuration", detailsModel, null));
        add(mainForm);
    }

    private LoadableDetachableModel<PrismObject<ResourceType>> createObjectModel(PrismObject<ResourceType> object) {
        return new LoadableDetachableModel<>() {
            @Override
            protected PrismObject<ResourceType> load() {
                return object;
            }
        };
    }
}
