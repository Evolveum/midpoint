/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.wizard.resource;

import java.util.*;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.model.NonEmptyLoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ConnectorTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.component.wizard.WizardStep;
import com.evolveum.midpoint.web.page.admin.resources.PageResourceWizard;
import com.evolveum.midpoint.web.page.admin.resources.component.TestConnectionResultPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * @author lazyman
 * @author mederly
 */
public class ConfigurationStep extends WizardStep {

    private static final Trace LOGGER = TraceManager.getTrace(ConfigurationStep.class);

    private static final String DOT_CLASS = ConfigurationStep.class.getName() + ".";
    private static final String TEST_CONNECTION = DOT_CLASS + "testConnection";
    private static final String OPERATION_SAVE = DOT_CLASS + "saveResource";
    private static final String OPERATION_CREATE_CONFIGURATION_WRAPPERS = "createConfigurationWrappers";

    private static final String ID_CONFIGURATION = "configuration";
    private static final String ID_TEST_CONNECTION = "testConnection";
    private static final String ID_MAIN = "main";

    final private NonEmptyLoadableModel<PrismObject<ResourceType>> resourceModelNoFetch;
//    final private NonEmptyLoadableModel<List<PrismContainerWrapper<?>>> configurationPropertiesModel;
    final private PageResourceWizard parentPage;
    final private LoadableModel<PrismContainerWrapper<ConnectorConfigurationType>> configurationModel;

    public ConfigurationStep(NonEmptyLoadableModel<PrismObject<ResourceType>> modelNoFetch, final PageResourceWizard parentPage) {
        super(parentPage);
        this.resourceModelNoFetch = modelNoFetch;
        this.parentPage = parentPage;

        this.configurationModel = new LoadableModel<PrismContainerWrapper<ConnectorConfigurationType>>(false) {
            @Override
            protected PrismContainerWrapper<ConnectorConfigurationType> load() {
                try {
                    return createConfigContainerWrappers();
                } catch (Exception e) {
                    getSession().error("Cannot load conector configuration, " + e.getMessage());
                    throw new RestartResponseException(PageResourceWizard.class);
                }
            }
        };

        initLayout();
    }

    public void resetConfiguration() {
        configurationModel.reset();
    }

    //@NotNull
    private PrismContainerWrapper<ConnectorConfigurationType> createConfigContainerWrappers() throws SchemaException {
        PrismObject<ResourceType> resource = resourceModelNoFetch.getObject();
        PrismContainer<ConnectorConfigurationType> configuration = resource.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);

        List<PrismContainerWrapper<?>> containerWrappers = new ArrayList<>();

        if(parentPage.isNewResource()) {
            return null;
        }
        ItemStatus configurationStatus = ItemStatus.NOT_CHANGED;
        if (configuration == null) {
            PrismObject<ConnectorType> connector = ResourceTypeUtil.getConnectorIfPresent(resource);
            if (connector == null) {
                throw new IllegalStateException("No resolved connector object in resource object");
            }
            ConnectorType connectorType = connector.asObjectable();
            PrismSchema schema;
            try {
                schema = ConnectorTypeUtil.parseConnectorSchema(connectorType, parentPage.getPrismContext());
            } catch (SchemaException e) {
                throw new SystemException("Couldn't parse connector schema: " + e.getMessage(), e);
            }
            PrismContainerDefinition<ConnectorConfigurationType> definition = ConnectorTypeUtil.findConfigurationContainerDefinition(connectorType, schema);
            // Fixing (errorneously) set maxOccurs = unbounded. See MID-2317 and related issues.
            PrismContainerDefinition<ConnectorConfigurationType> definitionFixed = definition.clone();
            definitionFixed.toMutable().setMaxOccurs(1);
            configuration = definitionFixed.instantiate();
            configurationStatus = ItemStatus.ADDED;
        }

        Task task = getPageBase().createSimpleTask(OPERATION_CREATE_CONFIGURATION_WRAPPERS);
        WrapperContext ctx = new WrapperContext(task, getResult());
        ctx.setReadOnly(parentPage.isReadOnly());
        ctx.setShowEmpty(ItemStatus.ADDED == configurationStatus);
        PrismContainerWrapper<ConnectorConfigurationType> configurationWrapper = getPageBase().createItemWrapper(configuration, configurationStatus, ctx);

