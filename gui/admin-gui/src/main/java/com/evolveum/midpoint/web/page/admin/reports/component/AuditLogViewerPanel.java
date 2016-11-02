package com.evolveum.midpoint.web.page.admin.reports.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.form.ValueChooseWrapperPanel;
import com.evolveum.midpoint.web.component.input.DatePanel;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.input.QNameChoiceRenderer;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.yui.calendar.DateTimeField;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.*;

/**
 * Created by honchar.
 */
public class AuditLogViewerPanel extends BasePanel{
    private static final long serialVersionUID = 1L;
    private static final String ID_PARAMETERS_PANEL = "parametersPanel";
    private static final String ID_TABLE = "table";
    private static final String ID_FROM = "fromField";
    private static final String ID_TO = "toField";
    private static final String ID_INITIATOR_NAME = "initiatorNameField";
    private static final String ID_TARGET_NAME = "targetNameField";
    private static final String ID_TARGET_NAME_LABEL = "targetNameLabel";
    private static final String ID_TARGET_OWNER_NAME = "targetOwnerNameField";
    private static final String ID_CHANNEL = "channelField";
    private static final String ID_HOST_IDENTIFIER = "hostIdentifierField";
    private static final String ID_EVENT_TYPE = "eventTypeField";
    private static final String ID_EVENT_STAGE = "eventStageField";
    private static final String ID_EVENT_STAGE_LABEL = "eventStageLabel";
    private static final String ID_OUTCOME = "outcomeField";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_SEARCH_BUTTON = "searchButton";
    private static final String ID_FEEDBACK = "feedback";

    private static final String OPERATION_RESOLVE_REFENRENCE_NAME = AuditLogViewerPanel.class.getSimpleName()
            + ".resolveReferenceName()";

    private IModel<AuditSearchDto> auditSearchDto;
    private AuditSearchDto searchDto;
    private PageBase pageBase;

    public AuditLogViewerPanel(String id, PageBase pageBase){
        this(id, pageBase, null);
    }

    public AuditLogViewerPanel(String id, PageBase pageBase, AuditSearchDto searchDto){
        super(id);
        this.pageBase = pageBase;
        this.searchDto = searchDto;
        initAuditSearchModel();
        initLayout();
    }

    private void initAuditSearchModel(){
        if (searchDto == null){
            searchDto = new AuditSearchDto();
        }
        auditSearchDto = new Model<AuditSearchDto>(searchDto);

    }

    private void initLayout() {
        Form mainForm = new Form(ID_MAIN_FORM);
        mainForm.setOutputMarkupId(true);
        add(mainForm);

        FeedbackPanel feedback = new FeedbackPanel(ID_FEEDBACK);
        feedback.setOutputMarkupId(true);
        mainForm.add(feedback);

        initParametersPanel(mainForm);
        addOrReplaceTable(mainForm);
    }

