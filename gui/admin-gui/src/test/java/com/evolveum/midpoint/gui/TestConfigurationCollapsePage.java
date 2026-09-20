/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui;

import java.util.List;

import com.evolveum.midpoint.gui.api.component.wizard.WizardModelBasic;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic.ConfigurationStepPanel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * Minimal page hosting the resource wizard configuration step panel (with the ICF
 * {@code configurationProperties} container) so the grouping behavior can be tested
 * in isolation.
 */
public class TestConfigurationCollapsePage extends PageBase {

    public static final String PARAM_RESOURCE_OID = "resourceOid";

    public TestConfigurationCollapsePage(PageParameters parameters) {
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
        ConfigurationStepPanel configurationStep = new ConfigurationStepPanel(detailsModel, true);
        MidpointForm<?> mainForm = new MidpointForm<>("mainForm");
        mainForm.add(configurationStep);
        add(mainForm);
        new WizardModelBasic(List.of(configurationStep)).init(this);
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
