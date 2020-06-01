/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.reports.component;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.session.AuditLogStorage;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.yui.calendar.DateTimeField;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.button.CsvDownloadButtonPanel;
import com.evolveum.midpoint.gui.api.component.path.ItemPathPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil.Channel;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.form.multivalue.MultiValueChoosePanel;
import com.evolveum.midpoint.web.component.input.DatePanel;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.web.page.admin.reports.PageAuditLogDetails;
import com.evolveum.midpoint.web.page.admin.reports.dto.AuditEventRecordProvider;
import com.evolveum.midpoint.web.page.admin.reports.dto.AuditSearchDto;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.DateValidator;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventStageType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventTypeType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Created by honchar.
 */
public abstract class AuditLogViewerPanel extends BasePanel<AuditSearchDto> {
    private static final long serialVersionUID = 1L;

    private static final String ID_PARAMETERS_PANEL = "parametersPanel";
    private static final String ID_TABLE = "table";
    private static final String ID_FROM = "fromField";
    private static final String ID_FROM_FIELD_HELP = "fromFieldHelp";
    private static final String ID_TO_FIELD_HELP = "toFieldHelp";
    private static final String ID_TARGET_NAME_FIELD_HELP = "targetNameFieldHelp";
    private static final String ID_TARGET_OWNER_NAME_FIELD_HELP = "targetOwnerNameFieldHelp";
    private static final String ID_INITIATOR_NAME_FIELD_HELP = "initiatorNameFieldHelp";
    private static final String ID_CHANGED_ITEM_FIELD_HELP = "changedItemFieldHelp";
    private static final String ID_EVENT_TYPE_FIELD_HELP = "eventTypeFieldHelp";
    private static final String ID_EVENT_STAGE_FIELD_HELP = "eventStageFieldHelp";
    private static final String ID_OUTCOME_FIELD_HELP = "outcomeFieldHelp";
    private static final String ID_CHANNEL_FIELD_HELP = "channelFieldHelp";
    private static final String ID_HOST_ID_FIELD_HELP = "hostIdentifierFieldHelp";
    private static final String ID_REQUEST_ID_FIELD_HELP = "requestIdentifierFieldHelp";
    private static final String ID_VALUE_REF_TARGET_NAMES_FIELD_HELP = "valueRefTargetNamesFieldHelp";
    private static final String ID_USED_QUERY_FIELD_HELP = "usedQueryFieldHelp";
    private static final String ID_USED_INTERVAL_FIELD_HELP = "usedIntervalFieldHelp";
    private static final String ID_RESOURCE_OID_FIELD = "resourceOidField";
    private static final String ID_RESOURCE_OID_FIELD_HELP = "resourceOidFieldHelp";
    private static final String ID_TO = "toField";
    private static final String ID_INITIATOR_NAME = "initiatorNameField";
    private static final String ID_TARGET_NAME_FIELD = "targetNameField";
    private static final String ID_TARGET_NAME = "targetName";
    private static final String ID_TARGET_OWNER_NAME = "targetOwnerName";
    private static final String ID_TARGET_OWNER_NAME_FIELD = "targetOwnerNameField";
    private static final String ID_CHANNEL = "channelField";
    private static final String ID_HOST_IDENTIFIER = "hostIdentifierField";
    private static final String ID_EVENT_TYPE = "eventTypeField";
    private static final String ID_EVENT_STAGE_FIELD = "eventStageField";
    private static final String ID_EVENT_STAGE = "eventStage";
    private static final String ID_OUTCOME = "outcomeField";
    private static final String ID_CHANGED_ITEM = "changedItem";
    private static final String ID_VALUE_REF_TARGET_NAMES_FIELD = "valueRefTargetNamesField";
    private static final String ID_VALUE_REF_TARGET_NAMES = "valueRefTargetNames";
    private static final String ID_USED_QUERY = "usedQueryField";
    private static final String ID_USED_QUERY_CONTAINER = "usedQueryContainer";
    private static final String ID_USED_INTERVAL = "usedIntervalField";
    private static final String ID_USED_INTERVAL_CONTAINER = "usedIntervalContainer";
    private static final String ID_REQUEST_IDENTIFIER_FIELD = "requestIdentifierField";
    private static final String ID_RESOURCE_OID_CONTAINER = "resourceOidContainer";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_SEARCH_BUTTON = "searchButton";
    private static final String ID_RESET_SEARCH_BUTTON = "resetSearchButton";

    static final Trace LOGGER = TraceManager.getTrace(AuditLogViewerPanel.class);


    private static final String OPERATION_RESOLVE_REFENRENCE_NAME = AuditLogViewerPanel.class.getSimpleName()
            + ".resolveReferenceName()";
    private static final String OPERATION_LOAD_AUDIT_CONFIGURATION = AuditLogViewerPanel.class.getSimpleName()
            + ".isResourceOidAuditEnabled()";

