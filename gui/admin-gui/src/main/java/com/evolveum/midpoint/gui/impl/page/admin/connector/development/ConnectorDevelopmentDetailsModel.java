/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.connector.development;

import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentOperation;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorDevelopmentType;

import org.apache.wicket.model.LoadableDetachableModel;

public class ConnectorDevelopmentDetailsModel extends AssignmentHolderDetailsModel<ConnectorDevelopmentType> {

    private LoadableDetachableModel<ConnectorDevelopmentOperation> developmentOperationModel;

    public ConnectorDevelopmentDetailsModel(LoadableDetachableModel<PrismObject<ConnectorDevelopmentType>> prismObjectModel, ModelServiceLocator serviceLocator) {
        super(prismObjectModel, serviceLocator);

        developmentOperationModel =  new LoadableDetachableModel<>() {
            @Override
            protected ConnectorDevelopmentOperation load() {
                PrismObject<ConnectorDevelopmentType> object = getPrismObject();
                if (isEditObject()) {
                    return getModelServiceLocator().getConnectorService().continueFrom(object.asObjectable());
                }
                return getModelServiceLocator().getConnectorService().startFromNew(object.asObjectable().getApplication(), new OperationResult("load_ConnectorDevelopmentOperation"));
            }
        };
    }

    public ConnectorDevelopmentOperation getConnectorDevelopmentOperation() {
        return developmentOperationModel.getObject();
    }

    @Override
    public void reset() {
        super.reset();
        developmentOperationModel.detach();
    }

    public ModelServiceLocator getServiceLocator() {
        return super.getModelServiceLocator();
    }
}
