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

package com.evolveum.midpoint.web.page.admin.help;

import com.evolveum.midpoint.schema.LabeledString;
import com.evolveum.midpoint.schema.RepositoryDiag;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author mserbak
 */
public class PageSystem extends PageAdminHelp {

    private static final Trace LOGGER = TraceManager.getTrace(PageSystem.class);

    private static final String DOT_CLASS = PageSystem.class.getName() + ".";
    private static final String OPERATION_TEST_REPOSITORY = DOT_CLASS + "testRepository";
    private static final String OPERATION_TEST_PROVISIONING = DOT_CLASS + "testProvisioning";
    private static final String OPERATION_GET_REPO_DIAG = DOT_CLASS + "getRepoDiag";

    private static final String ID_REVISION = "revision";
    private static final String ID_PROPERTY = "property";
    private static final String ID_VALUE = "value";
    private static final String ID_LIST_SYSTEM_ITEMS = "listSystemItems";
    private static final String ID_TEST_REPOSITORY = "testRepository";
    private static final String ID_TEST_PROVISIONING = "testProvisioning";
    private static final String ID_IMPLEMENTATION_SHORT_NAME = "implementationShortName";
    private static final String ID_IMPLEMENTATION_DESCRIPTION = "implementationDescription";
    private static final String ID_IS_EMBEDDED = "isEmbedded";
    private static final String ID_DRIVER_SHORT_NAME = "driverShortName";
    private static final String ID_DRIVER_VERSION = "driverVersion";
    private static final String ID_REPOSITORY_URL = "repositoryUrl";
    private static final String ID_ADDITIONAL_DETAILS = "additionalDetails";
    private static final String ID_DETAIL_NAME = "detailName";
    private static final String ID_DETAIL_VALUE = "detailValue";

    private static final String[] PROPERTIES = new String[]{"file.separator", "java.class.path",
            "java.home", "java.vendor", "java.vendor.url", "java.version", "line.separator", "os.arch",
            "os.name", "os.version", "path.separator", "user.dir", "user.home", "user.name"};

    private IModel<RepositoryDiag> repoDiagModel;

    public PageSystem() {
        repoDiagModel = new LoadableModel<RepositoryDiag>(false) {

            @Override
            protected RepositoryDiag load() {
                return loadRepoDiagModel();
            }
        };
        initLayout();
    }

    private void initLayout() {
        Label revisionLabel = new Label(ID_REVISION, createStringResource("pageSystem.midPointRevision"));
        add(revisionLabel);

        ListView<SystemItem> listSystemItems = new ListView<SystemItem>(ID_LIST_SYSTEM_ITEMS, getItems()) {

            @Override
            protected void populateItem(ListItem<SystemItem> item) {
                item.add(new Label(ID_PROPERTY, item.getModelObject().getProperty()));
                item.add(new Label(ID_VALUE, item.getModelObject().getValue()));
            }
        };
        add(listSystemItems);

        add(new Label(ID_IMPLEMENTATION_SHORT_NAME, new PropertyModel<String>(repoDiagModel, "implementationShortName")));
        add(new Label(ID_IMPLEMENTATION_DESCRIPTION, new PropertyModel<String>(repoDiagModel, "implementationDescription")));
        add(new Label(ID_IS_EMBEDDED, new PropertyModel<String>(repoDiagModel, "isEmbedded")));
        add(new Label(ID_DRIVER_SHORT_NAME, new PropertyModel<String>(repoDiagModel, "driverShortName")));
        add(new Label(ID_DRIVER_VERSION, new PropertyModel<String>(repoDiagModel, "driverVersion")));
        add(new Label(ID_REPOSITORY_URL, new PropertyModel<String>(repoDiagModel, "repositoryUrl")));

        ListView<LabeledString> additionalDetails = new ListView<LabeledString>(ID_ADDITIONAL_DETAILS,
                new PropertyModel<List<? extends LabeledString>>(repoDiagModel, "additionalDetails")) {

            @Override
            protected void populateItem(ListItem<LabeledString> item) {
                item.add(new Label(ID_DETAIL_NAME, item.getModelObject().getLabel()));
                item.add(new Label(ID_DETAIL_VALUE, item.getModelObject().getData()));
            }
        };
        add(additionalDetails);

        initButtons();
    }

    private void initButtons() {
        AjaxLinkButton testRepository = new AjaxLinkButton(ID_TEST_REPOSITORY,
                createStringResource("pageSystem.button.testRepository")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                testRepositoryPerformed(target);
            }
        };
        add(testRepository);
        
        AjaxLinkButton testProvisioning = new AjaxLinkButton(ID_TEST_PROVISIONING,
                createStringResource("pageSystem.button.testProvisioning")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                testProvisioningPerformed(target);
            }
        };
        add(testProvisioning);
    }

    private RepositoryDiag loadRepoDiagModel() {
        OperationResult result = new OperationResult(OPERATION_GET_REPO_DIAG);
        RepositoryDiag diag = null;
        try {
            Task task = createSimpleTask(OPERATION_GET_REPO_DIAG);
            diag = getModelDiagnosticService().getRepositoryDiag(task, result);

            result.recordSuccessIfUnknown();
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't get repo diagnostics", ex);
            result.recordFatalError("Couldn't get repo diagnostics.", ex);
        }
        result.recomputeStatus();

        if (!WebMiscUtil.isSuccessOrHandledError(result)) {
            showResult(result);
        }

        return diag;
    }

    private IModel<List<SystemItem>> getItems() {
        return new LoadableModel<List<SystemItem>>(false) {

            @Override
            protected List<SystemItem> load() {
                List<SystemItem> items = new ArrayList<SystemItem>();
                for (String property : PROPERTIES) {
                    items.add(new SystemItem(property, System.getProperty(property)));
                }
                return items;
            }
        };
    }

    private void testRepositoryPerformed(AjaxRequestTarget target) {
        Task task = createSimpleTask(OPERATION_TEST_REPOSITORY);

        OperationResult result = getModelDiagnosticService().repositorySelfTest(task);
        showResult(result);

        target.add(getFeedbackPanel());
    }
    
    private void testProvisioningPerformed(AjaxRequestTarget target) {
        Task task = createSimpleTask(OPERATION_TEST_PROVISIONING);

        OperationResult result = getModelDiagnosticService().provisioningSelfTest(task);
        showResult(result);

        target.add(getFeedbackPanel());
    }

    private static class SystemItem implements Serializable {

        private String property;
        private String value;

        private SystemItem(String property, String value) {
            this.property = property;
            this.value = value;
        }

        public String getProperty() {
            return property;
        }

        public String getValue() {
            return value;
        }
    }
}
