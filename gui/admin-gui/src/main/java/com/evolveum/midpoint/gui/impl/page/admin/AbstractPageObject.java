/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin;

import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;

import com.evolveum.midpoint.gui.api.factory.wrapper.PrismObjectWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectDetailsPageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public abstract class AbstractPageObject<O extends ObjectType> extends PageBase {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractPageObject.class);

    private static final String DOT_CLASS = AbstractPageObject.class.getName() + ".";
    private static final String OPERATION_LOAD_USER = DOT_CLASS + "loadUser";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_MAIN_PANEL = "mainPanel";
    private static final String ID_NAVIGATION = "navigation";
    private static final String ID_SUMMARY = "summary";


    private LoadableModel<PrismObjectWrapper<O>> model;
    private GuiObjectDetailsPageType detailsPageConfiguration;

    public AbstractPageObject(PageParameters pageParameters) {
        super(pageParameters);
        model = createPageModel();
        detailsPageConfiguration = getCompiledGuiProfile().findObjectDetailsConfiguration(getType());
        initLayout();
    }

    private void initLayout() {
        initSummaryPanel();
        initButtons();
        MidpointForm form = new MidpointForm(ID_MAIN_FORM);
        add(form);
        ContainerPanelConfigurationType defaultConfiguration = findDefaultConfiguration();
        initMainPanel("basic", defaultConfiguration, form);
        initNavigation();
    }

    private void initSummaryPanel() {
        LoadableModel<O> summaryModel = new LoadableModel<>(false) {

            @Override
            protected O load() {
                PrismObjectWrapper<O> wrapper = model.getObject();
                if (wrapper == null) {
                    return null;
                }

                PrismObject<O> object = wrapper.getObject();
//                loadParentOrgs(object);
                return object.asObjectable();
            }
        };
        Panel summaryPanel = getSummaryPanel(ID_SUMMARY, summaryModel);
        add(summaryPanel);

    }

    private void initButtons() {

    }

    private ContainerPanelConfigurationType findDefaultConfiguration() {
        //TODO possibility to configure default panel in configuration
        ContainerPanelConfigurationType basicPanelConfig = getPanelConfigurations().stream().filter(panel -> "basic".equals(panel.getIdentifier())).findFirst().get();
        return basicPanelConfig;
    }

    private void initMainPanel(String identifier, ContainerPanelConfigurationType panelConfig, MidpointForm form) {
        //TODO load default panel?
//        IModel<?> panelModel = getPanelModel(panelConfig);

        Class<? extends Panel> panelClass = findObjectPanel(identifier);
        Panel panel = WebComponentUtil.createPanel(panelClass, ID_MAIN_PANEL, model, panelConfig);
        form.addOrReplace(panel);

    }

    private void initNavigation() {
//        List<ContainerPanelConfigurationType> panels = getPanelsForUser();
        DetailsNavigationPanel navigationPanel = createNavigationPanel(ID_NAVIGATION, getPanelConfigurations());
        add(navigationPanel);

    }

    private DetailsNavigationPanel createNavigationPanel(String id, List<ContainerPanelConfigurationType> panels) {

        DetailsNavigationPanel panel = new DetailsNavigationPanel(id, model, Model.ofList(panels)) {
            @Override
            protected void onClickPerformed(ContainerPanelConfigurationType config, AjaxRequestTarget target) {
                MidpointForm form = getMainForm();
                initMainPanel(config.getPanelType(), config, form);
                target.add(form);
            }
        };
        return panel;
    }



    private LoadableModel<PrismObjectWrapper<O>> createPageModel() {
        return new LoadableModel<>(false) {
            @Override
            protected PrismObjectWrapper<O> load() {
                PrismObject<O> prismUser = loadPrismObject();

                PrismObjectWrapperFactory<O> factory = findObjectWrapperFactory(prismUser.getDefinition());
                Task task = createSimpleTask("createWrapper");
                OperationResult result = task.getResult();
                WrapperContext ctx = new WrapperContext(task, result);
                ctx.setCreateIfEmpty(true);
                ctx.setContainerPanelConfigurationType(getPanelConfigurations());

                try {
                    return factory.createObjectWrapper(prismUser, isEditUser()? ItemStatus.NOT_CHANGED : ItemStatus.ADDED, ctx);
                } catch (SchemaException e) {
                    //TODO:
                    return null;
                }
            }
        };
    }
    private PrismObject<O> loadPrismObject() {
        Task task = createSimpleTask(OPERATION_LOAD_USER);
        OperationResult result = task.getResult();
        PrismObject<O> prismObject;
        try {
            if (!isEditUser()) {
                prismObject = getPrismContext().createObject(getType());
            } else {
                String focusOid = getObjectOidParameter();
                prismObject = WebModelServiceUtils.loadObject(getType(), focusOid, getOperationOptions(), this, task, result);
                LOGGER.trace("Loading object: Existing object (loadled): {} -> {}", focusOid, prismObject);
            }
            result.recordSuccess();
        } catch (Exception ex) {
            result.recordFatalError(getString("PageAdminObjectDetails.message.loadObjectWrapper.fatalError"), ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load object", ex);
            prismObject = null;
        }

        showResult(result, false);
        return prismObject;
    }

    protected Collection<SelectorOptions<GetOperationOptions>> getOperationOptions() {
        return null;
    }

    public boolean isEditUser() {
        return getObjectOidParameter() != null;
    }

    protected String getObjectOidParameter() {
        PageParameters parameters = getPageParameters();
        LOGGER.trace("Page parameters: {}", parameters);
        StringValue oidValue = parameters.get(OnePageParameterEncoder.PARAMETER);
        LOGGER.trace("OID parameter: {}", oidValue);
        if (oidValue == null) {
            return null;
        }
        String oid = oidValue.toString();
        if (StringUtils.isBlank(oid)) {
            return null;
        }
        return oid;
    }

    public List<ContainerPanelConfigurationType> getPanelConfigurations() {
        return detailsPageConfiguration.getPanel();
    }

    protected abstract Class<O> getType();
    protected abstract Panel getSummaryPanel(String id, LoadableModel<O> summaryModel);

    private MidpointForm getMainForm() {
        return (MidpointForm) get(ID_MAIN_FORM);
    }

    public PrismObject<O> getPrismObject() {
        return model.getObject().getObject();
    }
}
