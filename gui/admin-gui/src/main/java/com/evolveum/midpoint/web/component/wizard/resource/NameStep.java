/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.wizard.resource;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.component.wizard.WizardStep;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConnectorHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.util.*;

/**
 * @author lazyman
 */
public class NameStep extends WizardStep {

    private static final Trace LOGGER = TraceManager.getTrace(NameStep.class);

    private static final String DOT_CLASS = NameStep.class.getName() + ".";
    private static final String OPERATION_LOAD_CONNECTORS = DOT_CLASS + "loadConnectors";
    private static final String OPERATION_LOAD_CONNECTOR_HOSTS = DOT_CLASS + "loadConnectorHosts";
    private static final String OPERATION_DISCOVER_CONNECTORS = DOT_CLASS + "discoverConnectors";
    private static final String OPERATION_SAVE_RESOURCE = DOT_CLASS + "saveResource";
    private static final String OPERATION_LOAD_RESOURCE = DOT_CLASS + "loadResource";

    private static final String ID_NAME = "name";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_LOCATION = "location";
    private static final String ID_CONNECTOR_TYPE = "connectorType";
    private static final String ID_CONNECTOR_VERSION = "connectorVersion";

    private static final ConnectorHostType NOT_USED_HOST = new ConnectorHostType();

    private IModel<ResourceType> resourceModel;

    private LoadableModel<List<ConnectorHostType>> connectorHostsModel;
    private LoadableModel<List<ConnectorType>> connectorsModel;

    private LoadableModel<List<ConnectorType>> connectorTypes;
    private LoadableModel<List<ConnectorType>> connectorVersions;

    public NameStep(final IModel<ResourceType> model) {
        this.resourceModel = model;

        connectorsModel = new LoadableModel<List<ConnectorType>>(false) {

            @Override
            protected List<ConnectorType> load() {
                return loadConnectors();
            }
        };
        connectorHostsModel = new LoadableModel<List<ConnectorHostType>>(false) {

            @Override
            protected List<ConnectorHostType> load() {
                return loadConnectorHosts();
            }
        };

        initLayout(model);
    }

    private void initLayout(IModel<ResourceType> model) {
        RequiredTextField name = new RequiredTextField(ID_NAME, createNameModel(model));
        add(name);

        TextArea description = new TextArea(ID_DESCRIPTION, new PropertyModel(model, "description"));
        add(description);

        DropDownChoice<ConnectorHostType> location = createLocationDropDown(model);
        add(location);

        DropDownChoice<ConnectorType> connectorType = createConnectorTypeDropDown(model, location.getModel());
        add(connectorType);

        DropDownChoice<ConnectorType> connectorVersion = createConnectorVersionDropDown(model, location.getModel(),
                connectorType.getModel());
        add(connectorVersion);
    }

