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

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.delta.DiffUtil;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.DropDownFormGroup;
import com.evolveum.midpoint.web.component.form.TextAreaFormGroup;
import com.evolveum.midpoint.web.component.form.TextFormGroup;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.PrismPropertyModel;
import com.evolveum.midpoint.web.component.wizard.WizardStep;
import com.evolveum.midpoint.web.component.wizard.resource.dto.ConnectorHostTypeComparator;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.web.util.WebModelUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;

import java.util.*;

/**
 * @author lazyman
 */
public class NameStep extends WizardStep {

    private static final Trace LOGGER = TraceManager.getTrace(NameStep.class);

    private static final String DOT_CLASS = NameStep.class.getName() + ".";
    private static final String OPERATION_DISCOVER_CONNECTORS = DOT_CLASS + "discoverConnectors";
    private static final String OPERATION_SAVE_RESOURCE = DOT_CLASS + "saveResource";

    private static final String ID_NAME = "name";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_LOCATION = "location";
    private static final String ID_CONNECTOR_TYPE = "connectorType";
    private static final String ID_CONNECTOR_VERSION = "connectorVersion";

    private static final ConnectorHostType NOT_USED_HOST = new ConnectorHostType();

    private IModel<PrismObject<ResourceType>> resourceModel;

    //all objects
    private LoadableModel<List<PrismObject<ConnectorHostType>>> connectorHostsModel;
    private LoadableModel<List<PrismObject<ConnectorType>>> connectorsModel;

    //filtered, based on selection
    private LoadableModel<List<PrismObject<ConnectorType>>> connectorTypes;

    public NameStep(IModel<PrismObject<ResourceType>> model) {
        this.resourceModel = model;

        connectorsModel = new LoadableModel<List<PrismObject<ConnectorType>>>(false) {

            @Override
            protected List<PrismObject<ConnectorType>> load() {
                return WebModelUtils.searchObjects(ConnectorType.class, null, null, getPageBase());
            }
        };
        connectorHostsModel = new LoadableModel<List<PrismObject<ConnectorHostType>>>(false) {

            @Override
            protected List<PrismObject<ConnectorHostType>> load() {
                return WebModelUtils.searchObjects(ConnectorHostType.class, null, null, getPageBase());
            }
        };

        initLayout();
    }

    private void initLayout() {
        TextFormGroup name = new TextFormGroup(ID_NAME, new PrismPropertyModel(resourceModel, UserType.F_NAME),
                createStringResource("NameStep.name"), "col-md-3", "col-md-3", true);
        add(name);

        TextAreaFormGroup description = new TextAreaFormGroup(ID_DESCRIPTION,
                new PrismPropertyModel(resourceModel, UserType.F_DESCRIPTION),
                createStringResource("NameStep.description"), "col-md-3", "col-md-3");
        description.setRows(3);
        add(description);

        DropDownFormGroup<PrismObject<ConnectorHostType>> location = createLocationDropDown();
        add(location);

        DropDownFormGroup<PrismObject<ConnectorType>> connectorType = createConnectorTypeDropDown(location.getModel());
        add(connectorType);
    }

    private IModel<PrismObject<ConnectorHostType>> createConnectorHostModel() {
        return new IModel<PrismObject<ConnectorHostType>>() {

            private PrismObject<ConnectorHostType> connectorHost;

            @Override
            public PrismObject<ConnectorHostType> getObject() {
                if (connectorHost != null) {
                    return connectorHost;
                }

                PrismObject<ResourceType> resource = resourceModel.getObject();
                PrismReference ref = resource.findReference(ResourceType.F_CONNECTOR_REF);
                if (ref == null || ref.getOid() == null) {
                    connectorHost = null;
                    return connectorHost;
                }

                PrismObject<ConnectorType> connector = null;
                List<PrismObject<ConnectorType>> connectors = connectorsModel.getObject();
                for (PrismObject<ConnectorType> conn : connectors) {
                    if (ref.getOid().equals(conn.getOid())) {
                        connector = conn;
                        break;
                    }
                }

                if (connector == null || connector.findReference(ConnectorType.F_CONNECTOR_HOST_REF) == null) {
                    connectorHost = null;
                    return connectorHost;
                }

                PrismReference hostRef = connector.findReference(ConnectorType.F_CONNECTOR_HOST_REF);
                if (hostRef.getOid() == null) {
                    connectorHost = null;
                    return connectorHost;
                }

                for (PrismObject<ConnectorHostType> host : connectorHostsModel.getObject()) {
                    if (hostRef.getOid().equals(host.getOid())) {
                        connectorHost = host;
                        return connectorHost;
                    }
                }

                connectorHost = null;
                return connectorHost;
            }

            @Override
            public void setObject(PrismObject<ConnectorHostType> object) {
                connectorHost = object;
            }

            @Override
            public void detach() {
            }
        };
    }