    private void initParametersPanel(Form mainForm) {
        WebMarkupContainer parametersPanel = new WebMarkupContainer(ID_PARAMETERS_PANEL);
        parametersPanel.setOutputMarkupId(true);
        mainForm.add(parametersPanel);

        PropertyModel<XMLGregorianCalendar> fromModel = new PropertyModel<XMLGregorianCalendar>(
                auditSearchDto, AuditSearchDto.F_FROM);

        DatePanel from = new DatePanel(ID_FROM, fromModel);
        DateValidator dateFromValidator = WebComponentUtil.getRangeValidator(mainForm,
                new ItemPath(AuditSearchDto.F_FROM));
        dateFromValidator.setMessageKey("AuditLogViewerPanel.dateValidatorMessage");
        dateFromValidator.setDateFrom((DateTimeField) from.getBaseFormComponent());
        for (FormComponent<?> formComponent : from.getFormComponents()) {
            formComponent.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        }

        from.setOutputMarkupId(true);
        parametersPanel.add(from);

        PropertyModel<XMLGregorianCalendar> toModel = new PropertyModel<XMLGregorianCalendar>(auditSearchDto,
                AuditSearchDto.F_TO);
        DatePanel to = new DatePanel(ID_TO, toModel);
        DateValidator dateToValidator = WebComponentUtil.getRangeValidator(mainForm,
                new ItemPath(AuditSearchDto.F_FROM));
        dateToValidator.setMessageKey("AuditLogViewerPanel.dateValidatorMessage");
        dateToValidator.setDateTo((DateTimeField) to.getBaseFormComponent());
        for (FormComponent<?> formComponent : to.getFormComponents()) {
            formComponent.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        }

        to.setOutputMarkupId(true);
        parametersPanel.add(to);

        PropertyModel<String> hostIdentifierModel = new PropertyModel<String>(auditSearchDto,
                AuditSearchDto.F_HOST_IDENTIFIER);
        TextPanel<String> hostIdentifier = new TextPanel<String>(ID_HOST_IDENTIFIER, hostIdentifierModel);
        hostIdentifier.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        hostIdentifier.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        hostIdentifier.setOutputMarkupId(true);
        parametersPanel.add(hostIdentifier);

        ListModel<AuditEventTypeType> eventTypeListModel = new ListModel<AuditEventTypeType>(
                Arrays.asList(AuditEventTypeType.values()));
        PropertyModel<AuditEventTypeType> eventTypeModel = new PropertyModel<AuditEventTypeType>(
                auditSearchDto, AuditSearchDto.F_EVENT_TYPE);
        DropDownChoicePanel<AuditEventTypeType> eventType = new DropDownChoicePanel<AuditEventTypeType>(
                ID_EVENT_TYPE, eventTypeModel, eventTypeListModel,
                new EnumChoiceRenderer<AuditEventTypeType>(), true);
        eventType.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        eventType.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        eventType.setOutputMarkupId(true);
        parametersPanel.add(eventType);

        Label eventStageLabel = new Label(ID_EVENT_STAGE_LABEL, pageBase.createStringResource("PageAuditLogViewer.eventStageLabel"));
        eventStageLabel.add(new VisibleEnableBehaviour(){
            @Override
        public boolean isVisible(){
                return isTargetNameVisible();
            }
        });
        eventStageLabel.setOutputMarkupId(true);
        parametersPanel.add(eventStageLabel);

        ListModel<AuditEventStageType> eventStageListModel = new ListModel<AuditEventStageType>(
                Arrays.asList(AuditEventStageType.values()));
        PropertyModel<AuditEventStageType> eventStageModel = new PropertyModel<AuditEventStageType>(
                auditSearchDto, AuditSearchDto.F_EVENT_STAGE);
        DropDownChoicePanel<AuditEventStageType> eventStage = new DropDownChoicePanel<AuditEventStageType>(
                ID_EVENT_STAGE, eventStageModel, eventStageListModel,
                new EnumChoiceRenderer<AuditEventStageType>(), true);
        eventStage.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return isEventStageVisible();
            }
        });
        eventStage.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        eventStage.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        eventStage.setOutputMarkupId(true);
        parametersPanel.add(eventStage);

        ListModel<OperationResultStatusType> outcomeListModel = new ListModel<OperationResultStatusType>(
                Arrays.asList(OperationResultStatusType.values()));
        PropertyModel<OperationResultStatusType> outcomeModel = new PropertyModel<OperationResultStatusType>(
                auditSearchDto, AuditSearchDto.F_OUTCOME);
        DropDownChoicePanel<OperationResultStatusType> outcome = new DropDownChoicePanel<OperationResultStatusType>(
                ID_OUTCOME, outcomeModel, outcomeListModel,
                new EnumChoiceRenderer<OperationResultStatusType>(), true);
        outcome.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        outcome.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        outcome.setOutputMarkupId(true);
        parametersPanel.add(outcome);

        List<String> channelList = WebComponentUtil.getChannelList();
        List<QName> channelQnameList = new ArrayList<QName>();
        for (int i = 0; i < channelList.size(); i++) {
            String channel = channelList.get(i);
            if (channel != null) {
                QName channelQName = QNameUtil.uriToQName(channel);
                channelQnameList.add(channelQName);
            }
        }
        ListModel<QName> channelListModel = new ListModel<QName>(channelQnameList);
        PropertyModel<QName> channelModel = new PropertyModel<QName>(auditSearchDto,
                AuditSearchDto.F_CHANNEL);
        DropDownChoicePanel<QName> channel = new DropDownChoicePanel<QName>(ID_CHANNEL, channelModel,
                channelListModel, new QNameChoiceRenderer(), true);
        channel.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        channel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        channel.setOutputMarkupId(true);
        parametersPanel.add(channel);

        Collection<Class<? extends UserType>> allowedClasses = new ArrayList<>();
        allowedClasses.add(UserType.class);
        ValueChooseWrapperPanel<ObjectReferenceType, UserType> chooseInitiatorPanel = new ValueChooseWrapperPanel<ObjectReferenceType, UserType>(
                ID_INITIATOR_NAME,
                new PropertyModel<ObjectReferenceType>(auditSearchDto, AuditSearchDto.F_INITIATOR_NAME),
                allowedClasses) {

            @Override
            protected void replaceIfEmpty(ObjectType object) {
                ObjectReferenceType ort = ObjectTypeUtil.createObjectRef(object);
                ort.setTargetName(object.getName());
                getModel().setObject(ort);
            }
        };
        parametersPanel.add(chooseInitiatorPanel);

        ValueChooseWrapperPanel<ObjectReferenceType, UserType> chooseTargerOwnerPanel = new ValueChooseWrapperPanel<ObjectReferenceType, UserType>(
                ID_TARGET_OWNER_NAME,
                new PropertyModel<ObjectReferenceType>(auditSearchDto, AuditSearchDto.F_TARGET_OWNER_NAME),
                allowedClasses) {
            @Override
            protected void replaceIfEmpty(ObjectType object) {
                ObjectReferenceType ort = ObjectTypeUtil.createObjectRef(object);
                ort.setTargetName(object.getName());
                getModel().setObject(ort);
            }
        };
        parametersPanel.add(chooseTargerOwnerPanel);

        Label targetNameLabel = new Label(ID_TARGET_NAME_LABEL, pageBase.createStringResource("PageAuditLogViewer.targetNameLabel"));
        targetNameLabel.add(new VisibleEnableBehaviour(){
            @Override
            public boolean isVisible(){
                return searchDto.getTargetName() == null || StringUtils.isEmpty(searchDto.getTargetName().getOid());
            }
        });
        parametersPanel.add(targetNameLabel);
        Collection<Class<? extends ObjectType>> allowedClassesAll = new ArrayList<>();
        allowedClassesAll.addAll(ObjectTypes.getAllObjectTypes());
        ValueChooseWrapperPanel<ObjectReferenceType, ObjectType> chooseTargetPanel = new ValueChooseWrapperPanel<ObjectReferenceType, ObjectType>(
                ID_TARGET_NAME,
                new PropertyModel<ObjectReferenceType>(auditSearchDto, AuditSearchDto.F_TARGET_NAME),
                allowedClassesAll){
            @Override
            protected void replaceIfEmpty(ObjectType object) {
                ObjectReferenceType ort = ObjectTypeUtil.createObjectRef(object);
                ort.setTargetName(object.getName());
                getModel().setObject(ort);
            }
        };
        chooseTargetPanel.add(new VisibleEnableBehaviour(){
            @Override
            public boolean isVisible(){
                return searchDto.getTargetName() == null || StringUtils.isEmpty(searchDto.getTargetName().getOid());
            }
        });
        parametersPanel.add(chooseTargetPanel);

        AjaxSubmitButton ajaxButton = new AjaxSubmitButton(ID_SEARCH_BUTTON,
                createStringResource("BasicSearchPanel.search")) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                Form mainForm = (Form) getParent().getParent();
                addOrReplaceTable(mainForm);
                getFeedbackPanel().getFeedbackMessages().clear();
                target.add(getFeedbackPanel());
                target.add(mainForm);
            }

            @Override
        protected void onError(AjaxRequestTarget target, Form<?> form){
                target.add(getFeedbackPanel());
            }
        };
        ajaxButton.setOutputMarkupId(true);
        parametersPanel.add(ajaxButton);
    }

    private void addOrReplaceTable(Form mainForm) {
        AuditEventRecordProvider provider = new AuditEventRecordProvider(AuditLogViewerPanel.this) {
            private static final long serialVersionUID = 1L;

            public Map<String, Object> getParameters() {
                Map<String, Object> parameters = new HashMap<String, Object>();

                AuditSearchDto search = auditSearchDto.getObject();
                parameters.put("from", search.getFrom());
                parameters.put("to", search.getTo());

                if (search.getChannel() != null) {
                    parameters.put("channel", QNameUtil.qNameToUri(search.getChannel()));
                }
                parameters.put("hostIdentifier", search.getHostIdentifier());

                if (search.getInitiatorName() != null) {
                    parameters.put("initiatorName", search.getInitiatorName().getOid());
                }

                if (search.getTargetOwnerName() != null) {
                    parameters.put("targetOwnerName", search.getTargetOwnerName().getOid());
                }
                if (search.getTargetName() != null) {
                    parameters.put("targetName", search.getTargetName().getOid());
                }

                parameters.put("eventType", search.getEventType());
                parameters.put("eventStage", search.getEventStage());
                parameters.put("outcome", search.getOutcome());
                return parameters;
            }
        };
        BoxedTablePanel table = new BoxedTablePanel(ID_TABLE, provider, initColumns(),
                UserProfileStorage.TableId.PAGE_AUDIT_LOG_VIEWER,
                (int) pageBase.getItemsPerPage(UserProfileStorage.TableId.PAGE_AUDIT_LOG_VIEWER));
        table.setShowPaging(true);
        table.setOutputMarkupId(true);
        mainForm.addOrReplace(table);
    }

    protected List<IColumn<AuditEventRecordType, String>> initColumns() {
        List<IColumn<AuditEventRecordType, String>> columns = new ArrayList<IColumn<AuditEventRecordType, String>>();
        IColumn<AuditEventRecordType, String> linkColumn = new LinkColumn<AuditEventRecordType>(
                createStringResource("AuditEventRecordType.timestamp"), "timestamp") {
            private static final long serialVersionUID = 1L;

            @Override
            protected IModel<String> createLinkModel(final IModel<AuditEventRecordType> rowModel){
                return new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        XMLGregorianCalendar time = rowModel.getObject().getTimestamp();
                        return WebComponentUtil.formatDate(time);
                    }
                };
            }
            @Override
            public void onClick(AjaxRequestTarget target, IModel<AuditEventRecordType> rowModel) {
                setResponsePage(new PageAuditLogDetails(rowModel.getObject()));
            }

        };
        columns.add(linkColumn);

        PropertyColumn<AuditEventRecordType, String> initiatorRefColumn = new PropertyColumn<AuditEventRecordType, String>(createStringResource("AuditEventRecordType.initiatorRef"),
                AuditEventRecordType.F_INITIATOR_REF.getLocalPart()) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<AuditEventRecordType>> item, String componentId,
                                     IModel<AuditEventRecordType> rowModel) {
                AuditEventRecordType auditEventRecordType = (AuditEventRecordType) rowModel.getObject();
                createReferenceColumn(auditEventRecordType.getInitiatorRef(), item, componentId);
            }
        };
        columns.add(initiatorRefColumn);

        if (isEventStageVisible()) {
            IColumn<AuditEventRecordType, String> eventStageColumn = new PropertyColumn<AuditEventRecordType, String>(
                    createStringResource("PageAuditLogViewer.eventStageLabel"), "eventStage");
            columns.add(eventStageColumn);
        }
        IColumn<AuditEventRecordType, String> eventTypeColumn = new PropertyColumn<AuditEventRecordType, String>(
                createStringResource("PageAuditLogViewer.eventTypeLabel"), "eventType");
        columns.add(eventTypeColumn);

        PropertyColumn<AuditEventRecordType, String> targetRefColumn = new PropertyColumn<AuditEventRecordType, String>(createStringResource("AuditEventRecordType.targetRef"),
                AuditEventRecordType.F_TARGET_REF.getLocalPart()) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<AuditEventRecordType>> item, String componentId,
                                     IModel<AuditEventRecordType> rowModel) {
                AuditEventRecordType auditEventRecordType = (AuditEventRecordType) rowModel.getObject();
                createReferenceColumn(auditEventRecordType.getTargetRef(), item, componentId);
            }
        };
        columns.add(targetRefColumn);

        PropertyColumn<AuditEventRecordType, String> targetOwnerRefColumn = new PropertyColumn<AuditEventRecordType, String>(createStringResource("AuditEventRecordType.targetOwnerRef"),
                AuditEventRecordType.F_TARGET_OWNER_REF.getLocalPart()) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<AuditEventRecordType>> item, String componentId,
                                     IModel<AuditEventRecordType> rowModel) {
                AuditEventRecordType auditEventRecordType = (AuditEventRecordType) rowModel.getObject();
                createReferenceColumn(auditEventRecordType.getTargetOwnerRef(), item, componentId);
            }
        };
        columns.add(targetOwnerRefColumn);

        IColumn<AuditEventRecordType, String> channelColumn = new PropertyColumn<AuditEventRecordType, String>(
                createStringResource("AuditEventRecordType.channel"), "channel") {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<AuditEventRecordType>> item, String componentId,
                                     IModel<AuditEventRecordType> rowModel) {
                AuditEventRecordType auditEventRecordType = (AuditEventRecordType) rowModel.getObject();
                String channel = auditEventRecordType.getChannel();
                if (channel != null) {
                    QName channelQName = QNameUtil.uriToQName(channel);
                    String return_ = channelQName.getLocalPart();
                    item.add(new Label(componentId, return_));
                } else {
                    item.add(new Label(componentId, ""));
                }
                item.add(new AttributeModifier("style", new Model<String>("width: 10%;")));
            }
        };
        columns.add(channelColumn);

        IColumn<AuditEventRecordType, String> outcomeColumn = new PropertyColumn<AuditEventRecordType, String>(
                createStringResource("PageAuditLogViewer.outcomeLabel"), "outcome");
        columns.add(outcomeColumn);

        return columns;
    }