    private DropDownChoice createConnectorVersionDropDown(IModel<ResourceType> model,
                                                          final IModel<ConnectorHostType> connectorHostTypeModel,
                                                          final IModel<ConnectorType> connectorTypeModel) {
        connectorVersions = new LoadableModel<List<ConnectorType>>(false) {

            @Override
            protected List<ConnectorType> load() {
                return loadConnectorVersions(connectorHostTypeModel.getObject(), connectorTypeModel.getObject());
            }
        };

        DropDownChoice connectorVersion = new DropDownChoice(ID_CONNECTOR_VERSION, createUsedConnectorModel(model),
                connectorVersions, new IChoiceRenderer<ConnectorType>() {

            @Override
            public Object getDisplayValue(ConnectorType object) {
                String version = object.getConnectorVersion();
                if (StringUtils.isEmpty(version)) {
                    return NameStep.this.getString("NameStep.unknownVersion");
                }

                return version;
            }

            @Override
            public String getIdValue(ConnectorType object, int index) {
                return Integer.toString(index);
            }
        });
        connectorVersion.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
            }
        });
        connectorVersion.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isEnabled() {
                return connectorTypeModel.getObject() != null;
            }
        });
        connectorVersion.setOutputMarkupId(true);

        return connectorVersion;
    }

    private IModel<ConnectorHostType> createReadonlyConnectorHostModel(final IModel<ResourceType> model) {
        return new IModel<ConnectorHostType>() {

            private ConnectorHostType connectorHost;

            @Override
            public ConnectorHostType getObject() {
                if (connectorHost != null) {
                    return connectorHost;
                }

                ResourceType resource = model.getObject();
                ObjectReferenceType ref = resource.getConnectorRef();
                if (ref == null || ref.getOid() == null) {
                    connectorHost = NOT_USED_HOST;
                    return connectorHost;
                }

                ConnectorType connector = null;
                List<ConnectorType> connectors = connectorsModel.getObject();
                for (ConnectorType conn : connectors) {
                    if (ref.getOid().equals(conn.getOid())) {
                        connector = conn;
                        break;
                    }
                }

                if (connector == null || connector.getConnectorHostRef() == null) {
                    connectorHost = NOT_USED_HOST;
                    return connectorHost;
                }

                ObjectReferenceType hostRef = connector.getConnectorHostRef();
                if (hostRef.getOid() == null) {
                    connectorHost = NOT_USED_HOST;
                    return connectorHost;
                }

                for (ConnectorHostType host : connectorHostsModel.getObject()) {
                    if (hostRef.getOid().equals(host.getOid())) {
                        connectorHost = host;
                        return connectorHost;
                    }
                }

                connectorHost = NOT_USED_HOST;
                return connectorHost;
            }

            @Override
            public void setObject(ConnectorHostType object) {
                connectorHost = object;
            }

            @Override
            public void detach() {
            }
        };
    }

    private ConnectorType getConnectorFromResource(ResourceType resource, List<ConnectorType> connectors) {
        ObjectReferenceType ref = resource.getConnectorRef();
        if (ref == null || ref.getOid() == null) {
            return null;
        }

        for (ConnectorType connector : connectors) {
            if (ref.getOid().equals(connector.getOid())) {
                return connector;
            }
        }

        return null;
    }

    private IModel<ConnectorType> createUsedConnectorModel(final IModel<ResourceType> model) {
        return new IModel<ConnectorType>() {

            @Override
            public ConnectorType getObject() {
                ResourceType resource = model.getObject();
                List<ConnectorType> connectors = connectorsModel.getObject();
                return getConnectorFromResource(resource, connectors);
            }

            @Override
            public void setObject(ConnectorType object) {
                ResourceType resource = model.getObject();
                if (object == null) {
                    resource.setConnectorRef(null);
                    return;
                }

//                ObjectReferenceType ref = new ObjectReferenceType();
//                ref.setType(ConnectorType.COMPLEX_TYPE);
//                ref.setOid(object.getOid());

//                resource.setConnectorRef(ref);

                //todo remove
                resource.setConnector(object);
            }

            @Override
            public void detach() {
            }
        };
    }

    private IModel<ConnectorType> createReadonlyUsedConnectorModel(final IModel<ResourceType> model) {
        return new IModel<ConnectorType>() {

            private ConnectorType selected;

            @Override
            public ConnectorType getObject() {
                if (selected != null) {
                    return selected;
                }

                ResourceType resource = model.getObject();
                List<ConnectorType> connectors = connectorsModel.getObject();
                selected = getConnectorFromResource(resource, connectors);

                return selected;
            }

            @Override
            public void setObject(ConnectorType object) {
                selected = object;
            }

            @Override
            public void detach() {
            }
        };
    }

    private DropDownChoice createConnectorTypeDropDown(IModel<ResourceType> model,
                                                       final IModel<ConnectorHostType> hostModel) {
        connectorTypes = new LoadableModel(false) {

            @Override
            protected Object load() {
                return loadConnectorTypes(hostModel.getObject());
            }
        };

        DropDownChoice connectorName = new DropDownChoice(ID_CONNECTOR_TYPE, createReadonlyUsedConnectorModel(model),
                connectorTypes, new IChoiceRenderer<ConnectorType>() {

            @Override
            public Object getDisplayValue(ConnectorType object) {
                return WebMiscUtil.getName(object);
            }

            @Override
            public String getIdValue(ConnectorType object, int index) {
                return Integer.toString(index);
            }
        }
        );
        connectorName.setOutputMarkupId(true);
        connectorName.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                changeConnectorTypePerformed(target);
            }
        });

        return connectorName;
    }

    private DropDownChoice createLocationDropDown(IModel<ResourceType> model) {
        DropDownChoice location = new DropDownChoice(ID_LOCATION, createReadonlyConnectorHostModel(model),
                connectorHostsModel, new IChoiceRenderer<ConnectorHostType>() {

            @Override
            public Object getDisplayValue(ConnectorHostType object) {
                if (NOT_USED_HOST.equals(object )) {
                    return NameStep.this.getString("NameStep.hostNotUsed");
                }
                return ConnectorHostTypeComparator.getUserFriendlyName(object);
            }

            @Override
            public String getIdValue(ConnectorHostType object, int index) {
                return Integer.toString(index);
            }
        }
        ) {


        };
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

            List<PrismObject<ConnectorType>> objects = model.searchObjects(ConnectorType.class, null, null,
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

    private List<ConnectorType> loadConnectorTypes(ConnectorHostType host) {
        List<ConnectorType> filtered = filterConnectorTypes(host, null);

        Collections.sort(filtered, new Comparator<ConnectorType>() {

            @Override
            public int compare(ConnectorType c1, ConnectorType c2) {
                String name1 = c1.getConnectorType() == null ? "" : c1.getConnectorType();
                String name2 = c2.getConnectorType() == null ? "" : c2.getConnectorType();

                return String.CASE_INSENSITIVE_ORDER.compare(name1, name2);
            }
        });

        return filtered;
    }

    private List<ConnectorType> filterConnectorTypes(ConnectorHostType host, ConnectorType type) {
        List<ConnectorType> connectors = connectorsModel.getObject();

        Set<String> alreadyAddedTypes = new HashSet<String>();
        List<ConnectorType> filtered = new ArrayList<ConnectorType>();
        for (ConnectorType connector : connectors) {
            if (!NOT_USED_HOST.equals(host) && !isConnectorOnHost(connector, host)) {
                continue;
            }

            if (type != null && type.getConnectorType() != null
                    && !type.getConnectorType().equals(connector.getConnectorType())) {
                //filter out connector if connectorType is not equal to parameter type.connectorType
                continue;
            }

            if (type == null && alreadyAddedTypes.contains(connector.getConnectorType())) {
                //we remove same connector types if we filtering connector based on types...
                continue;
            }

            alreadyAddedTypes.add(connector.getConnectorType());
            filtered.add(connector);
        }

        return filtered;
    }

    private List<ConnectorType> loadConnectorVersions(ConnectorHostType host, ConnectorType type) {
        List<ConnectorType> filtered = filterConnectorTypes(host, type);

        Collections.sort(filtered, new Comparator<ConnectorType>() {
            @Override
            public int compare(ConnectorType c1, ConnectorType c2) {
                String v1 = c1.getVersion() == null ? "" : c1.getVersion();
                String v2 = c2.getVersion() == null ? "" : c2.getVersion();

                return String.CASE_INSENSITIVE_ORDER.compare(v1, v2);
            }
        });

        return filtered;
    }

    private boolean isConnectorOnHost(ConnectorType connector, ConnectorHostType host) {
        ConnectorHostType cHost = connector.getConnectorHost();

        if (cHost != null && cHost.getOid() != null && cHost.getOid().equals(host.getOid())) {
            return true;
        }

        ObjectReferenceType ref = connector.getConnectorHostRef();
        if (ref == null) {
            return false;
        }

        if (ref.getOid() != null && ref.getOid().equals(host.getOid())) {
            return true;
        }

        return false;
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
                PolyStringType name = new PolyStringType();
                name.setOrig(object);

                resource.setName(name);
            }
        };
    }

    private List<ConnectorHostType> loadConnectorHosts() {
        List<ConnectorHostType> hosts = new ArrayList<ConnectorHostType>();

        OperationResult result = new OperationResult(OPERATION_LOAD_CONNECTOR_HOSTS);
        try {
            PageBase page = (PageBase) getPage();
            ModelService model = page.getModelService();
            List<PrismObject<ConnectorHostType>> objects = model.searchObjects(ConnectorHostType.class, null, null,
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
        hosts.add(0, NOT_USED_HOST);

        return hosts;
    }

    private void changeConnectorTypePerformed(AjaxRequestTarget target) {
        connectorVersions.reset();

        DropDownChoice<ConnectorType> version = (DropDownChoice) get(ID_CONNECTOR_VERSION);
//        version.getModel().setObject(null);

        target.add(version);
    }

    private void discoverConnectorsPerformed(AjaxRequestTarget target) {
        DropDownChoice<ConnectorHostType> location = (DropDownChoice) get(ID_LOCATION);
        ConnectorHostType host = location.getModelObject();

        if (!NOT_USED_HOST.equals(host)) {
            discoverConnectors(host);
            connectorsModel.reset();
        }

        connectorTypes.reset();
        connectorVersions.reset();

        PageBase page = (PageBase) getPage();

        target.add(get(ID_CONNECTOR_TYPE), get(ID_CONNECTOR_VERSION), page.getFeedbackPanel());
    }

    private void discoverConnectors(ConnectorHostType host) {
        PageBase page = (PageBase) getPage();
        OperationResult result = new OperationResult(OPERATION_DISCOVER_CONNECTORS);
        try {
            ModelService model = page.getModelService();
            model.discoverConnectors(host, result);
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't discover connectors", ex);
        } finally {
            result.recomputeStatus();
        }

        if (WebMiscUtil.showResultInPage(result)) {
            page.showResult(result);
        }
    }

    @Override
    public void applyState() {
        PageBase page = (PageBase) getPage();
        OperationResult result = new OperationResult(OPERATION_SAVE_RESOURCE);
        try {
            PrismObject<ResourceType> resource = resourceModel.getObject().asPrismObject();
            page.getPrismContext().adopt(resource);

            ModelService model = page.getModelService();
            ObjectDelta addDelta = ObjectDelta.createAddDelta(resource);
            model.executeChanges(WebMiscUtil.createDeltaCollection(addDelta), null,
                    page.createSimpleTask(OPERATION_SAVE_RESOURCE), result);

            OperationResult loadResult = result.createSubresult(OPERATION_LOAD_RESOURCE);
            resource = model.getObject(ResourceType.class, addDelta.getOid(), null,
                    page.createSimpleTask(OPERATION_LOAD_RESOURCE), loadResult);
            resourceModel.setObject(resource.asObjectable());
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't save resource", ex);
            result.recordFatalError("Couldn't save resource, reason: " + ex.getMessage(), ex);
        } finally {
            result.computeStatus();
        }

        if (WebMiscUtil.showResultInPage(result)) {
            page.showResult(result);
        }
    }



    @Override
    public boolean isComplete() {
//        DropDownChoice<ConnectorType> version = (DropDownChoice) get(ID_CONNECTOR_VERSION);
//        return version.getModelObject() != null;
        return true;
    }
}