    private PrismObject<ConnectorType> getConnectorFromResource(List<PrismObject<ConnectorType>> connectors) {
        PrismReference ref = resourceModel.getObject().findReference(ResourceType.F_CONNECTOR_REF);
        if (ref == null || ref.getOid() == null) {
            return null;
        }

        for (PrismObject<ConnectorType> connector : connectors) {
            if (ref.getOid().equals(connector.getOid())) {
                return connector;
            }
        }

        return null;
    }

    private IModel<PrismObject<ConnectorType>> createReadonlyUsedConnectorModel() {
        return new IModel<PrismObject<ConnectorType>>() {

            private PrismObject<ConnectorType> selected;

            @Override
            public PrismObject<ConnectorType> getObject() {
                if (selected != null) {
                    return selected;
                }

                List<PrismObject<ConnectorType>> connectors = connectorsModel.getObject();
                selected = getConnectorFromResource(connectors);

                return selected;
            }

            @Override
            public void setObject(PrismObject<ConnectorType> object) {
                selected = object;
            }

            @Override
            public void detach() {
            }
        };
    }

    private DropDownFormGroup<PrismObject<ConnectorType>> createConnectorTypeDropDown(
            final IModel<PrismObject<ConnectorHostType>> hostModel) {
        connectorTypes = new LoadableModel<List<PrismObject<ConnectorType>>>(false) {

            @Override
            protected List<PrismObject<ConnectorType>> load() {
                return loadConnectorTypes(hostModel.getObject());
            }
        };

        return new DropDownFormGroup<PrismObject<ConnectorType>>(
                ID_CONNECTOR_TYPE, createReadonlyUsedConnectorModel(), connectorTypes,
                new IChoiceRenderer<PrismObject<ConnectorType>>() {

                    @Override
                    public Object getDisplayValue(PrismObject<ConnectorType> object) {
                        return WebMiscUtil.getName(object);
                    }

                    @Override
                    public String getIdValue(PrismObject<ConnectorType> object, int index) {
                        return Integer.toString(index);
                    }
                }, createStringResource("NameStep.connectorType"), "col-md-3", "col-md-3", true) {

            @Override
            protected DropDownChoice createDropDown(String id, IModel<List<PrismObject<ConnectorType>>> choices,
                                                    IChoiceRenderer<PrismObject<ConnectorType>> renderer, boolean required) {
                DropDownChoice choice = super.createDropDown(id, choices, renderer, required);
                choice.setOutputMarkupId(true);

                return choice;
            }
        };
    }

