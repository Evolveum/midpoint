/*
 * Copyright (c) 2016-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import java.util.List;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.ConnectorOperationalStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * @author semancik
 */
@PanelType(name = "resourceConnector")
@PanelInstance(identifier = "resourceConnector", applicableForOperation = OperationTypeType.MODIFY, applicableForType = ResourceType.class,
        display = @PanelDisplay(label = "PageResource.tab.connector.status", icon = "fa fa-plug", order = 100))
public class ResourceConnectorPanel extends AbstractObjectMainPanel<ResourceType, ResourceDetailsModel> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ResourceConnectorPanel.class);

    private static final String DOT_CLASS = ResourceConnectorPanel.class.getName() + ".";
    private static final String OPERATION_GET_CONNECTOR_OPERATIONAL_STATUS = DOT_CLASS + "getConnectorOperationalStatus";

    private static final String ID_CONNECTOR_LIST = "connectorList";
    private static final String ID_CONNECTOR_NAME = "connectorName";
    private static final String ID_CONNECOTR_CLASS = "connectorClass";
    private static final String ID_POOL_CONFIG_MIN_SIZE = "poolConfigMinSize";
    private static final String ID_POOL_CONFIG_MAX_SIZE = "poolConfigMaxSize";
    private static final String ID_POOL_CONFIG_MIN_IDLE = "poolConfigMinIdle";
    private static final String ID_POOL_CONFIG_MAX_IDLE = "poolConfigMaxIdle";
    private static final String ID_POOL_CONFIG_WAIT_TIMEOUT = "poolConfigWaitTimeout";
    private static final String ID_POOL_CONFIG_MIN_EVICTABLE_IDLE_TIME = "poolConfigMinEvictableIdleTime";
    private static final String ID_POOL_CONFIG_MAX_IDLE_TIME = "poolConfigMaxIdleTime";
    private static final String ID_POOL_STATUS_NUM_IDLE = "poolStatusNumIdle";
    private static final String ID_POOL_STATUS_NUM_ACTIVE = "poolStatusNumActive";


    public ResourceConnectorPanel(String id, ResourceDetailsModel model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    protected void initLayout() {
        setOutputMarkupId(true);

        IModel<List<ConnectorOperationalStatus>> statsModel = new IModel<>() {
            private static final long serialVersionUID = 1L;

            @Override
            public List<ConnectorOperationalStatus> getObject() {
                Task task = getPageBase().createSimpleTask(OPERATION_GET_CONNECTOR_OPERATIONAL_STATUS);
                OperationResult result = task.getResult();
                List<ConnectorOperationalStatus> status = null;
                try {
                    status = getPageBase().getModelInteractionService().getConnectorOperationalStatus(getObjectWrapper().getOid(), task, result);
                } catch (SchemaException | ObjectNotFoundException | CommunicationException
                        | ConfigurationException | ExpressionEvaluationException e) {
                    LOGGER.error("Error getting connector status for {}: {}", getObjectWrapper(), e.getMessage(), e);
                    getPageBase().showResult(result);
                }
                return status;
            }
        };

        ListView<ConnectorOperationalStatus> listview = new ListView<>(ID_CONNECTOR_LIST, statsModel) {
            private static final long serialVersionUID = 1L;

            protected void populateItem(ListItem<ConnectorOperationalStatus> item) {
//                item.add(new Label("label", item.getModelService()));
                IModel<ConnectorOperationalStatus> statModel = item.getModel();
                item.add(createLabel(statModel, ID_CONNECTOR_NAME, ConnectorOperationalStatus.F_CONNECTOR_NAME));
                item.add(createLabel(statModel, ID_CONNECOTR_CLASS, ConnectorOperationalStatus.F_CONNECTOR_CLASS_NAME));
                item.add(createLabel(statModel, ID_POOL_CONFIG_MIN_SIZE, ConnectorOperationalStatus.F_POOL_CONFIG_MIN_SIZE));
                item.add(createLabel(statModel, ID_POOL_CONFIG_MAX_SIZE, ConnectorOperationalStatus.F_POOL_CONFIG_MAX_SIZE));
                item.add(createLabel(statModel, ID_POOL_CONFIG_MIN_IDLE, ConnectorOperationalStatus.F_POOL_CONFIG_MIN_IDLE));
                item.add(createLabel(statModel, ID_POOL_CONFIG_MAX_IDLE, ConnectorOperationalStatus.F_POOL_CONFIG_MAX_IDLE));
                item.add(createLabel(statModel, ID_POOL_CONFIG_WAIT_TIMEOUT, ConnectorOperationalStatus.F_POOL_CONFIG_WAIT_TIMEOUT));
                item.add(createLabel(statModel, ID_POOL_CONFIG_MIN_EVICTABLE_IDLE_TIME, ConnectorOperationalStatus.F_POOL_CONFIG_MIN_EVICTABLE_IDLE_TIME));
                item.add(createLabel(statModel, ID_POOL_CONFIG_MAX_IDLE_TIME, ConnectorOperationalStatus.F_POOL_CONFIG_MAX_IDLE_TIME));
                item.add(createLabel(statModel, ID_POOL_STATUS_NUM_IDLE, ConnectorOperationalStatus.F_POOL_STATUS_NUM_IDLE));
                item.add(createLabel(statModel, ID_POOL_STATUS_NUM_ACTIVE, ConnectorOperationalStatus.F_POOL_STATUS_NUM_ACTIVE));

            }
        };
        add(listview);

    }

    private Label createLabel(IModel<ConnectorOperationalStatus> statsModel, String id, String fieldName) {
        return new Label(id, new PropertyModel<String>(statsModel, fieldName));
    }
}
