/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.user;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.DetailsNavigationPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.factory.wrapper.PrismObjectWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.application.*;
import com.evolveum.midpoint.web.page.admin.users.component.UserSummaryPanel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectDetailsPageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/userNew")
        },
        encoder = OnePageParameterEncoder.class,
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_USERS_ALL_URL,
                        label = "PageAdminUsers.auth.usersAll.label",
                        description = "PageAdminUsers.auth.usersAll.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_USER_URL,
                        label = "PageUser.auth.user.label",
                        description = "PageUser.auth.user.description")
        })
public class PageUser extends PageBase {

    private static final Trace LOGGER = TraceManager.getTrace(PageUser.class);

    private static final String ID_MAIN_PANEL = "mainPanel";
    private static final String ID_NAVIGATION = "navigation";

    private static final String ID_SUMMARY = "summary";


    private static final String DOT_CLASS = PageUser.class.getName() + ".";
    private static final String OPERATION_LOAD_USER = DOT_CLASS + "loadUser";



    private LoadableModel<PrismObjectWrapper<UserType>> model;
    private GuiObjectDetailsPageType detailsPageConfiguration;

    public PageUser() {
        this(null);
    }

    public PageUser(PageParameters params) {
        super(params);

        model = createPageModel();
        detailsPageConfiguration = getCompiledGuiProfile().findObjectDetailsConfiguration(UserType.class);
        initLayout();
    }

    private LoadableModel<PrismObjectWrapper<UserType>> createPageModel() {
        return new LoadableModel<>(false) {
            @Override
            protected PrismObjectWrapper<UserType> load() {
                PrismObject<UserType> prismUser = loadPrismObject();

                PrismObjectWrapperFactory<UserType> factory = findObjectWrapperFactory(prismUser.getDefinition());
                Task task = createSimpleTask("createWrapper");
                OperationResult result = task.getResult();
                WrapperContext ctx = new WrapperContext(task, result);
                ctx.setCreateIfEmpty(true);

                try {
                    return factory.createObjectWrapper(prismUser, ItemStatus.ADDED, ctx);
                } catch (SchemaException e) {
                    //TODO:
                    return null;
                }
            }
        };
    }
    private PrismObject<UserType> loadPrismObject() {
        Task task = createSimpleTask(OPERATION_LOAD_USER);
        OperationResult result = task.getResult();
        PrismObject<UserType> prismUser;
            try {
                if (!isOidParameterExists()) {
                    UserType userType = new UserType(getPrismContext());
                    prismUser = userType.asPrismObject();
                } else {


                    String focusOid = getObjectOidParameter();
                    prismUser = WebModelServiceUtils.loadObject(UserType.class, focusOid, null, this, task, result);
                    LOGGER.trace("Loading object: Existing object (loadled): {} -> {}", focusOid, prismUser);
                }

                result.recordSuccess();
            } catch (Exception ex) {
                result.recordFatalError(getString("PageAdminObjectDetails.message.loadObjectWrapper.fatalError"), ex);
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load object", ex);
                prismUser = null;
            }

            showResult(result, false);
            return prismUser;

        }