    private DropDownFormGroup<PrismObject<ConnectorHostType>> createLocationDropDown() {
        return new DropDownFormGroup<PrismObject<ConnectorHostType>>(ID_LOCATION, createConnectorHostModel(),
                        connectorHostsModel, new IChoiceRenderer<PrismObject<ConnectorHostType>>() {

            @Override
            public Object getDisplayValue(PrismObject<ConnectorHostType> object) {
                if (object == null) {
                    return NameStep.this.getString("NameStep.hostNotUsed");
                }
                return ConnectorHostTypeComparator.getUserFriendlyName(object);
            }

            @Override
            public String getIdValue(PrismObject<ConnectorHostType> object, int index) {
                return Integer.toString(index);
            }
        },
                createStringResource("NameStep.connectorHost"), "col-md-3", "col-md-3", false) {

            @Override
            protected DropDownChoice createDropDown(String id, IModel<List<PrismObject<ConnectorHostType>>> choices,
                                                    IChoiceRenderer<PrismObject<ConnectorHostType>> renderer, boolean required) {
                DropDownChoice choice = super.createDropDown(id, choices, renderer, required);
                choice.add(new AjaxFormComponentUpdatingBehavior("onchange") {

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        discoverConnectorsPerformed(target);
                    }
                });
                return choice;
            }
        };
    }

    private List<PrismObject<ConnectorType>> loadConnectorTypes(PrismObject<ConnectorHostType> host) {
        List<PrismObject<ConnectorType>> filtered = filterConnectorTypes(host, null);

        Collections.sort(filtered, new Comparator<PrismObject<ConnectorType>>() {

            @Override
            public int compare(PrismObject<ConnectorType> c1, PrismObject<ConnectorType> c2) {
                String name1 = c1.getPropertyRealValue(ConnectorType.F_CONNECTOR_TYPE, String.class);
                String name2 = c2.getPropertyRealValue(ConnectorType.F_CONNECTOR_TYPE, String.class);

                return String.CASE_INSENSITIVE_ORDER.compare(name1, name2);
            }
        });

        return filtered;
    }

    private List<PrismObject<ConnectorType>> filterConnectorTypes(PrismObject<ConnectorHostType> host,
                                                                  PrismObject<ConnectorType> type) {
        List<PrismObject<ConnectorType>> connectors = connectorsModel.getObject();
        final String connectorType = type == null ? null :
                type.getPropertyRealValue(ConnectorType.F_CONNECTOR_TYPE, String.class);

        Set<String> alreadyAddedTypes = new HashSet<>();
        List<PrismObject<ConnectorType>> filtered = new ArrayList<>();
        for (PrismObject<ConnectorType> connector : connectors) {
            if (host != null && !isConnectorOnHost(connector, host)) {
                continue;
            }

            String cType = connector.getPropertyRealValue(ConnectorType.F_CONNECTOR_TYPE, String.class);
            if (connectorType != null && (!StringUtils.equals(connectorType, cType) || alreadyAddedTypes.contains(cType))) {
                //filter out connector if connectorType is not equal to parameter type.connectorType
                //and we remove same connector types if we filtering connector based on types...
                continue;
            }

            alreadyAddedTypes.add(cType);
            filtered.add(connector);
        }

        return filtered;
    }

    private boolean isConnectorOnHost(PrismObject<ConnectorType> connector, PrismObject<ConnectorHostType> host) {
        PrismReference hostRef = connector.findReference(ConnectorType.F_CONNECTOR_HOST_REF);
        if (hostRef == null && host == null) {
            return true;
        }

        if (hostRef != null && host == null) {
            return false;
        }

        if (hostRef != null && hostRef.getOid() != null && hostRef.getOid().equals(host.getOid())) {
            return true;
        }

        return false;
    }

    private void discoverConnectorsPerformed(AjaxRequestTarget target) {
        DropDownChoice<ConnectorHostType> location = (DropDownChoice) get(ID_LOCATION);
        ConnectorHostType host = location.getModelObject();

        if (!NOT_USED_HOST.equals(host)) {
            discoverConnectors(host);
            connectorsModel.reset();
        }

        connectorTypes.reset();

        PageBase page = (PageBase) getPage();

        DropDownFormGroup type = (DropDownFormGroup) get(ID_CONNECTOR_TYPE);
        DropDownFormGroup version = (DropDownFormGroup) get(ID_CONNECTOR_VERSION);

        target.add(type.getInput(), version.getInput(), page.getFeedbackPanel());
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
            PrismObject<ResourceType> resource = resourceModel.getObject();
            resource.findOrCreateContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
            resource.findOrCreateContainer(new ItemPath(ResourceType.F_CONNECTOR_CONFIGURATION, SchemaConstants.ICF_CONFIGURATION_PROPERTIES));
            page.getPrismContext().adopt(resource);

            ObjectDelta delta;
            if (StringUtils.isNotEmpty(resource.getOid())) {
                PrismObject<ResourceType> oldResource = WebModelUtils.loadObject(ResourceType.class, resource.getOid(),
                        result, page);

                delta = DiffUtil.diff(oldResource, resource);
            } else {
                delta = ObjectDelta.createAddDelta(resource);
            }

            WebModelUtils.save(delta, ModelExecuteOptions.createRaw(), result, page);

            resource = WebModelUtils.loadObject(ResourceType.class, delta.getOid(), result, page);
            resourceModel.setObject(resource);
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
}
