/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin;

import java.time.Duration;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.server.OperationalButtonsPanel;

import com.evolveum.midpoint.web.page.admin.server.RefreshableTabPanel;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
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
    private static final String ID_BUTTONS = "buttons";

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
        initMainPanel(defaultConfiguration, form);
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
        OperationalButtonsPanel opButtonPanel = new OperationalButtonsPanel(ID_BUTTONS) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void addButtons(RepeatingView repeatingView) {
                initOperationalButtons(repeatingView);
            }

            @Override
            protected void addStateButtons(RepeatingView stateButtonsView) {
                initStateButtons(stateButtonsView);
            }
        };

        opButtonPanel.setOutputMarkupId(true);
//        opButtonPanel.add(new VisibleBehaviour(() -> isOperationalButtonsVisible() && opButtonPanel.buttonsExist()));

        AjaxSelfUpdatingTimerBehavior behavior = new AjaxSelfUpdatingTimerBehavior(Duration.ofMillis(getRefreshInterval())) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onPostProcessTarget(AjaxRequestTarget target) {
                refresh(target);
            }

            @Override
            protected boolean shouldTrigger() {
                return isRefreshEnabled();
            }
        };

        opButtonPanel.add(behavior);

        add(opButtonPanel);
    }

    protected void initOperationalButtons(RepeatingView repeatingView) {
        AjaxIconButton save = new AjaxIconButton(repeatingView.newChildId(), Model.of(GuiStyleConstants.CLASS_ICON_SAVE), Model.of("Save")) {

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {

            }
        };
        save.add(AttributeAppender.append("class", "btn btn-default btn-sm"));
        repeatingView.add(save);

        AjaxIconButton preview = new AjaxIconButton(repeatingView.newChildId(), Model.of(GuiStyleConstants.CLASS_ICON_PREVIEW), Model.of("Preview changes")) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {

            }
        };
        preview.add(AttributeAppender.append("class", "btn btn-default btn-sm"));
        repeatingView.add(preview);

        AjaxIconButton remove = new AjaxIconButton(repeatingView.newChildId(), Model.of(GuiStyleConstants.CLASS_ICON_REMOVE), Model.of("Delete object")) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {

            }
        };
        remove.add(AttributeAppender.append("class", "btn btn-default btn-sm"));
        repeatingView.add(remove);


//        AjaxButton changeArchetype = new AjaxButton(repeatingView.newChildId(), createStringResource("PageAdminObjectDetails.button.changeArchetype")) {
//            @Override
//            public void onClick(AjaxRequestTarget target) {
//                changeArchetypeButtonClicked(target);
//            }
//        };
//        changeArchetype.add(new VisibleBehaviour(() -> !getObjectWrapper().isReadOnly() && isChangeArchetypeAllowed() &&
//                getObjectArchetypeRef() != null && CollectionUtils.isNotEmpty(getArchetypeOidsListToAssign())));
//        changeArchetype.add(AttributeAppender.append("class", "btn-default"));
//        repeatingView.add(changeArchetype);
    }

    protected void initStateButtons(RepeatingView stateButtonsView) {

    }

    public int getRefreshInterval() {
        return 30;
    }

    public boolean isRefreshEnabled() {
        return false;
    }

    public void refresh(AjaxRequestTarget target) {
        refresh(target, true);
    }

    public void refresh(AjaxRequestTarget target, boolean soft) {

        if (isEditUser()) {
            model.reset();
        }
        target.add(getSummaryPanel());
        target.add(getOperationalButtonsPanel());
        target.add(getFeedbackPanel());
        refreshTitle(target);

//        if (soft) {
//            for (Component component : getMainPanel().getTabbedPanel()) {
//                if (component instanceof RefreshableTabPanel) {
//                    for (Component c : ((RefreshableTabPanel) component).getComponentsToUpdate()) {
//                        target.add(c);
//                    }
//                }
//            }
//        } else {
//            target.add(getMainPanel().getTabbedPanel());
//        }
    }

    private ContainerPanelConfigurationType findDefaultConfiguration() {
        //TODO support for second level panel as a default, e.g. assignment -> role
        ContainerPanelConfigurationType basicPanelConfig = getPanelConfigurations().stream().filter(panel -> BooleanUtils.isTrue(panel.isDefault())).findFirst().get();
        return basicPanelConfig;
    }

    private void initMainPanel(ContainerPanelConfigurationType panelConfig, MidpointForm form) {
        getSessionStorage().setObjectDetailsStorage("details" + getType().getSimpleName(), panelConfig);
        String panelType = panelConfig.getPanelType();
        if (panelType == null) {
            return;
        }
        Class<? extends Panel> panelClass = findObjectPanel(panelConfig.getPanelType());
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
                initMainPanel(config, form);
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
    private Component getSummaryPanel() {
        return get(ID_SUMMARY);
    }
    private Component getOperationalButtonsPanel() {
        return get(ID_BUTTONS);
    }

    public PrismObject<O> getPrismObject() {
        return model.getObject().getObject();
    }
}