    private static final int DEFAULT_PAGE_SIZE = 10;

    private boolean isHistory = false;

    public <F extends ObjectType> AuditLogViewerPanel(String id, IModel<AuditSearchDto> model, boolean isHistory) {
        super(id, model);

        this.isHistory = isHistory;

    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {

        Form mainForm = new Form(ID_MAIN_FORM);
        mainForm.setOutputMarkupId(true);
        add(mainForm);

        initParametersPanel(mainForm);
        initAuditLogViewerTable(mainForm);
    }

    private void initParametersPanel(Form mainForm) {
        WebMarkupContainer parametersPanel = new WebMarkupContainer(ID_PARAMETERS_PANEL);
        parametersPanel.setOutputMarkupId(true);
        mainForm.add(parametersPanel);

        DatePanel from = new DatePanel(ID_FROM, new PropertyModel<>(
            getModel(), AuditSearchDto.F_FROM));
        DateValidator dateFromValidator = WebComponentUtil.getRangeValidator(mainForm, ItemPath.create(AuditSearchDto.F_FROM));
        dateFromValidator.setMessageKey("AuditLogViewerPanel.dateValidatorMessage");
        dateFromValidator.setDateFrom((DateTimeField) from.getBaseFormComponent());
        for (FormComponent<?> formComponent : from.getFormComponents()) {
            formComponent.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        }

        from.setOutputMarkupId(true);
        parametersPanel.add(from);
        parametersPanel.add(getHelpComponent(ID_FROM_FIELD_HELP, WebPrismUtil.getHelpText(getItemDefinition(AuditEventRecordType.F_TIMESTAMP))));

        DatePanel to = new DatePanel(ID_TO, new PropertyModel<>(getModel(),
            AuditSearchDto.F_TO));
        DateValidator dateToValidator = WebComponentUtil.getRangeValidator(mainForm,
                ItemPath.create(AuditSearchDto.F_FROM));
        dateToValidator.setMessageKey("AuditLogViewerPanel.dateValidatorMessage");
        dateToValidator.setDateTo((DateTimeField) to.getBaseFormComponent());
        for (FormComponent<?> formComponent : to.getFormComponents()) {
            formComponent.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        }

        to.setOutputMarkupId(true);
        parametersPanel.add(to);
        parametersPanel.add(getHelpComponent(ID_TO_FIELD_HELP, WebPrismUtil.getHelpText(getItemDefinition(AuditEventRecordType.F_TIMESTAMP))));

        WebMarkupContainer resourceOidContainer = new WebMarkupContainer(ID_RESOURCE_OID_CONTAINER);
        resourceOidContainer.setOutputMarkupId(true);
        resourceOidContainer.add(new VisibleBehaviour(() -> isResourceOidAuditEnabled()));
        parametersPanel.add(resourceOidContainer);

        TextPanel<String> resourceOidFiels = new TextPanel<>(ID_RESOURCE_OID_FIELD, new PropertyModel<>(getModel(),
                AuditSearchDto.F_RESOURCE_OID));
        resourceOidFiels.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        resourceOidFiels.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        resourceOidFiels.setOutputMarkupId(true);
        resourceOidContainer.add(resourceOidFiels);
        resourceOidContainer.add(getHelpComponent(ID_RESOURCE_OID_FIELD_HELP, ""));

        ItemPathPanel changedItemPanel = new ItemPathPanel(ID_CHANGED_ITEM, new PropertyModel<>(getModel(),
            AuditSearchDto.F_CHANGED_ITEM), true, getAuditLogStorage() != null ?
                getAuditLogStorage().getSearchDto().getChangedItemPanelMode() : ItemPathPanel.ItemPathPanelMode.NAMESPACE_MODE){
            private static final long serialVersionUID = 1L;

            @Override
            protected void switchButtonClickPerformed(AjaxRequestTarget target){
                super.switchButtonClickPerformed(target);
                if (getAuditLogStorage() != null){
                    getAuditLogStorage().getSearchDto().setChangedItemPanelMode(getPanelMode());
                }
            }
        };
        changedItemPanel.setOutputMarkupId(true);
        parametersPanel.add(changedItemPanel);
        parametersPanel.add(getHelpComponent(ID_CHANGED_ITEM_FIELD_HELP, ""));

        TextPanel<String> hostIdentifier = new TextPanel<>(ID_HOST_IDENTIFIER, new PropertyModel<>(getModel(),
                AuditSearchDto.F_HOST_IDENTIFIER));
        hostIdentifier.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        hostIdentifier.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        hostIdentifier.setOutputMarkupId(true);
        parametersPanel.add(hostIdentifier);
        parametersPanel.add(getHelpComponent(ID_HOST_ID_FIELD_HELP,
                WebPrismUtil.getHelpText(getItemDefinition(AuditEventRecordType.F_HOST_IDENTIFIER))));

        TextPanel<String> requestIdentifier = new TextPanel<>(ID_REQUEST_IDENTIFIER_FIELD, new PropertyModel<>(getModel(),
                AuditSearchDto.F_REQUEST_IDENTIFIER));
        requestIdentifier.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        requestIdentifier.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        requestIdentifier.setOutputMarkupId(true);
        parametersPanel.add(requestIdentifier);
        parametersPanel.add(getHelpComponent(ID_REQUEST_ID_FIELD_HELP,
                WebPrismUtil.getHelpText(getItemDefinition(AuditEventRecordType.F_REQUEST_IDENTIFIER))));

        WebMarkupContainer usedQueryContainer = new WebMarkupContainer(ID_USED_QUERY_CONTAINER);
        usedQueryContainer.add(getVisibleBehaviourForUsedQueryComponent());
        usedQueryContainer.setOutputMarkupId(true);
        parametersPanel.add(usedQueryContainer);

        TextPanel<String> usedQuery = new TextPanel<>(ID_USED_QUERY, new PropertyModel<>(getModel(),
                AuditSearchDto.F_COLLECTION + ".auditSearch.recordQuery"));
        usedQuery.setOutputMarkupId(true);
        usedQuery.setEnabled(false);
        usedQueryContainer.add(usedQuery);
        usedQueryContainer.add(getHelpComponent(ID_USED_QUERY_FIELD_HELP, ""));

        WebMarkupContainer usedIntervalContainer = new WebMarkupContainer(ID_USED_INTERVAL_CONTAINER);
        usedIntervalContainer.setOutputMarkupId(true);
        usedIntervalContainer.add(getVisibleBehaviourForUsedQueryComponent());
        parametersPanel.add(usedIntervalContainer);

        TextPanel<String> usedInterval = new TextPanel<>(ID_USED_INTERVAL, new PropertyModel<>(getModel(),
                AuditSearchDto.F_COLLECTION + ".auditSearch.interval"));
        usedInterval.setOutputMarkupId(true);
        usedInterval.setEnabled(false);
        usedIntervalContainer.add(usedInterval);
        usedIntervalContainer.add(getHelpComponent(ID_USED_INTERVAL_FIELD_HELP, ""));

        DropDownChoicePanel<AuditEventTypeType> eventType = new DropDownChoicePanel<>(
            ID_EVENT_TYPE, new PropertyModel<>(
            getModel(), AuditSearchDto.F_EVENT_TYPE), new ListModel<>(
            Arrays.asList(AuditEventTypeType.values())),
            new EnumChoiceRenderer<>(), true);
        eventType.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        eventType.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        eventType.setOutputMarkupId(true);
        parametersPanel.add(eventType);
        parametersPanel.add(getHelpComponent(ID_EVENT_TYPE_FIELD_HELP,
                WebPrismUtil.getHelpText(getItemDefinition(AuditEventRecordType.F_EVENT_TYPE))));

        WebMarkupContainer eventStage = new WebMarkupContainer(ID_EVENT_STAGE);
        eventStage.setOutputMarkupId(true);
        eventStage.add(new VisibleEnableBehaviour() {

                private static final long serialVersionUID = 1L;

                @Override
                public boolean isVisible() {
                    return !isHistory;
                }
        });

        parametersPanel.add(eventStage);

        ListModel<AuditEventStageType> eventStageListModel = new ListModel<>(
            Arrays.asList(AuditEventStageType.values()));
        PropertyModel<AuditEventStageType> eventStageModel = new PropertyModel<>(
            getModel(), AuditSearchDto.F_EVENT_STAGE);
        DropDownChoicePanel<AuditEventStageType> eventStageField = new DropDownChoicePanel<>(
            ID_EVENT_STAGE_FIELD, eventStageModel, eventStageListModel,
            new EnumChoiceRenderer<>(), true);
        eventStageField.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        eventStageField.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        eventStageField.setOutputMarkupId(true);
        eventStage.add(eventStageField);
        eventStage.add(getHelpComponent(ID_EVENT_STAGE_FIELD_HELP,
                WebPrismUtil.getHelpText(getItemDefinition(AuditEventRecordType.F_EVENT_STAGE))));

        ListModel<OperationResultStatusType> outcomeListModel = new ListModel<>(Arrays.asList(OperationResultStatusType.values()));
        PropertyModel<OperationResultStatusType> outcomeModel = new PropertyModel<>(getModel(), AuditSearchDto.F_OUTCOME);
        DropDownChoicePanel<OperationResultStatusType> outcome = new DropDownChoicePanel<>(ID_OUTCOME, outcomeModel,
                outcomeListModel, new EnumChoiceRenderer<>(), true);
        outcome.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        outcome.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        outcome.setOutputMarkupId(true);
        parametersPanel.add(outcome);
        parametersPanel.add(getHelpComponent(ID_OUTCOME_FIELD_HELP,
                WebPrismUtil.getHelpText(getItemDefinition(AuditEventRecordType.F_OUTCOME))));

        List<String> channelList = WebComponentUtil.getChannelList();
        List<QName> channelQnameList = new ArrayList<>();
        for (int i = 0; i < channelList.size(); i++) {
            String channel = channelList.get(i);
            if (channel != null) {
                QName channelQName = QNameUtil.uriToQName(channel);
                channelQnameList.add(channelQName);
            }
        }
//        ListModel<QName> channelListModel = new ListModel<>(channelQnameList);
        PropertyModel<Channel> channelModel = new PropertyModel<>(getModel(),
            AuditSearchDto.F_CHANNEL);
        DropDownChoicePanel<Channel> channel = new DropDownChoicePanel<>(ID_CHANNEL, channelModel,
            Model.ofList(Arrays.asList(Channel.values())),
                new EnumChoiceRenderer<>(), true);
        channel.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        channel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        channel.setOutputMarkupId(true);
        parametersPanel.add(channel);
        parametersPanel.add(getHelpComponent(ID_CHANNEL_FIELD_HELP,
                WebPrismUtil.getHelpText(getItemDefinition(AuditEventRecordType.F_CHANNEL))));

        List<Class<? extends ObjectType>> allowedClasses = new ArrayList<>();
        allowedClasses.add(UserType.class);
        MultiValueChoosePanel<ObjectType> chooseInitiatorPanel = new SingleValueChoosePanel<>(
                ID_INITIATOR_NAME, allowedClasses, objectReferenceTransformer,
                new PropertyModel<>(getModel(), AuditSearchDto.F_INITIATOR_NAME));
        parametersPanel.add(chooseInitiatorPanel);
        parametersPanel.add(getHelpComponent(ID_INITIATOR_NAME_FIELD_HELP,
                WebPrismUtil.getHelpText(getItemDefinition(AuditEventRecordType.F_INITIATOR_REF))));

        WebMarkupContainer targetOwnerName = new WebMarkupContainer(ID_TARGET_OWNER_NAME);
        targetOwnerName.add(new VisibleEnableBehaviour() {

                private static final long serialVersionUID = 1L;

                @Override
                public boolean isVisible() {
                    return !isHistory;
                }
        });
        parametersPanel.add(targetOwnerName);

        MultiValueChoosePanel<ObjectType> chooseTargetOwnerPanel = new SingleValueChoosePanel<>(
                ID_TARGET_OWNER_NAME_FIELD, allowedClasses, objectReferenceTransformer,
                new PropertyModel<>(getModel(), AuditSearchDto.F_TARGET_OWNER_NAME));
        targetOwnerName.add(chooseTargetOwnerPanel);
        targetOwnerName.add(getHelpComponent(ID_TARGET_OWNER_NAME_FIELD_HELP,
                WebPrismUtil.getHelpText(getItemDefinition(AuditEventRecordType.F_TARGET_OWNER_REF))));

        WebMarkupContainer targetName = new WebMarkupContainer(ID_TARGET_NAME);
        targetName.add(new VisibleEnableBehaviour() {

            private static final long serialVersionUID = 1L;

                @Override
                public boolean isVisible() {
                    return !isHistory;
                }
        });

        parametersPanel.add(targetName);
        List<Class<? extends ObjectType>> allowedClassesAll = new ArrayList<>();
        allowedClassesAll.addAll(ObjectTypes.getAllObjectTypes());

        MultiValueChoosePanel<ObjectType> chooseTargetPanel = new MultiValueChoosePanel<>(
            ID_TARGET_NAME_FIELD,
            new PropertyModel<>(getModel(), AuditSearchDto.F_TARGET_NAMES_OBJECTS),
            allowedClassesAll);
        chooseTargetPanel.setOutputMarkupId(true);
        targetName.add(chooseTargetPanel);
        targetName.add(getHelpComponent(ID_TARGET_NAME_FIELD_HELP,
                WebPrismUtil.getHelpText(getItemDefinition(AuditEventRecordType.F_TARGET_REF))));

        AjaxSubmitButton searchButton = new AjaxSubmitButton(ID_SEARCH_BUTTON,
                createStringResource("BasicSearchPanel.search")) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                updateAuditSearchStorage(getModel().getObject());
                searchUpdatePerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target){
                target.add(getPageBase().getFeedbackPanel());
            }
        };
        searchButton.setOutputMarkupId(true);
        parametersPanel.add(searchButton);

