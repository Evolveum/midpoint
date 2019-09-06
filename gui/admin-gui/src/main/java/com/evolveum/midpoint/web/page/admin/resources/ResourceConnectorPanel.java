/*
 * Copyright (c) 2016-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.resources;

import java.util.List;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.ConnectorOperationalStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

/**
 * @author semancik
 */
public class ResourceConnectorPanel extends Panel {
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
	private static final String ID_POOL_STATUS_NUM_IDLE = "poolStatusNumIdle";
	private static final String ID_POOL_STATUS_NUM_ACTIVE = "poolStatusNumActive";

	private PageBase parentPage;

	public ResourceConnectorPanel(String id, ShadowKindType kind,
			final IModel<PrismObject<ResourceType>> model, PageBase parentPage) {
		super(id, model);
		this.parentPage = parentPage;

		initLayout(model, parentPage);
	}


	private void initLayout(final IModel<PrismObject<ResourceType>> model, final PageBase parentPage) {
		setOutputMarkupId(true);

		IModel<List<ConnectorOperationalStatus>> statsModel = new IModel<List<ConnectorOperationalStatus>>() {
			private static final long serialVersionUID = 1L;

			@Override
			public List<ConnectorOperationalStatus> getObject() {
				PrismObject<ResourceType> resource = model.getObject();
				Task task = parentPage.createSimpleTask(OPERATION_GET_CONNECTOR_OPERATIONAL_STATUS);
				OperationResult result = task.getResult();
				List<ConnectorOperationalStatus> status = null;
				try {
					status = parentPage.getModelInteractionService().getConnectorOperationalStatus(resource.getOid(), task, result);
				} catch (SchemaException | ObjectNotFoundException | CommunicationException
						| ConfigurationException | ExpressionEvaluationException e) {
					LOGGER.error("Error getting connector status for {}: {}", resource, e.getMessage(), e);
					parentPage.showResult(result);
				}
				return status;
			}
		};

		ListView<ConnectorOperationalStatus> listview = new ListView<ConnectorOperationalStatus>(ID_CONNECTOR_LIST, statsModel) {
			private static final long serialVersionUID = 1L;

			protected void populateItem(ListItem<ConnectorOperationalStatus> item) {
		        item.add(new Label("label", item.getModel()));
		        IModel<ConnectorOperationalStatus> statModel = item.getModel();
		        item.add(createLabel(statModel, ID_CONNECTOR_NAME,  ConnectorOperationalStatus.F_CONNECTOR_NAME));
		        item.add(createLabel(statModel, ID_CONNECOTR_CLASS,  ConnectorOperationalStatus.F_CONNECTOR_CLASS_NAME));
		        item.add(createLabel(statModel, ID_POOL_CONFIG_MIN_SIZE,  ConnectorOperationalStatus.F_POOL_CONFIG_MIN_SIZE));
		        item.add(createLabel(statModel, ID_POOL_CONFIG_MAX_SIZE,  ConnectorOperationalStatus.F_POOL_CONFIG_MAX_SIZE));
		        item.add(createLabel(statModel, ID_POOL_CONFIG_MIN_IDLE,  ConnectorOperationalStatus.F_POOL_CONFIG_MIN_IDLE));
		        item.add(createLabel(statModel, ID_POOL_CONFIG_MAX_IDLE,  ConnectorOperationalStatus.F_POOL_CONFIG_MAX_IDLE));
		        item.add(createLabel(statModel, ID_POOL_CONFIG_WAIT_TIMEOUT,  ConnectorOperationalStatus.F_POOL_CONFIG_WAIT_TIMEOUT));
		        item.add(createLabel(statModel, ID_POOL_CONFIG_MIN_EVICTABLE_IDLE_TIME,  ConnectorOperationalStatus.F_POOL_CONFIG_MIN_EVICTABLE_IDLE_TIME));
		        item.add(createLabel(statModel, ID_POOL_STATUS_NUM_IDLE,  ConnectorOperationalStatus.F_POOL_STATUS_NUM_IDLE));
		        item.add(createLabel(statModel, ID_POOL_STATUS_NUM_ACTIVE,  ConnectorOperationalStatus.F_POOL_STATUS_NUM_ACTIVE));

		    }
		};
		add(listview);

	}

	private Label createLabel(IModel<ConnectorOperationalStatus> statsModel, String id, String fieldName) {
		return new Label(id, new PropertyModel<String>(statsModel, fieldName));
	}
}