//	private PropertyColumn<AuditEventRecordType, String> createReferenceColumn(String columnKey,
//			QName attributeName) {
//		return new PropertyColumn<AuditEventRecordType, String>(createStringResource(columnKey),
//				attributeName.getLocalPart()) {
//			private static final long serialVersionUID = 1L;
//
//			@Override
//			public void populateItem(Item<ICellPopulator<AuditEventRecordType>> item, String componentId,
//					IModel<AuditEventRecordType> rowModel) {
//				AuditEventRecordType auditEventRecordType = (AuditEventRecordType) rowModel.getObject();
//				createReferenceColumn(auditEventRecordType.getTargetRef(), item, componentId);
//			}
//		};
//	}

    private void createReferenceColumn(ObjectReferenceType ref, Item item, String componentId) {
        String name = WebModelServiceUtils.resolveReferenceName(ref, pageBase,
                pageBase.createSimpleTask(OPERATION_RESOLVE_REFENRENCE_NAME),
                new OperationResult(OPERATION_RESOLVE_REFENRENCE_NAME));
        item.add(new Label(componentId, name));
        item.add(new AttributeModifier("style", new Model<String>("width: 10%;")));
    }

    public WebMarkupContainer getFeedbackPanel() {
        return (FeedbackPanel) get(pageBase.createComponentPath(ID_MAIN_FORM, ID_FEEDBACK));
    }

    protected boolean isTargetNameVisible(){
        return true;
    }

    protected boolean isEventStageVisible(){
        return true;
    }
}