        AjaxSubmitButton resetSearchButton = new AjaxSubmitButton(ID_RESET_SEARCH_BUTTON,
                createStringResource("AuditLogViewerPanel.resetSearchButton")) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                    resetAuditSearchStorage();
                searchUpdatePerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target){
                target.add(getPageBase().getFeedbackPanel());
            }
        };
        resetSearchButton.setOutputMarkupId(true);
        parametersPanel.add(resetSearchButton);

        WebMarkupContainer valueRefTargetNameContainer = new WebMarkupContainer(ID_VALUE_REF_TARGET_NAMES);
        valueRefTargetNameContainer.add(new VisibleEnableBehaviour() {

                @Override
                public boolean isVisible() {
                    return !isHistory;
                }

        });
        parametersPanel.add(valueRefTargetNameContainer);

        MultiValueChoosePanel<ObjectType> chooseValueRefTargetNamePanel = new MultiValueChoosePanel<>(
            ID_VALUE_REF_TARGET_NAMES_FIELD,
            new PropertyModel<>(getModel(), AuditSearchDto.F_VALUE_REF_TARGET_NAME),
            allowedClassesAll);
        chooseValueRefTargetNamePanel.setOutputMarkupId(true);
        valueRefTargetNameContainer.add(chooseValueRefTargetNamePanel);
        valueRefTargetNameContainer.add(getHelpComponent(ID_VALUE_REF_TARGET_NAMES_FIELD_HELP, ""));

    }

    private VisibleEnableBehaviour getVisibleBehaviourForUsedQueryComponent() {
        return new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                IModel<AuditSearchDto> model = getModel();
                if(model == null || model.getObject() == null
                        || model.getObject().getCollection() == null
                        || model.getObject().getCollection().getAuditSearch() == null
                        || model.getObject().getCollection().getAuditSearch().getRecordQuery() == null) {
                    return false;
                }
                return true;
            }
        };
    }

    // Serializable as it becomes part of panel which is serialized
    private Function<ObjectType, ObjectReferenceType> objectReferenceTransformer =
            (Function<ObjectType, ObjectReferenceType> & Serializable) (ObjectType o) ->
                ObjectTypeUtil.createObjectRef(o, getPageBase().getPrismContext());

    // Serializable as it becomes part of panel which is serialized
    private Function<ObjectType, String> stringTransformer =
            (Function<ObjectType, String> & Serializable) (ObjectType o) ->
                o.getName().getOrig();


    private IModel<ObjectCollectionType> getCollectionFroAuditEventModel() {
        return new IModel<ObjectCollectionType>() {
            private static final long serialVersionUID = 1L;

            @Override
            public ObjectCollectionType getObject() {
                AuditSearchDto search = AuditLogViewerPanel.this.getModelObject();
                ObjectCollectionType collection = search.getCollection();
                return collection;
            }

        };
    }

    private Map<String, Object> getAuditEventProviderParameters() {
        Map<String, Object> parameters = new HashMap<>();

        AuditSearchDto search = AuditLogViewerPanel.this.getModelObject();
        parameters.put(AuditEventRecordProvider.PARAMETER_FROM, search.getFrom());
        parameters.put(AuditEventRecordProvider.PARAMETER_TO, search.getTo());

        if (search.getChannel() != null) {
            Channel channel = search.getChannel();
            if (channel.equals(Channel.IMPORT)) {
                parameters.put(AuditEventRecordProvider.PARAMETER_CHANNEL, SchemaConstants.CHANGE_CHANNEL_IMPORT_URI);
            } else {
                parameters.put(AuditEventRecordProvider.PARAMETER_CHANNEL, channel.getChannel());
            }
        }
        parameters.put(AuditEventRecordProvider.PARAMETER_HOST_IDENTIFIER, search.getHostIdentifier());
        parameters.put(AuditEventRecordProvider.PARAMETER_REQUEST_IDENTIFIER, search.getRequestIdentifier());

        if (StringUtils.isNotEmpty(search.getResourceOid())){
            parameters.put(AuditEventRecordProvider.PARAMETER_RESOURCE_OID, search.getResourceOid());
        }
        if (search.getInitiatorName() != null) {
            parameters.put(AuditEventRecordProvider.PARAMETER_INITIATOR_NAME, search.getInitiatorName().getOid());
        }

        if (search.getTargetOwnerName() != null) {
            parameters.put(AuditEventRecordProvider.PARAMETER_TARGET_OWNER_NAME, search.getTargetOwnerName().getOid());
        }
        List<String> targetOids = new ArrayList<>();
        if (isNotEmpty(search.getTargetNamesObjects())) {
            targetOids.addAll(search.getTargetNamesObjects().stream()
                    .map(ObjectType::getOid)
                    .collect(toList()));
        }
        if (isNotEmpty(search.getTargetNames())) {
            targetOids.addAll(search.getTargetNames().stream()
                    .map(ObjectReferenceType::getOid)
                    .collect(toList()));
        }
        if (!targetOids.isEmpty()) {
            parameters.put(AuditEventRecordProvider.PARAMETER_TARGET_NAMES, targetOids);
        }
        if (getAuditLogStorage() != null &&
                ItemPathPanel.ItemPathPanelMode.TEXT_MODE.equals(getAuditLogStorage().getSearchDto().getChangedItemPanelMode())) {
            parameters.put(AuditEventRecordProvider.PARAMETER_CHANGED_ITEM, search.getChangedItem().getPathStringValue());
        } else if (search.getChangedItem().toItemPath() != null) {
            ItemPath itemPath = search.getChangedItem().toItemPath();
            parameters.put(AuditEventRecordProvider.PARAMETER_CHANGED_ITEM, getPrismContext().createCanonicalItemPath(itemPath).asString());
        }
        parameters.put(AuditEventRecordProvider.PARAMETER_EVENT_TYPE, search.getEventType());
        parameters.put(AuditEventRecordProvider.PARAMETER_EVENT_STAGE, search.getEventStage());
        parameters.put(AuditEventRecordProvider.PARAMETER_OUTCOME, search.getOutcome());
        if (isNotEmpty(search.getvalueRefTargetNames())) {
            parameters.put(AuditEventRecordProvider.PARAMETER_VALUE_REF_TARGET_NAMES,
                    search.getvalueRefTargetNames().stream()
                            .map(ObjectType::getName)
                            .map(PolyStringType::getOrig)
                            .collect(toList()));
        }
        return parameters;
    }

    private void initAuditLogViewerTable(Form mainForm) {
        AuditEventRecordProvider provider = new AuditEventRecordProvider(AuditLogViewerPanel.this, getCollectionFroAuditEventModel(),
                this::getAuditEventProviderParameters) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void saveCurrentPage(long from, long count) {
                if (count != 0) {
                    updateCurrentPage(from / count);
                }
//                if (count != 0) {
//                    auditLogStorage.setPageNumber(from / count);
//                }
            }

            @Override
            protected PageStorage getPageStorage(){
                return getAuditLogStorage();
            }

        };
        UserProfileStorage userProfile = getPageBase().getSessionStorage().getUserProfile();
        int pageSize = DEFAULT_PAGE_SIZE;
        if (userProfile.getTables().containsKey(UserProfileStorage.TableId.PAGE_AUDIT_LOG_VIEWER.toString())) {
            pageSize = userProfile.getPagingSize(UserProfileStorage.TableId.PAGE_AUDIT_LOG_VIEWER.toString());
        }
        List<IColumn<AuditEventRecordType, String>> columns = initColumns();
        BoxedTablePanel<AuditEventRecordType> table = new BoxedTablePanel<AuditEventRecordType>(ID_TABLE, provider, columns,
                UserProfileStorage.TableId.PAGE_AUDIT_LOG_VIEWER, pageSize) {
            private static final long serialVersionUID = 1L;

            @Override
            protected WebMarkupContainer createButtonToolbar(String id) {
                CsvDownloadButtonPanel exportDataLink = new CsvDownloadButtonPanel(id) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected String getFilename() {
                        return "AuditLogViewer_" + createStringResource("MainObjectListPanel.exportFileName").getString();
                    }

                    @Override
                    protected void createReportPerformed(String name, SearchFilterType filter, List<Integer> object, AjaxRequestTarget target) {

                    }

                    @Override
                    protected DataTable<?, ?> getDataTable() {
                        return getAuditLogViewerTable().getDataTable();
                    }
                };

                return exportDataLink;
            }

            @Override
            public void setShowPaging(boolean show) {
                //we don't need to do anything here
            }

            };
        table.setShowPaging(true);
        table.setCurrentPage(getCurrentPage());
        table.setOutputMarkupId(true);
        mainForm.add(table);
    }


    protected abstract AuditLogStorage getAuditLogStorage();
    protected abstract void resetAuditSearchStorage();

    protected void updateAuditSearchStorage(AuditSearchDto searchDto){
        getAuditLogStorage().setSearchDto(searchDto);
        getAuditLogStorage().setPageNumber(0);
    }

    protected void updateCurrentPage(long current){
        getAuditLogStorage().setPageNumber(current);
    }

    protected long getCurrentPage(){
        return getAuditLogStorage().getPageNumber();
    }


    private BoxedTablePanel getAuditLogViewerTable(){
        return (BoxedTablePanel) get(ID_MAIN_FORM).get(ID_TABLE);
    }

    protected List<IColumn<AuditEventRecordType, String>> initColumns() {
        List<IColumn<AuditEventRecordType, String>> columns = new ArrayList<>();
        IColumn<AuditEventRecordType, String> linkColumn = new LinkColumn<AuditEventRecordType>(
                createStringResource("AuditEventRecordType.timestamp"), AuditEventRecordProvider.TIMESTAMP_VALUE_PARAMETER,
                AuditEventRecordProvider.TIMESTAMP_VALUE_PARAMETER) {
            private static final long serialVersionUID = 1L;

            @Override
            protected IModel<String> createLinkModel(final IModel<AuditEventRecordType> rowModel){
                return new IModel<String>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public String getObject() {
                        XMLGregorianCalendar time = rowModel.getObject().getTimestamp();
                        return WebComponentUtil.formatDate(time);
                    }
                };
            }
            @Override
            public void onClick(AjaxRequestTarget target, IModel<AuditEventRecordType> rowModel) {
                AuditEventRecordType record = rowModel.getObject();
                try {
                    AuditEventRecord.adopt(record, getPageBase().getPrismContext());
                } catch (SchemaException e) {
                    throw new SystemException("Couldn't adopt event record: " + e, e);
                }
                getPageBase().navigateToNext(new PageAuditLogDetails(record));
            }

        };
        columns.add(linkColumn);

        PropertyColumn<AuditEventRecordType, String> initiatorRefColumn = new PropertyColumn<AuditEventRecordType, String>(createStringResource("AuditEventRecordType.initiatorRef"),
                AuditEventRecordProvider.INITIATOR_OID_PARAMETER, AuditEventRecordType.F_INITIATOR_REF.getLocalPart()) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<AuditEventRecordType>> item, String componentId,
                                     IModel<AuditEventRecordType> rowModel) {
                AuditEventRecordType auditEventRecordType = rowModel.getObject();
                createReferenceColumn(auditEventRecordType.getInitiatorRef(), item, componentId);
            }
        };
        columns.add(initiatorRefColumn);

        if (!isHistory) {
            IColumn<AuditEventRecordType, String> eventStageColumn = new PropertyColumn<AuditEventRecordType, String>(
                createStringResource("PageAuditLogViewer.eventStageLabel"),
                    AuditEventRecordProvider.EVENT_STAGE_PARAMETER, "eventStage"){
                private static final long serialVersionUID = 1L;

                @Override
                public IModel<String> getDataModel(IModel<AuditEventRecordType> rowModel) {
                    return WebComponentUtil.createLocalizedModelForEnum(rowModel.getObject().getEventStage(), AuditLogViewerPanel.this);
                }
            };
            columns.add(eventStageColumn);
        }
        IColumn<AuditEventRecordType, String> eventTypeColumn = new PropertyColumn<AuditEventRecordType, String>(
            createStringResource("PageAuditLogViewer.eventTypeLabel"),
                AuditEventRecordProvider.EVENT_TYPE_PARAMETER, "eventType"){
            private static final long serialVersionUID = 1L;

            @Override
            public IModel<String> getDataModel(IModel<AuditEventRecordType> rowModel) {
                return WebComponentUtil.createLocalizedModelForEnum(rowModel.getObject().getEventType(), AuditLogViewerPanel.this);
            }
        };
        columns.add(eventTypeColumn);

        if (!isHistory) {
            PropertyColumn<AuditEventRecordType, String> targetRefColumn = new PropertyColumn<AuditEventRecordType, String>(createStringResource("AuditEventRecordType.targetRef"),
                    AuditEventRecordProvider.TARGET_OID_PARAMETER, AuditEventRecordType.F_TARGET_REF.getLocalPart()) {
                private static final long serialVersionUID = 1L;

                @Override
                public void populateItem(Item<ICellPopulator<AuditEventRecordType>> item, String componentId,
                                         IModel<AuditEventRecordType> rowModel) {
                    AuditEventRecordType auditEventRecordType = (AuditEventRecordType) rowModel.getObject();
                    createReferenceColumn(auditEventRecordType.getTargetRef(), item, componentId);
                }
            };
            columns.add(targetRefColumn);
        }

        if (!isHistory) {
            PropertyColumn<AuditEventRecordType, String> targetOwnerRefColumn = new PropertyColumn<AuditEventRecordType, String>(createStringResource("AuditEventRecordType.targetOwnerRef"),
                    AuditEventRecordProvider.TARGET_OWNER_OID_PARAMETER, AuditEventRecordType.F_TARGET_OWNER_REF.getLocalPart()) {
                private static final long serialVersionUID = 1L;

                @Override
                public void populateItem(Item<ICellPopulator<AuditEventRecordType>> item, String componentId,
                                         IModel<AuditEventRecordType> rowModel) {
                    AuditEventRecordType auditEventRecordType = (AuditEventRecordType) rowModel.getObject();
                    createReferenceColumn(auditEventRecordType.getTargetOwnerRef(), item, componentId);
                }
            };
            columns.add(targetOwnerRefColumn);
        }
        IColumn<AuditEventRecordType, String> channelColumn = new PropertyColumn<AuditEventRecordType, String>(
                createStringResource("AuditEventRecordType.channel"),
                AuditEventRecordProvider.CHANNEL_PARAMETER, "channel") {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<AuditEventRecordType>> item, String componentId,
                                     IModel<AuditEventRecordType> rowModel) {
                AuditEventRecordType auditEventRecordType = (AuditEventRecordType) rowModel.getObject();
                String channel = auditEventRecordType.getChannel();
                Channel channelValue = null;
                for (Channel chan : Channel.values()) {
                    if (chan.getChannel().equals(channel)) {
                        channelValue = chan;
                        break;
                    } else if(SchemaConstants.CHANGE_CHANNEL_IMPORT_URI.equals(channel)) {
                        channelValue = Channel.IMPORT;
                    }
                }
                if (channelValue != null) {
                    item.add(new Label(componentId, WebComponentUtil.createLocalizedModelForEnum(channelValue, AuditLogViewerPanel.this)));
                } else {
                    item.add(new Label(componentId, ""));
                }
                item.add(new AttributeModifier("style", new Model<>("width: 10%;")));
            }
        };
        columns.add(channelColumn);

        IColumn<AuditEventRecordType, String> outcomeColumn = new PropertyColumn<AuditEventRecordType, String>(
                createStringResource("PageAuditLogViewer.outcomeLabel"),
                AuditEventRecordProvider.OUTCOME_PARAMETER, "outcome") {
            private static final long serialVersionUID = 1L;

            @Override
            public IModel<String> getDataModel(IModel<AuditEventRecordType> rowModel) {
                return WebComponentUtil.createLocalizedModelForEnum(rowModel.getObject().getOutcome(), AuditLogViewerPanel.this);
            }
        };
        columns.add(outcomeColumn);

        return columns;
    }

    private void createReferenceColumn(ObjectReferenceType ref, Item item, String componentId) {
        String name = WebModelServiceUtils.resolveReferenceName(ref, getPageBase(),
                getPageBase().createSimpleTask(OPERATION_RESOLVE_REFENRENCE_NAME),
                new OperationResult(OPERATION_RESOLVE_REFENRENCE_NAME));
        item.add(new Label(componentId, name));
        item.add(new AttributeModifier("style", new Model<>("width: 10%;")));
    }

    private void searchUpdatePerformed(AjaxRequestTarget target){
        getPageBase().getFeedbackPanel().getFeedbackMessages().clear();
        target.add(getPageBase().getFeedbackPanel());
        target.add(getMainFormComponent());
    }

    private Form getMainFormComponent(){
        return (Form) get(ID_MAIN_FORM);
    }

    private Label getHelpComponent(String id, String helpInfo){
        Label help = new Label(id);
        help.add(AttributeModifier.replace("title",createStringResource(helpInfo != null ? helpInfo : "")));
        help.add(new InfoTooltipBehavior());
        help.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(helpInfo)));
        return help;
    }

    private ItemDefinition getItemDefinition(ItemPath itemPath){
        PrismSchema auditSchema = getPageBase().getPrismContext().getSchemaRegistry().findSchemaByCompileTimeClass(AuditEventRecordType.class);
        if (auditSchema != null){
            return auditSchema.findComplexTypeDefinitionByType(AuditEventRecordType.COMPLEX_TYPE).findItemDefinition(itemPath);
        }
        return null;
    }

    private boolean isResourceOidAuditEnabled(){
        OperationResult result = new OperationResult(OPERATION_LOAD_AUDIT_CONFIGURATION);
        try {
            SystemConfigurationAuditType auditConfig = getPageBase().getModelInteractionService().getAuditConfiguration(result);
            if (auditConfig != null && auditConfig.getEventRecording() != null){
                return Boolean.TRUE.equals(auditConfig.getEventRecording().isRecordResourceOids());
            }
        } catch (Exception ex){
            LOGGER.error("Cannot load audit configuration: {}", ex.getMessage());
        }
        return false;
    }

    private ItemPathPanel getChangedItemPanel(){
        return (ItemPathPanel) get(getPageBase().createComponentPath(ID_MAIN_FORM, ID_PARAMETERS_PANEL, ID_CHANGED_ITEM));
    }
}
