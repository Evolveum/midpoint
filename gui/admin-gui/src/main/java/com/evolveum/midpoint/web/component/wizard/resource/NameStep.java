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
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ConnectorHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author lazyman
 */
public class NameStep extends WizardStep {

    public static final String DOT_CLASS = NameStep.class.getName() + ".";
    public static final String OPERATION_LOAD_CONNECTORS = DOT_CLASS + "loadConnectors";
    public static final String OPERATION_LOAD_CONNECTOR_HOSTS = DOT_CLASS + "loadConnectorHosts";

    private static final String ID_NAME = "name";
    private static final String ID_LOCATION = "location";
    private static final String ID_CONNECTOR_NAME = "connectorName";
    private static final String ID_CONNECTOR_VERSION = "connectorVersion";

    private LoadableModel<List<ConnectorType>> connectorsModel;
    private LoadableModel<ConnectorType> selectedConnectorModel;

    public NameStep(final IModel<ResourceType> model) {
        connectorsModel = new LoadableModel<List<ConnectorType>>(false) {

            @Override
            protected List<ConnectorType> load() {
                return loadConnectors();
            }
        };
        selectedConnectorModel = new LoadableModel<ConnectorType>() {

            @Override
            protected ConnectorType load() {
                return loadSelectedConnector(model);
            }
        };
        initLayout(model);
    }

    private void initLayout(IModel<ResourceType> model) {
        RequiredTextField name = new RequiredTextField(ID_NAME, createNameModel(model));
        add(name);

        DropDownChoice location = createLocationDropDown(model);
        add(location);

        DropDownChoice connectorName = createConnectorNameDropDown(model);
        add(connectorName);

        DropDownChoice connectorVersion = createConnectorVersionDropDown(model);
        add(connectorVersion);
    }

    private DropDownChoice createConnectorVersionDropDown(IModel<ResourceType> model) {
        DropDownChoice connectorVersion = new DropDownChoice(ID_CONNECTOR_VERSION); //todo model
        connectorVersion.setOutputMarkupId(true);

        return connectorVersion;
    }

    private DropDownChoice createConnectorNameDropDown(IModel<ResourceType> model) {
        DropDownChoice connectorName = new DropDownChoice(ID_CONNECTOR_NAME, new Model(), new LoadableModel(false) {

            @Override
            protected Object load() {
                return loadConnectors();
            }
        }); //todo model
        connectorName.setOutputMarkupId(true);
        connectorName.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                refreshVersionPerformed(target);
            }
        });

        return connectorName;
    }

    private DropDownChoice createLocationDropDown(IModel<ResourceType> model) {
        DropDownChoice location = new DropDownChoice(ID_LOCATION, new Model(), new LoadableModel<List<ConnectorHostType>>(false) {

            @Override
            protected List<ConnectorHostType> load() {
                return loadConnectorHosts();
            }
        }, new IChoiceRenderer<ConnectorHostType>() {

            @Override
            public Object getDisplayValue(ConnectorHostType object) {
                if (object == null) {
                    return NameStep.this.getString("NameStep.hostNotUsed");
                }
                return ConnectorHostTypeComparator.getUserFriendlyName(object);
            }

            @Override
            public String getIdValue(ConnectorHostType object, int index) {
                return Integer.toString(index);
            }
        }
        );
        location.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                discoverConnectorsPerformed(target);
            }
        });

        return location;
    }

    private List<ConnectorType> loadConnectors() {
        List<ConnectorType> connectors = new ArrayList<ConnectorType>();

        OperationResult result = new OperationResult(OPERATION_LOAD_CONNECTORS);
        try {
            PageBase page = (PageBase) getPage();
            ModelService model = page.getModelService();

            List<PrismObject<ConnectorType>> objects = model.searchObjects(ConnectorType.class, null,
                    page.createSimpleTask(OPERATION_LOAD_CONNECTORS), result);

            for (PrismObject<ConnectorType> connector : objects) {
                connectors.add(connector.asObjectable());
            }

            Collections.sort(connectors, new ConnectorTypeComparator());
        } catch (Exception ex) {
            ex.printStackTrace();
            //todo error handling
        }

        return connectors;
    }

    private IModel<String> createNameModel(final IModel<ResourceType> model) {
        return new Model<String>() {

            @Override
            public String getObject() {
                return WebMiscUtil.getOrigStringFromPoly(model.getObject().getName());
            }

            @Override
            public void setObject(String object) {
                ResourceType resource = model.getObject();
                PolyStringType name = resource.getName();
                if (name == null) {
                    name = new PolyStringType();
                    resource.setName(name);
                }

                name.setOrig(object);
            }
        };
    }

    private List<ConnectorHostType> loadConnectorHosts() {
        List<ConnectorHostType> hosts = new ArrayList<ConnectorHostType>();

        OperationResult result = new OperationResult(OPERATION_LOAD_CONNECTOR_HOSTS);
        try {
            PageBase page = (PageBase) getPage();
            ModelService model = page.getModelService();
            List<PrismObject<ConnectorHostType>> objects = model.searchObjects(ConnectorHostType.class, null,
                    page.createSimpleTask(OPERATION_LOAD_CONNECTOR_HOSTS), result);

            for (PrismObject<ConnectorHostType> host : objects) {
                hosts.add(host.asObjectable());
            }

            Collections.sort(hosts, new ConnectorHostTypeComparator());
        } catch (Exception ex) {
            //todo error handling
            ex.printStackTrace();
        }

        //for localhost
        hosts.add(0, null);

        return hosts;
    }

    private void refreshVersionPerformed(AjaxRequestTarget target) {
        //todo implement

        target.add(NameStep.this.get(ID_CONNECTOR_VERSION));
    }

    private void discoverConnectorsPerformed(AjaxRequestTarget target) {
        //todo implement

        connectorsModel.reset();
        target.add(NameStep.this.get(ID_CONNECTOR_NAME), NameStep.this.get(ID_CONNECTOR_VERSION));
    }

    private ConnectorType loadSelectedConnector(IModel<ResourceType> model) {
        //todo implement
        return null;
    }
}
