/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.reports.component;

import java.util.*;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.report.api.ReportConstants;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.message.FeedbackAlerts;
import com.evolveum.midpoint.web.page.admin.reports.dto.ReportDto;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class RunReportPopupPanel extends BasePanel<ReportDto> implements Popupable {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(RunReportPopupPanel.class);

    private static final String DOT_CLASS = RunReportPopupPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_RESOURCES = DOT_CLASS + "createResourceList";

    private static final String ID_MAIN_FORM = "mainForm";

    private static final String ID_RUN = "runReport";
    private static final String ID_POPUP_FEEDBACK = "popupFeedback";

    private static final Integer AUTO_COMPLETE_BOX_SIZE = 10;

    private static final String ID_PARAMETERS = "parameters";
    private static final String ID_PARAMETER = "parameter";

    private final ReportType reportType;
    private PrismContainer<ReportParameterType> paramContainer;

    public RunReportPopupPanel(String id, final ReportType reportType) {
        super(id);
        this.reportType = reportType;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initParameters();
        initLayout();
    }

    private void initParameters() {
        PrismContainerValue<ReportParameterType> reportParamValue;
        try {
            PrismContainerDefinition<ReportParameterType> paramContainerDef = getPrismContext().getSchemaRegistry()
                    .findContainerDefinitionByElementName(ReportConstants.REPORT_PARAMS_PROPERTY_NAME);
            paramContainer = paramContainerDef.instantiate();

            ReportParameterType reportParam = new ReportParameterType();
            reportParamValue = reportParam.asPrismContainerValue();
            reportParamValue.revive(getPrismContext());
            paramContainer.add(reportParamValue);
        } catch (SchemaException e) {
            LOGGER.error("Couldn't create container for report parameters");
            return;
        }
        for (SearchFilterParameterType parameter : reportType.getObjectCollection().getParameter()) {
            Class<?> clazz = getPrismContext().getSchemaRegistry().determineClassForType(parameter.getType());
            QName type = getPrismContext().getSchemaRegistry().determineTypeForClass(clazz);
            if (Containerable.class.isAssignableFrom(clazz)) {
                LOGGER.error("Couldn't create container item for parameter " + parameter);
                continue;
            }
            MutableItemDefinition def;
            if (Referencable.class.isAssignableFrom(clazz)) {
                def = getPrismContext().definitionFactory().createReferenceDefinition(
                        new QName(ReportConstants.NS_EXTENSION, parameter.getName()), type);
                ((MutablePrismReferenceDefinition) def).setTargetTypeName(parameter.getTargetType());
            } else {
                List values = WebComponentUtil.getAllowedValues(parameter, getPageBase());
                if (CollectionUtils.isNotEmpty(values)) {
                    def = (MutableItemDefinition) getPrismContext().definitionFactory().createPropertyDefinition(
                            new QName(ReportConstants.NS_EXTENSION, parameter.getName()), type, values, null);
                } else {
                    def = getPrismContext().definitionFactory().createPropertyDefinition(
                            new QName(ReportConstants.NS_EXTENSION, parameter.getName()), type);
                }
            }
            def.setDynamic(true);
            def.setRuntimeSchema(true);
            def.setMaxOccurs(1);
            def.setMinOccurs(0);
            if (parameter.getDisplay() != null) {
                String displayName = WebComponentUtil.getTranslatedPolyString(parameter.getDisplay().getLabel());
                def.setDisplayName(displayName);
                String help = WebComponentUtil.getTranslatedPolyString(parameter.getDisplay().getHelp());
                def.setHelp(help);
            }
            if (parameter.getAllowedValuesLookupTable() != null) {
                def.setValueEnumerationRef(parameter.getAllowedValuesLookupTable().asReferenceValue());
            }
            try {
                Item item = def.instantiate();
                if (item instanceof PrismReference){
                    ObjectReferenceType ref = new ObjectReferenceType();
                    ref.setType(parameter.getTargetType());
                    item.add(ref.asReferenceValue());
                }
                reportParamValue.add(item);

            } catch (SchemaException e) {
                LOGGER.error("Couldn't create item for parameter " + parameter);
            }
        }
    }

    protected void initLayout() {

        Form<?> mainForm = new MidpointForm<>(ID_MAIN_FORM);
        add(mainForm);

        IModel<List<Item>> listModel = (IModel) () -> {
            List<Item> list = new ArrayList<>();
            list.addAll(paramContainer.getValue().getItems());
            return list;
        };
        ListView<Item> paramListView = new ListView<Item>(ID_PARAMETERS, listModel) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<Item> paramItem) {
                try {
                    paramItem.add(createParameterPanel(paramItem.getModel()));
                } catch (SchemaException e) {
                    LOGGER.error("Couldn't create panel for item " + paramItem.getModelObject());
                }

            }
        };
        paramListView.setOutputMarkupId(true);
        mainForm.add(paramListView);

        FeedbackAlerts feedback = new FeedbackAlerts(ID_POPUP_FEEDBACK);
        feedback.setFilter(new ComponentFeedbackMessageFilter(paramListView) {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean accept(FeedbackMessage message) {
                return true;
            }
        });
        feedback.setOutputMarkupId(true);
        mainForm.add(feedback);

        AjaxSubmitButton addButton = new AjaxSubmitButton(ID_RUN,
                createStringResource("runReportPopupContent.button.run")) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onError(AjaxRequestTarget target) {
                FeedbackAlerts feedback = (FeedbackAlerts) getForm().get(ID_POPUP_FEEDBACK);
                target.add(feedback);
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                runConfirmPerformed(target);
            }
        };
        mainForm.add(addButton);

    }

    private WebMarkupContainer createParameterPanel(final IModel<Item> item) throws SchemaException {
        Task task = getPageBase().createSimpleTask("Create child containers");
        WrapperContext ctx = new WrapperContext(task, task.getResult());
        ItemWrapper wrapper = getPageBase().createItemWrapper(item.getObject(), ItemStatus.ADDED, ctx);
        return getPageBase().initItemPanel(ID_PARAMETER, item.getObject().getDefinition().getTypeName(),
                Model.of(wrapper), new ItemPanelSettingsBuilder().build());
    }

    public Task createSimpleTask(String operation, PrismObject<? extends FocusType> owner) {
        Task task = getPageBase().getTaskManager().createTaskInstance(operation);

        if (owner == null) {
            MidPointPrincipal user = SecurityUtils.getPrincipalUser();
            if (user == null) {
                return task;
            } else {
                owner = user.getFocus().asPrismObject();
            }
        }

        task.setOwner(owner);
        task.setChannel(SchemaConstants.CHANNEL_USER_URI);

        return task;
    }

    public Task createSimpleTask(String operation) {
        MidPointPrincipal user = SecurityUtils.getPrincipalUser();
        return createSimpleTask(operation, user != null ? user.getFocus().asPrismObject() : null);
    }

    private void runConfirmPerformed(AjaxRequestTarget target) {
        PrismContainer<ReportParameterType> parameterContainer = paramContainer.clone();
        List<Item> itemForRemoving = new ArrayList<>();
        parameterContainer.getValue().getItems().forEach((item) -> {
            if (item.isEmpty() || item.getRealValue() == null
                    || (item instanceof PrismReference && ((PrismReference) item).getRealValue() != null && ((PrismReference) item).getRealValue().getOid() == null)) {
                itemForRemoving.add(item);
            }
        });
        parameterContainer.getValue().getItems().removeAll(itemForRemoving);
        runConfirmPerformed(target, reportType, parameterContainer);
    }

    protected void runConfirmPerformed(AjaxRequestTarget target, ReportType reportType2,
            PrismContainer<ReportParameterType> reportParam) {
    }

    @Override
    public int getWidth() {
        return 1150;
    }

    @Override
    public int getHeight() {
        return 560;
    }

    @Override
    public String getWidthUnit() {
        return "px";
    }

    @Override
    public String getHeightUnit() {
        return "px";
    }

    @Override
    public StringResourceModel getTitle() {
        return createStringResource("RunReportPopupPanel.title");
    }

    @Override
    public Component getComponent() {
        return this;
    }
}
