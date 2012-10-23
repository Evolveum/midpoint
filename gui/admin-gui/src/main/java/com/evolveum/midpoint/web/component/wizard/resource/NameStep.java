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
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;
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
    public static final String OPERATION_LOAD_CONNECTOR_HOSTS = DOT_CLASS + "loadConnectorHosts";

    private static final String ID_NAME = "name";
    private static final String ID_LOCATION = "location";
    private static final String ID_FRAMEWORK = "framework";
    private static final String ID_CONNECTOR_NAME = "connectorName";
    private static final String ID_CONNECTOR_VERSION = "connectorVersion";

    public NameStep(IModel<ResourceType> model) {
        initLayout(model);
    }

    private void initLayout(IModel<ResourceType> model) {
        RequiredTextField name = new RequiredTextField(ID_NAME, createNameModel(model));
        add(name);

        DropDownChoice location = new DropDownChoice(ID_LOCATION, new Model(), new LoadableModel<List<ConnectorHostType>>() {

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
        add(location);

        DropDownChoice framework = new DropDownChoice(ID_FRAMEWORK); //todo model
        add(framework);

        DropDownChoice connectorName = new DropDownChoice(ID_CONNECTOR_NAME); //todo model
        add(connectorName);

        DropDownChoice connectorVersion = new DropDownChoice(ID_CONNECTOR_VERSION); //todo model
        add(connectorVersion);
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
}