    public boolean isOidParameterExists() {
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

    private void initLayout() {
        initSummaryPanel();
        initButtons();
        initMainPanel("basic", null);
        initNavigation();
    }

    private void initSummaryPanel() {
        LoadableModel<UserType> summaryModel = new LoadableModel<UserType>(false) {

            @Override
            protected UserType load() {
                PrismObjectWrapper<UserType> wrapper = model.getObject();
                if (wrapper == null) {
                    return null;
                }

                PrismObject<UserType> object = wrapper.getObject();
//                loadParentOrgs(object);
                return object.asObjectable();
            }
        };
        UserSummaryPanel summaryPanel = new UserSummaryPanel(ID_SUMMARY, summaryModel, PageUser.this);
        add(summaryPanel);

    }

    private void initButtons() {

    }


    private void initMainPanel(String identifier, ContainerPanelConfigurationType panelConfig) {
        //TODO load default panel?
        IModel<?> panelModel = getPanelModel(panelConfig);

        Panel panel = WebComponentUtil.createPanel(identifier, ID_MAIN_PANEL, model, panelConfig);
        addOrReplace(panel);

    }

    private IModel<? extends PrismContainerWrapper<? extends Containerable>> getPanelModel(ContainerPanelConfigurationType panelConfig) {
        if (panelConfig == null) {
            return model;
        }

        if (panelConfig.getPath() != null) {
            return PrismContainerWrapperModel.fromContainerWrapper(model, panelConfig.getPath().getItemPath());
        }

        return model;

    }

    private void initNavigation() {
        List<ContainerPanelConfigurationType> panels = getPanelsForUser();
        DetailsNavigationPanel navigationPanel = createNavigationPanel(ID_NAVIGATION, panels);
        add(navigationPanel);

    }

    private DetailsNavigationPanel createNavigationPanel(String id, List<ContainerPanelConfigurationType> panels) {

        DetailsNavigationPanel panel = new DetailsNavigationPanel(id, Model.ofList(panels)) {
            @Override
            protected void onClickPerformed(ContainerPanelConfigurationType config, AjaxRequestTarget target) {
                initMainPanel(config.getPanelIdentifier(), config);
                target.add(getMainPanel());
            }
        };
        return panel;
    }

    private List<ContainerPanelConfigurationType> getPanelsForUser() {
        List<ContainerPanelConfigurationType> defaultPanels = PanelLoader.getPanelsFor(UserType.class);
        List<ContainerPanelConfigurationType> configuredPanels = detailsPageConfiguration.getPanel();
        List<ContainerPanelConfigurationType> mergedPanels = mergeConfigurations(defaultPanels, configuredPanels);
        return mergedPanels;

    }

    private List<ContainerPanelConfigurationType> mergeConfigurations(List<ContainerPanelConfigurationType> defaultPanels, List<ContainerPanelConfigurationType> configuredPanels) {
        List<ContainerPanelConfigurationType> mergedPanels = new ArrayList<>(defaultPanels);
        for (ContainerPanelConfigurationType configuredPanel : configuredPanels) {
            mergePanelConfigurations(configuredPanel, defaultPanels, mergedPanels);
        }
        return mergedPanels;
    }

    private void mergePanelConfigurations(ContainerPanelConfigurationType configuredPanel, List<ContainerPanelConfigurationType> defaultPanels, List<ContainerPanelConfigurationType> mergedPanels) {
        for (ContainerPanelConfigurationType defaultPanel : defaultPanels) {
            if (defaultPanel.getIdentifier().equals(configuredPanel.getIdentifier())) {
                mergePanels(defaultPanel, configuredPanel);
                return;
            }
        }
        mergedPanels.add(configuredPanel.cloneWithoutId());
    }

    private void mergePanels(ContainerPanelConfigurationType mergedPanel, ContainerPanelConfigurationType configuredPanel) {
        if (configuredPanel.getPanelIdentifier() != null) {
            mergedPanel.setPanelIdentifier(configuredPanel.getPanelIdentifier());
        }

        if (configuredPanel.getPath() != null) {
            mergedPanel.setPath(configuredPanel.getPath());
        }

        if (configuredPanel.getListView() != null) {
            mergedPanel.setListView(configuredPanel.getListView().cloneWithoutId());
        }

        if (configuredPanel.getContainer() != null) {
            mergedPanel.setContainer(configuredPanel.getContainer().cloneWithoutId());
        }

        if (configuredPanel.getType() != null) {
            mergedPanel.setType(configuredPanel.getType());
        }

        if (!configuredPanel.getPanel().isEmpty()) {
            List<ContainerPanelConfigurationType> mergedConfigs = mergeConfigurations(mergedPanel.getPanel(), configuredPanel.getPanel());
            mergedPanel.getPanel().clear();
            mergedPanel.getPanel().addAll(mergedConfigs);
        }
    }

    private Component getMainPanel() {
        return get(ID_MAIN_PANEL);
    }

}
