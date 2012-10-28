/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component.wizard.resource;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.PrismContainerPanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceType;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.model.IModel;

/**
 * @author lazyman
 */
public class ConfigurationStep extends WizardStep {

    private static final Trace LOGGER = TraceManager.getTrace(ConfigurationStep.class);
    private static final String ID_CONFIGURATION_PROPERTIES = "configurationProperties";
    private static final String ID_CONNECTOR_POOL_CONFIGURATION = "connectorPoolConfiguration";
    private static final String ID_TIMEOUTS = "timeouts";

    private IModel<ResourceType> resourceModel;

    public ConfigurationStep(IModel<ResourceType> resourceModel) {
        this.resourceModel = resourceModel;

        initLayout(resourceModel);
    }

    private void initLayout(IModel<ResourceType> resourceModel) {
        IModel<ContainerWrapper> wrapperModel = new LoadableModel<ContainerWrapper>(false) {

            @Override
            protected ContainerWrapper load() {
                //todo remove this container wrapper hack, this method is really ugly

                try {
                    PrismObject<ResourceType> resource = ConfigurationStep.this.resourceModel.getObject().asPrismObject();
                    PrismContainer connectorConfiguration = resource.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
                    ContainerStatus status = ContainerStatus.MODIFYING;
                    if (connectorConfiguration == null) {
                        PrismContainerDefinition definition = resource.getDefinition().findContainerDefinition(
                                ResourceType.F_CONNECTOR_CONFIGURATION);
                        connectorConfiguration = definition.instantiate();
                        status = ContainerStatus.ADDING;
                    }

                    PrismReferenceValue connectorRef = resource.findReference(ResourceType.F_CONNECTOR_REF).getValue();
                    String connectorOid = connectorRef.getOid();

                    PageBase page = (PageBase) ConfigurationStep.this.getPage();
                    ModelService model = page.getModelService();
                    PrismObject<ConnectorType> connector = model.getObject(ConnectorType.class, connectorOid, null,
                            page.createSimpleTask("load connector"), new OperationResult("load connector"));



                    ObjectWrapper wrapper = new ObjectWrapper(null, null, resource, ContainerStatus.MODIFYING);

                    return new ContainerWrapper(wrapper, connectorConfiguration, status, null);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                return null;
            }
        };
        PrismContainerPanel configurationProperties = new PrismContainerPanel(ID_CONFIGURATION_PROPERTIES,
                wrapperModel, null);
        add(configurationProperties);

//        PrismContainerPanel connectorPoolConfiguration = new PrismContainerPanel(ID_CONNECTOR_POOL_CONFIGURATION,
//                wrapperModel, null);
//        add(connectorPoolConfiguration);
//
//        PrismContainerPanel timeouts = new PrismContainerPanel(ID_TIMEOUTS, wrapperModel, null);
//        add(timeouts);
    }
}