        return configurationWrapper;
    }

    private void initLayout() {
        com.evolveum.midpoint.web.component.form.Form form = new com.evolveum.midpoint.web.component.form.Form<>(ID_MAIN, true);
        form.setOutputMarkupId(true);
        add(form);

        form.add(WebComponentUtil.createTabPanel(ID_CONFIGURATION, parentPage, createConfigurationTabs(), null));

        AjaxSubmitButton testConnection = new AjaxSubmitButton(ID_TEST_CONNECTION,
                createStringResource("ConfigurationStep.button.testConnection")) {

            @Override
            protected void onError(final AjaxRequestTarget target) {
                WebComponentUtil.refreshFeedbacks(form, target);
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                testConnectionPerformed(target);
            }
        };
        testConnection.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return !parentPage.isReadOnly();
            }
        });
        add(testConnection);
    }

    private List<ITab> createConfigurationTabs() {
        final com.evolveum.midpoint.web.component.form.Form form = getForm();
        List<ITab> tabs = new ArrayList<>();
        PrismContainerWrapper<ConnectorConfigurationType> configuration = configurationModel.getObject();
        if (configuration == null) {
            return new ArrayList<>();
        }
        PrismContainerValueWrapper<ConnectorConfigurationType> configurationValue = null;
        try {
            configurationValue = configuration.getValue();
        } catch (SchemaException e) {
            LOGGER.error("Cannot get value for conenctor configuration, {}", e.getMessage(), e);
            getSession().error("A problem occurred while getting value for connector configuration, " + e.getMessage());
            return null;
        }
        for (final PrismContainerWrapper<?> wrapper : configurationValue.getContainers()) {
            String tabName = wrapper.getDisplayName();
            tabs.add(new AbstractTab(new Model<>(tabName)) {
                private static final long serialVersionUID = 1L;

                @Override
                public WebMarkupContainer getPanel(String panelId) {
                    try {
                        return getPageBase().initItemPanel(panelId, wrapper.getTypeName(), Model.of(wrapper), null);
                    } catch (SchemaException e) {
                        LOGGER.error("Cannot create panel for {}, reason: {}", wrapper.getTypeName(), e.getMessage(), e);
                        getSession().error("Cannot create panel for " + wrapper.getTypeName() + ", reason: " + e.getMessage());
                        return null;
                    }
                }
            });
        }

        return tabs;
    }

    public void updateConfigurationTabs() {

        TabbedPanel<ITab> tabbedPanel = getConfigurationTabbedPanel();
        List<ITab> tabs = tabbedPanel.getTabs().getObject();
        tabs.clear();

        tabs.addAll(createConfigurationTabs());
        if (tabs.size() == 0){
            return;
        }
        int i = tabbedPanel.getSelectedTab();
        if (i < 0 || i > tabs.size()) {
            i = 0;
        }
        tabbedPanel.setSelectedTab(i);
    }

    @SuppressWarnings("unchecked")
    private com.evolveum.midpoint.web.component.form.Form getForm() {
        return (com.evolveum.midpoint.web.component.form.Form) get(ID_MAIN);
    }

    @SuppressWarnings("unchecked")
    private TabbedPanel<ITab> getConfigurationTabbedPanel() {
        return (TabbedPanel<ITab>) get(createComponentPath(ID_MAIN, ID_CONFIGURATION));
    }

    // copied from PageResource, TODO deduplicate
    private void testConnectionPerformed(AjaxRequestTarget target) {
        parentPage.refreshIssues(null);
        if (parentPage.isReadOnly() || !isComplete()) {
            return;
        }
        saveChanges();

        PageBase page = getPageBase();
        TestConnectionResultPanel testConnectionPanel = new TestConnectionResultPanel(page.getMainPopupBodyId(), resourceModelNoFetch.getObject().getOid(), getPage());
        testConnectionPanel.setOutputMarkupId(true);
        page.showMainPopup(testConnectionPanel, target);
//        page.showResult(result, "Test connection failed", false);
        target.add(page.getFeedbackPanel());
        target.add(getForm());
    }

    @Override
    public void applyState() {
        parentPage.refreshIssues(null);
        if (parentPage.isReadOnly() || !isComplete()) {
            return;
        }
        saveChanges();
    }

    private void saveChanges() {
        Task task = parentPage.createSimpleTask(OPERATION_SAVE);
        OperationResult result = task.getResult();
        boolean saved = false;
        try {
            PrismContainerWrapper<ConnectorConfigurationType> configuration = configurationModel.getObject();
            Collection<ItemDelta> modifications = configuration.getDelta();

            if (modifications == null) {
                //TODO should we show some info to user?
                return;
            }

            ObjectDelta delta = parentPage.getPrismContext()
                    .deltaFactory()
                    .object()
                    .createEmptyModifyDelta(ResourceType.class, parentPage.getEditedResourceOid());

            if (ItemStatus.ADDED == configuration.getStatus()) {
                ContainerDelta connectorConfigDelta = delta.createContainerModification(ResourceType.F_CONNECTOR_CONFIGURATION);
                for (ItemDelta mod : modifications) {
                    connectorConfigDelta.merge(mod);
                }
            } else {
                delta.mergeModifications(modifications);
            }

            parentPage.getPrismContext().adopt(delta);
            if (!delta.isEmpty()) {
                parentPage.logDelta(delta);
                WebModelServiceUtils.save(delta, result, parentPage);
                parentPage.resetModels();
                saved = true;
            }
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Error occurred during saving changes", ex);
            result.recordFatalError(getString("ConfigurationStep.message.saveConfiguration.fatalError"), ex);
        } finally {
            result.computeStatusIfUnknown();
            setResult(result);
        }

        if (parentPage.showSaveResultInPage(saved, result)) {
            parentPage.showResult(result);
        }

    }
}
