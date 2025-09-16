/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.wrapper;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.page.PageAdminLTE;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.LoadableDetachableModel;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Collections;
import java.util.List;

/**
 * Wrapper for ConnectorDevelopmentType/testing/testingResource.
 */
public class ConnectorDevelopmentTypeResourceValueWrapperImpl<T extends Referencable> extends PrismReferenceValueWrapperImpl<T> {

    public ConnectorDevelopmentTypeResourceValueWrapperImpl(PrismReferenceWrapper<T> parent, PrismReferenceValue value, ValueStatus status) {
        super(parent, value, status);
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected @NotNull <O extends ObjectType> ObjectDetailsModels<O> createObjectDetailsModels(
            ContainerPanelConfigurationType config, ModelServiceLocator serviceLocator, LoadableDetachableModel<PrismObject<O>> prismObjectModel) {
        ObjectDetailsModels objectDetailsModels = new ResourceDetailsModel((LoadableDetachableModel) prismObjectModel, serviceLocator) {
            @Override
            public List<? extends ContainerPanelConfigurationType> getPanelConfigurations() {
                return Collections.singletonList(config);
            }

            @Override
            protected WrapperContext createWrapperContext(Task task, OperationResult result) {
                return ConnectorDevelopmentTypeResourceValueWrapperImpl.this.createWrapperContextForNewObject(super.createWrapperContext(task, result));
            }
        };
        return objectDetailsModels;
    }

    protected <O extends ObjectType> PrismObject<O> createNewPrismObject(OperationResult result, PageAdminLTE pageAdminLTE) throws SchemaException {
        if (StringUtils.isNotEmpty(getNewValue().getOid())) {
            return WebModelServiceUtils.loadObject(getRealValue(), pageAdminLTE);
        }

        return super.createNewPrismObject(result, pageAdminLTE);
    }
}
