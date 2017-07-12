/*
 * Copyright (c) 2010-2017 Evolveum
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

import com.evolveum.midpoint.gui.api.model.NonEmptyLoadableModel;
import com.evolveum.midpoint.gui.api.model.NonEmptyWrapperModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.paging.NavigatorPanel;
import com.evolveum.midpoint.web.component.form.multivalue.MultiValueAutoCompleteTextPanel;
import com.evolveum.midpoint.web.component.form.multivalue.MultiValueTextEditPanel;
import com.evolveum.midpoint.web.component.input.ObjectReferenceChoiceRenderer;
import com.evolveum.midpoint.web.component.input.QNameChoiceRenderer;
import com.evolveum.midpoint.web.component.input.TriStateComboPanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.component.wizard.WizardStep;
import com.evolveum.midpoint.web.component.wizard.resource.component.synchronization.ConditionalSearchFilterEditor;
import com.evolveum.midpoint.web.component.wizard.resource.component.synchronization.SynchronizationExpressionEditor;
import com.evolveum.midpoint.web.component.wizard.resource.component.synchronization.SynchronizationReactionEditor;
import com.evolveum.midpoint.web.component.wizard.resource.dto.ResourceSynchronizationDto;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.web.page.admin.resources.PageResourceWizard;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.IValidator;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.*;

/**
 * @author lazyman
 * @author shood
 */
public class SynchronizationStep extends WizardStep {

    private static final Trace LOGGER = TraceManager.getTrace(SynchronizationStep.class);

    private static final String DOT_CLASS = SynchronizationStep.class.getName() + ".";
    private static final String OPERATION_SAVE_SYNC = DOT_CLASS + "saveResourceSynchronization";

    private static final String ID_TABLE_ROWS = "tableRows";
    private static final String ID_OBJECT_SYNC_ROW = "objectSyncRow";
    private static final String ID_OBJECT_SYNC_LINK = "objectSyncLink";
    private static final String ID_OBJECT_SYNC_LABEL = "objectSyncName";
    private static final String ID_OBJECT_SYNC_DELETE = "objectSyncDelete";
    private static final String ID_PAGING = "objectSyncPaging";
    private static final String ID_OBJECT_SYNC_ADD = "objectSyncAddButton";
    private static final String ID_OBJECT_SYNC_EDITOR = "objectSyncConfig";
    private static final String ID_THIRD_ROW_CONTAINER = "thirdRowContainer";

	private static final String ID_EDITOR_LABEL = "editorLabel";
    private static final String ID_EDITOR_NAME = "editorName";
    private static final String ID_EDITOR_DESCRIPTION = "editorDescription";
    private static final String ID_EDITOR_KIND = "editorKind";
    private static final String ID_EDITOR_INTENT = "editorIntent";
    private static final String ID_EDITOR_FOCUS = "editorFocus";
    private static final String ID_EDITOR_ENABLED = "editorEnabled";
    private static final String ID_EDITOR_BUTTON_CONDITION = "editorConditionButton";
    private static final String ID_EDITOR_BUTTON_CONFIRMATION = "editorConfirmationButton";
    private static final String ID_EDITOR_OBJECT_TEMPLATE = "editorObjectTemplate";
    private static final String ID_EDITOR_RECONCILE = "editorReconcile";
    private static final String ID_EDITOR_OPPORTUNISTIC = "editorOpportunistic";
    private static final String ID_EDITOR_OBJECT_CLASS = "editorObjectClass";
    private static final String ID_EDITOR_EDITOR_CORRELATION = "editorCorrelation";
    private static final String ID_EDITOR_REACTION = "editorReaction";
    private static final String ID_T_KIND = "kindTooltip";
    private static final String ID_T_INTENT = "intentTooltip";
    private static final String ID_T_OBJ_CLASS = "objectClassTooltip";
    private static final String ID_T_FOCUS = "focusTooltip";
    private static final String ID_T_ENABLED = "enabledTooltip";
    private static final String ID_T_CONDITION = "conditionTooltip";
    private static final String ID_T_CONFIRMATION = "confirmationTooltip";
    private static final String ID_T_OBJ_TEMPLATE = "objectTemplateTooltip";
    private static final String ID_T_RECONCILE = "reconcileTooltip";
    private static final String ID_T_OPPORTUNISTIC = "opportunisticTooltip";
    private static final String ID_T_CORRELATION = "correlationTooltip";
    private static final String ID_T_REACTION = "reactionTooltip";

	@NotNull private final PageResourceWizard parentPage;
    @NotNull private final NonEmptyLoadableModel<PrismObject<ResourceType>> resourceModel;
    @NotNull private final NonEmptyLoadableModel<ResourceSynchronizationDto> syncDtoModel;

    public SynchronizationStep(@NotNull NonEmptyLoadableModel<PrismObject<ResourceType>> resourceModel, @NotNull PageResourceWizard parentPage) {
        super(parentPage);
		this.parentPage = parentPage;
        this.resourceModel = resourceModel;

        syncDtoModel = new NonEmptyLoadableModel<ResourceSynchronizationDto>(false) {
            @Override
			@NotNull
            protected ResourceSynchronizationDto load() {
                return loadResourceSynchronization();
            }
        };
		parentPage.registerDependentModel(syncDtoModel);

        initLayout();
		setOutputMarkupId(true);
    }

	@NotNull
    private ResourceSynchronizationDto loadResourceSynchronization() {

		if (resourceModel.getObject().asObjectable().getSynchronization() == null) {
			resourceModel.getObject().asObjectable().setSynchronization(new SynchronizationType());
		}

		ResourceSynchronizationDto dto = new ResourceSynchronizationDto(resourceModel.getObject().asObjectable().getSynchronization().getObjectSynchronization());
        dto.setObjectClassList(loadResourceObjectClassList(resourceModel, LOGGER, parentPage.getString("SynchronizationStep.message.errorLoadingObjectSyncList")));
        return dto;
    }

    private boolean isAnySelected() {
		return syncDtoModel.getObject().getSelected() != null;
    }

    private void initLayout() {
        final ListDataProvider<ObjectSynchronizationType> syncProvider = new ListDataProvider<>(this,
                new PropertyModel<List<ObjectSynchronizationType>>(syncDtoModel, ResourceSynchronizationDto.F_OBJECT_SYNCRONIZATION_LIST));

        //first row - object sync list
        WebMarkupContainer tableBody = new WebMarkupContainer(ID_TABLE_ROWS);
        tableBody.setOutputMarkupId(true);
        add(tableBody);

        //second row - ObjectSynchronizationType editor
        WebMarkupContainer objectSyncEditor = new WebMarkupContainer(ID_OBJECT_SYNC_EDITOR);
        objectSyncEditor.setOutputMarkupId(true);
        objectSyncEditor.add(new VisibleEnableBehaviour(){

            @Override
            public boolean isVisible(){
                return isAnySelected();
            }

        });
        add(objectSyncEditor);

        //third row - container for more specific editors
        WebMarkupContainer thirdRowContainer = new WebMarkupContainer(ID_THIRD_ROW_CONTAINER);
        thirdRowContainer.setOutputMarkupId(true);
        add(thirdRowContainer);

        DataView<ObjectSynchronizationType> syncDataView = new DataView<ObjectSynchronizationType>(ID_OBJECT_SYNC_ROW,
                syncProvider, UserProfileStorage.DEFAULT_PAGING_SIZE) {

            @Override
            protected void populateItem(Item<ObjectSynchronizationType> item) {
                final ObjectSynchronizationType syncObject = item.getModelObject();

                AjaxSubmitLink link = new AjaxSubmitLink(ID_OBJECT_SYNC_LINK) {

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        editSyncObjectPerformed(target, syncObject);
                    }
                };
                item.add(link);

                Label label = new Label(ID_OBJECT_SYNC_LABEL, createObjectSyncTypeDisplayModel(syncObject));
                label.setOutputMarkupId(true);
                link.add(label);

                AjaxLink delete = new AjaxLink(ID_OBJECT_SYNC_DELETE){

                    @Override
                    public void onClick(AjaxRequestTarget target){
                        deleteSyncObjectPerformed(target, syncObject);
                    }
                };
				parentPage.addEditingVisibleBehavior(delete);
                link.add(delete);

                item.add(AttributeModifier.replace("class", new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        if (isSelected(syncObject)) {
                            return "success";
                        }

                        return null;
                    }
                }));

            }
        };
        tableBody.add(syncDataView);

        NavigatorPanel navigator = new NavigatorPanel(ID_PAGING, syncDataView, true);
        navigator.setOutputMarkupId(true);
        navigator.setOutputMarkupPlaceholderTag(true);
        add(navigator);

        AjaxLink add = new AjaxLink(ID_OBJECT_SYNC_ADD) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                addSyncObjectPerformed(target);
            }
        };
		parentPage.addEditingVisibleBehavior(add);
        add(add);

		initObjectSyncEditor(objectSyncEditor);
    }

	@Override
	protected void onConfigure() {
		super.onConfigure();
		if (!isAnySelected()) {
			insertEmptyThirdRow();
		}
	}

	private void initObjectSyncEditor(WebMarkupContainer editor){
        Label editorLabel = new Label(ID_EDITOR_LABEL, new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                if (!isAnySelected()) {
					return null;        // shouldn't occur
				}
				String name = syncDtoModel.getObject().getSelected().getName() != null ? syncDtoModel.getObject().getSelected().getName() : "";
				return getString("SynchronizationStep.label.editSyncObject", name);
            }
        });
		editorLabel.setOutputMarkupId(true);
        editor.add(editorLabel);

        TextField editorName = new TextField<>(ID_EDITOR_NAME, new PropertyModel<String>(syncDtoModel,
                ResourceSynchronizationDto.F_SELECTED + ".name"));
		editorName.add(new UpdateNamesBehaviour());
		parentPage.addEditingEnabledBehavior(editorName);
        editor.add(editorName);

        TextArea editorDescription = new TextArea<>(ID_EDITOR_DESCRIPTION, new PropertyModel<String>(syncDtoModel,
                ResourceSynchronizationDto.F_SELECTED + ".description"));
		parentPage.addEditingEnabledBehavior(editorDescription);
        editor.add(editorDescription);

        DropDownChoice editorKind = new DropDownChoice<>(ID_EDITOR_KIND,
                new PropertyModel<ShadowKindType>(syncDtoModel, ResourceSynchronizationDto.F_SELECTED + ".kind"),
                WebComponentUtil.createReadonlyModelFromEnum(ShadowKindType.class),
                new EnumChoiceRenderer<ShadowKindType>());
        editorKind.setNullValid(true);
		editorKind.add(new UpdateNamesBehaviour());
		parentPage.addEditingEnabledBehavior(editorKind);
        editor.add(editorKind);

        TextField editorIntent = new TextField<>(ID_EDITOR_INTENT, new PropertyModel<String>(syncDtoModel,
                ResourceSynchronizationDto.F_SELECTED + ".intent"));
		editorIntent.add(new UpdateNamesBehaviour());
		parentPage.addEditingEnabledBehavior(editorIntent);
        editor.add(editorIntent);

        MultiValueAutoCompleteTextPanel<QName> editorObjectClass = new MultiValueAutoCompleteTextPanel<QName>(ID_EDITOR_OBJECT_CLASS,
                new PropertyModel<List<QName>>(syncDtoModel, ResourceSynchronizationDto.F_SELECTED + ".objectClass"), true, parentPage.getReadOnlyModel()) {

            @Override
            protected IModel<String> createTextModel(final IModel<QName> model) {
                return new PropertyModel<>(model, "localPart");
            }

            @Override
            protected QName createNewEmptyItem() {
                return new QName("");
            }

            @Override
            protected boolean buttonsDisabled() {
                return !isAnySelected();
            }

            @Override
            protected List<QName> createObjectList() {
                return syncDtoModel.getObject().getObjectClassList();
            }

            @Override
            protected String createAutoCompleteObjectLabel(QName object) {
                return object.getLocalPart();
            }

            @Override
            protected IValidator<String> createAutoCompleteValidator(){
                return createObjectClassValidator(new AbstractReadOnlyModel<List<QName>>() {
                    @Override
					public List<QName> getObject() {
                        return syncDtoModel.getObject().getObjectClassList();
                    }
                });
            }
        };
		parentPage.addEditingEnabledBehavior(editorObjectClass);
        editor.add(editorObjectClass);

        // TODO: switch to ObjectTypeSelectPanel
        DropDownChoice editorFocus = new DropDownChoice<>(ID_EDITOR_FOCUS, new PropertyModel<QName>(syncDtoModel,
                ResourceSynchronizationDto.F_SELECTED + ".focusType"),
                new AbstractReadOnlyModel<List<QName>>() {

                    @Override
                    public List<QName> getObject() {
                        return WebComponentUtil.createFocusTypeList();
                    }
                }, new QNameChoiceRenderer());
        editorFocus.setNullValid(true);
		editorFocus.add(new UpdateNamesBehaviour());
		parentPage.addEditingEnabledBehavior(editorFocus);
        editor.add(editorFocus);

        CheckBox editorEnabled = new CheckBox(ID_EDITOR_ENABLED, new PropertyModel<Boolean>(syncDtoModel,
                ResourceSynchronizationDto.F_SELECTED + ".enabled"));
		parentPage.addEditingEnabledBehavior(editorEnabled);
        editor.add(editorEnabled);

        AjaxSubmitLink editorCondition = new AjaxSubmitLink(ID_EDITOR_BUTTON_CONDITION){

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                conditionEditPerformed(target);
            }
        };
        addDisableClassModifier(editorCondition);
        editor.add(editorCondition);

        AjaxSubmitLink editorConfirmation = new AjaxSubmitLink(ID_EDITOR_BUTTON_CONFIRMATION){

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                confirmationEditPerformed(target);
            }
        };
        addDisableClassModifier(editorConfirmation);
        editor.add(editorConfirmation);

        DropDownChoice editorObjectTemplate = new DropDownChoice<>(ID_EDITOR_OBJECT_TEMPLATE,
                new PropertyModel<ObjectReferenceType>(syncDtoModel, ResourceSynchronizationDto.F_SELECTED + ".objectTemplateRef"),
                new AbstractReadOnlyModel<List<ObjectReferenceType>>() {

                    @Override
                    public List<ObjectReferenceType> getObject() {
                    	return WebModelServiceUtils.createObjectReferenceList(ObjectTemplateType.class, getPageBase(), syncDtoModel.getObject().getObjectTemplateMap());
                    }
                }, new ObjectReferenceChoiceRenderer(syncDtoModel.getObject().getObjectTemplateMap()));
        editorObjectTemplate.setNullValid(true);
		parentPage.addEditingEnabledBehavior(editorObjectTemplate);
        editor.add(editorObjectTemplate);

        CheckBox editorReconcile = new CheckBox(ID_EDITOR_RECONCILE, new PropertyModel<Boolean>(syncDtoModel,
                ResourceSynchronizationDto.F_SELECTED + ".reconcile"));
		parentPage.addEditingEnabledBehavior(editorReconcile);
        editor.add(editorReconcile);

        TriStateComboPanel opportunistic = new TriStateComboPanel(ID_EDITOR_OPPORTUNISTIC, new PropertyModel<Boolean>(syncDtoModel, ResourceSynchronizationDto.F_SELECTED + ".opportunistic"));
		parentPage.addEditingEnabledBehavior(opportunistic);
        editor.add(opportunistic);

        MultiValueTextEditPanel editorCorrelation = new MultiValueTextEditPanel<ConditionalSearchFilterType>(ID_EDITOR_EDITOR_CORRELATION,
                new PropertyModel<List<ConditionalSearchFilterType>>(syncDtoModel, ResourceSynchronizationDto.F_SELECTED + ".correlation"),
				new PropertyModel<ConditionalSearchFilterType>(syncDtoModel, ResourceSynchronizationDto.F_SELECTED_CORRELATION),
				false, true, parentPage.getReadOnlyModel()) {

            @Override
            protected IModel<String> createTextModel(final IModel<ConditionalSearchFilterType> model) {
                return new Model<String>() {

                    @Override
                    public String getObject() {
                        StringBuilder sb = new StringBuilder();

                        ConditionalSearchFilterType searchFilter = model.getObject();
                        if(searchFilter != null && searchFilter.getDescription() != null){
                            sb.append(searchFilter.getDescription());
                        }

                        if(sb.toString().isEmpty()){
                            sb.append(getString("SynchronizationStep.label.notSpecified"));
                        }

                        return sb.toString();
                    }
                };
            }

            @Override
            protected ConditionalSearchFilterType createNewEmptyItem(){
                return new ConditionalSearchFilterType();
            }

            @Override
            protected void editPerformed(AjaxRequestTarget target, ConditionalSearchFilterType object) {
                correlationEditPerformed(target, object);
            }

			@Override
			protected void performAddValueHook(AjaxRequestTarget target, ConditionalSearchFilterType added) {
				parentPage.refreshIssues(target);
			}

			@Override
			protected void performRemoveValueHook(AjaxRequestTarget target, ListItem<ConditionalSearchFilterType> item) {
				parentPage.refreshIssues(target);
			}

			@Override
            protected boolean buttonsDisabled(){
                return !isAnySelected();
            }
        };
        editor.add(editorCorrelation);

        MultiValueTextEditPanel editorReaction = new MultiValueTextEditPanel<SynchronizationReactionType>(ID_EDITOR_REACTION,
                new PropertyModel<List<SynchronizationReactionType>>(syncDtoModel, ResourceSynchronizationDto.F_SELECTED + ".reaction"),
                new PropertyModel<SynchronizationReactionType>(syncDtoModel, ResourceSynchronizationDto.F_SELECTED_REACTION),
				false, true, parentPage.getReadOnlyModel()) {

            @Override
            protected IModel<String> createTextModel(final IModel<SynchronizationReactionType> model) {
                return new Model<String>() {

                    @Override
                    public String getObject() {
						SynchronizationReactionType reaction = model.getObject();
                        if (reaction == null) {
							return "";
						}
						StringBuilder sb = new StringBuilder();
                        sb.append(reaction.getName() != null ? reaction.getName() + " " : "");
						sb.append("(");
						if (reaction.getSituation() != null) {
							sb.append(reaction.getSituation());
                        }
						if (Boolean.TRUE.equals(reaction.isSynchronize()) || !reaction.getAction().isEmpty()) {
							sb.append(" -> ");
							if (!reaction.getAction().isEmpty()) {
								boolean first = true;
								for (SynchronizationActionType action : reaction.getAction()) {
									if (first) {
										first = false;
									} else {
										sb.append(", ");
									}
									sb.append(StringUtils.substringAfter(action.getHandlerUri(), "#"));
								}
							} else {
								sb.append("sync");	// TODO i18n
							}
						}
						sb.append(")");
                        return sb.toString();
                    }
                };
            }

            @Override
            protected SynchronizationReactionType createNewEmptyItem(){
                return new SynchronizationReactionType();
            }

            @Override
            protected void editPerformed(AjaxRequestTarget target, SynchronizationReactionType object){
                reactionEditPerformed(target, object);
            }

			@Override
			protected void performAddValueHook(AjaxRequestTarget target, SynchronizationReactionType added) {
				parentPage.refreshIssues(target);
			}

			@Override
			protected void performRemoveValueHook(AjaxRequestTarget target, ListItem<SynchronizationReactionType> item) {
				parentPage.refreshIssues(target);
			}

			@Override
            protected boolean buttonsDisabled(){
                return !isAnySelected();
            }
        };
		editorReaction.setOutputMarkupId(true);
        editor.add(editorReaction);

        Label kindTooltip = new Label(ID_T_KIND);
        kindTooltip.add(new InfoTooltipBehavior());
        editor.add(kindTooltip);

        Label intentTooltip = new Label(ID_T_INTENT);
        intentTooltip.add(new InfoTooltipBehavior());
        editor.add(intentTooltip);

        Label objClassTooltip = new Label(ID_T_OBJ_CLASS);
        objClassTooltip.add(new InfoTooltipBehavior());
        editor.add(objClassTooltip);

        Label focusTooltip = new Label(ID_T_FOCUS);
        focusTooltip.add(new InfoTooltipBehavior());
        editor.add(focusTooltip);

        Label enabledTooltip = new Label(ID_T_ENABLED);
        enabledTooltip.add(new InfoTooltipBehavior());
        editor.add(enabledTooltip);

        Label conditionTooltip = new Label(ID_T_CONDITION);
        conditionTooltip.add(new InfoTooltipBehavior());
        editor.add(conditionTooltip);

        Label confirmationTooltip = new Label(ID_T_CONFIRMATION);
        confirmationTooltip.add(new InfoTooltipBehavior());
        editor.add(confirmationTooltip);

        Label objTemplateTooltip = new Label(ID_T_OBJ_TEMPLATE);
        objTemplateTooltip.add(new InfoTooltipBehavior());
        editor.add(objTemplateTooltip);

        Label reconcileTooltip = new Label(ID_T_RECONCILE);
        reconcileTooltip.add(new InfoTooltipBehavior());
        editor.add(reconcileTooltip);

        Label opportunisticTooltip = new Label(ID_T_OPPORTUNISTIC);
        opportunisticTooltip.add(new InfoTooltipBehavior());
        editor.add(opportunisticTooltip);

        Label correlationTooltip = new Label(ID_T_CORRELATION);
        correlationTooltip.add(new InfoTooltipBehavior());
        editor.add(correlationTooltip);

        Label reactionTooltip = new Label(ID_T_REACTION);
        reactionTooltip.add(new InfoTooltipBehavior());
        editor.add(reactionTooltip);
    }

    private IModel<String> createObjectSyncTypeDisplayModel(final ObjectSynchronizationType syncObject){
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                StringBuilder sb = new StringBuilder();

                if (syncObject != null) {
                    sb.append(syncObject.getName() != null ? syncObject.getName() + " " : "");
					SchemaHandlingStep.addKindAndIntent(sb, syncObject.getKind(), syncObject.getIntent());
					sb.append(" => ");
					sb.append(getTypeDisplayName(ResourceTypeUtil.fillDefaultFocusType(syncObject.getFocusType())));
                }

                return sb.toString();
            }
        };
    }

	// TODO move to some utils
	private static String getTypeDisplayName(@NotNull QName name) {
		return StringUtils.removeEnd(name.getLocalPart(), "Type");
	}

	private void addDisableClassModifier(Component component){
        component.add(new AttributeAppender("class", new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                if(!isAnySelected()){
                    return " disabled";
                }

                return null;
            }
        }));
    }

    private Component getSyncObjectTable(){
        return get(ID_TABLE_ROWS);
    }

    private Component getNavigator(){
        return get(ID_PAGING);
    }

	private Component getSyncObjectEditor(){
        return get(ID_OBJECT_SYNC_EDITOR);
    }

	public Component getReactionList() {
		return get(createComponentPath(ID_OBJECT_SYNC_EDITOR, ID_EDITOR_REACTION));
	}

	public Component getCorrelationList() {
		return get(createComponentPath(ID_OBJECT_SYNC_EDITOR, ID_EDITOR_EDITOR_CORRELATION));
	}

    private Component getThirdRowContainer(){
        return get(ID_THIRD_ROW_CONTAINER);
    }

    private void insertEmptyThirdRow(){
        getThirdRowContainer().replaceWith(new WebMarkupContainer(ID_THIRD_ROW_CONTAINER));
    }

    private void conditionEditPerformed(AjaxRequestTarget target){
        WebMarkupContainer newContainer = new SynchronizationExpressionEditor(ID_THIRD_ROW_CONTAINER,
                new PropertyModel<ExpressionType>(syncDtoModel, ResourceSynchronizationDto.F_SELECTED + ".condition"), parentPage) {

            @Override
            public String getLabel(){
                return "SynchronizationExpressionEditor.label.condition";
            }
        };
        getThirdRowContainer().replaceWith(newContainer);
		resetSelections(target);
        target.add(getThirdRowContainer(), get(ID_OBJECT_SYNC_EDITOR), getPageBase().getFeedbackPanel());
    }

    private void confirmationEditPerformed(AjaxRequestTarget target){
        WebMarkupContainer newContainer = new SynchronizationExpressionEditor(ID_THIRD_ROW_CONTAINER,
                new PropertyModel<ExpressionType>(syncDtoModel, ResourceSynchronizationDto.F_SELECTED + ".confirmation"), parentPage){

            @Override
            public String getLabel(){
                return "SynchronizationExpressionEditor.label.confirmation";
            }
        };
        getThirdRowContainer().replaceWith(newContainer);
		resetSelections(target);
        target.add(getThirdRowContainer(), get(ID_OBJECT_SYNC_EDITOR), getPageBase().getFeedbackPanel());
    }

    private void correlationEditPerformed(AjaxRequestTarget target, @NotNull ConditionalSearchFilterType condition) {
		if (condition.getCondition() == null) {
			condition.setCondition(new ExpressionType());			// removed at save
		}
		resetSelections(target);
		syncDtoModel.getObject().setSelectedCorrelation(condition);
        WebMarkupContainer newContainer = new ConditionalSearchFilterEditor(ID_THIRD_ROW_CONTAINER,
				new NonEmptyWrapperModel<>(new Model<>(condition)), parentPage);
        getThirdRowContainer().replaceWith(newContainer);

        target.add(getThirdRowContainer(), get(ID_OBJECT_SYNC_EDITOR), getPageBase().getFeedbackPanel());
    }

    private void reactionEditPerformed(AjaxRequestTarget target, SynchronizationReactionType reaction){
		WebMarkupContainer newContainer = new SynchronizationReactionEditor(ID_THIRD_ROW_CONTAINER, new Model<>(reaction), this, parentPage);
		getThirdRowContainer().replaceWith(newContainer);

		for (SynchronizationActionType action : reaction.getAction()) {
			if (action.getRef() != null) {
				warn(getString("SynchronizationStep.message.unsupportedActionFormat"));
				break;
			}
		}

		resetSelections(target);
		syncDtoModel.getObject().setSelectedReaction(reaction);

        target.add(getThirdRowContainer(), get(ID_OBJECT_SYNC_EDITOR), getPageBase().getFeedbackPanel());
    }

    @Override
    public void applyState() {
		parentPage.refreshIssues(null);
		if (parentPage.isReadOnly() || !isComplete()) {
			return;
		}
		savePerformed();
		insertEmptyThirdRow();
		resetSelections(null);
	}

    private void savePerformed() {
        PrismObject<ResourceType> oldResource;
        PrismObject<ResourceType> newResource = resourceModel.getObject();
        Task task = getPageBase().createSimpleTask(OPERATION_SAVE_SYNC);
        OperationResult result = task.getResult();
        ModelService modelService = getPageBase().getModelService();
		boolean saved = false;

        removeEmptyContainers(newResource.asObjectable());

        try {
            oldResource = WebModelServiceUtils.loadObject(ResourceType.class, newResource.getOid(), getPageBase(), task, result);
            if (oldResource != null) {
                ObjectDelta<ResourceType> delta = parentPage.computeDiff(oldResource, newResource);
				if (!delta.isEmpty()) {
					parentPage.logDelta(delta);
					Collection<ObjectDelta<? extends ObjectType>> deltas = WebComponentUtil.createDeltaCollection(delta);
					modelService.executeChanges(deltas, null, getPageBase().createSimpleTask(OPERATION_SAVE_SYNC), result);
					parentPage.resetModels();
					syncDtoModel.reset();
					saved = true;
				}
            }
        } catch (CommonException|RuntimeException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't save resource synchronization.", e);
            result.recordFatalError(getString("SynchronizationStep.message.cantSave", e));
        } finally {
            result.computeStatusIfUnknown();
            setResult(result);
        }

		if (parentPage.showSaveResultInPage(saved, result)) {
            getPageBase().showResult(result);
        }
    }

	private void removeEmptyContainers(ResourceType resourceType) {
		if (resourceType.getSynchronization() == null) {
			return;
		}

		for (ObjectSynchronizationType objectSync : resourceType.getSynchronization().getObjectSynchronization()) {
			objectSync.getObjectClass().removeIf(name -> name == null || StringUtils.isBlank(name.getLocalPart()));
			if (objectSync.getCondition() != null && ExpressionUtil.isEmpty(objectSync.getCondition())) {
				objectSync.setCondition(null);
			}
			if (objectSync.getConfirmation() != null && ExpressionUtil.isEmpty(objectSync.getConfirmation())) {
				objectSync.setConfirmation(null);
			}
			for (ConditionalSearchFilterType correlationFilter : objectSync.getCorrelation()) {
				if (correlationFilter.getCondition() != null && ExpressionUtil.isEmpty(correlationFilter.getCondition())) {
					correlationFilter.setCondition(null);
				}
			}
		}
	}

    private void editSyncObjectPerformed(AjaxRequestTarget target, ObjectSynchronizationType syncObject) {
		boolean wasAnySelected = isAnySelected();
		syncDtoModel.getObject().setSelected(syncObject);
		insertEmptyThirdRow();
		resetSelections(target);
		if (wasAnySelected) {
			target.add(getSyncObjectTable(), getNavigator(), getSyncObjectEditor(), getThirdRowContainer());
		} else {
			target.add(this);
		}
    }

    private void deleteSyncObjectPerformed(AjaxRequestTarget target, ObjectSynchronizationType syncObject) {
        if (isSelected(syncObject)) {
			syncDtoModel.getObject().setSelected(null);
            insertEmptyThirdRow();
			resetSelections(target);
            target.add(getThirdRowContainer());
        }

		ArrayList<ObjectSynchronizationType> list = (ArrayList<ObjectSynchronizationType>) syncDtoModel.getObject().getObjectSynchronizationList();
		list.remove(syncObject);
		if (list.isEmpty()) {
            insertEmptyThirdRow();
			resetSelections(target);
            target.add(getThirdRowContainer());
        }

        target.add(getSyncObjectEditor(), getSyncObjectTable(), getNavigator());
		parentPage.refreshIssues(target);
    }

	private boolean isSelected(ObjectSynchronizationType syncObject) {
		return syncDtoModel.getObject().getSelected() == syncObject;
	}

	private void addSyncObjectPerformed(AjaxRequestTarget target){
        ObjectSynchronizationType syncObject = new ObjectSynchronizationType();
		syncObject.setEnabled(true);
        //syncObject.setName(generateName(getString("SynchronizationStep.label.newObjectType")));

        resourceModel.getObject().asObjectable().getSynchronization().getObjectSynchronization().add(syncObject);
		editSyncObjectPerformed(target, syncObject);
		parentPage.refreshIssues(target);
    }

	private class UpdateNamesBehaviour extends EmptyOnChangeAjaxFormUpdatingBehavior {
		@Override
		protected void onUpdate(AjaxRequestTarget target) {
			target.add(getSyncObjectTable(), getSyncObjectEditor().get(ID_EDITOR_LABEL));
			parentPage.refreshIssues(target);
		}
	}

	private void resetSelections(AjaxRequestTarget target) {
		ResourceSynchronizationDto dto = syncDtoModel.getObject();
		if (dto.getSelectedCorrelation() != null) {
			dto.setSelectedCorrelation(null);
			if (target != null) {
				target.add(getCorrelationList());
			}
		}
		if (dto.getSelectedReaction() != null) {
			dto.setSelectedReaction(null);
			if (target != null) {
				target.add(getReactionList());
			}
		}
	}

	private String generateName(String prefix) {
		List<String> existing = new ArrayList<>();
		for (ObjectSynchronizationType sync : syncDtoModel.getObject().getObjectSynchronizationList()) {
			CollectionUtils.addIgnoreNull(existing, sync.getName());
		}
		return SchemaHandlingStep.generateName(existing, prefix);
	}

}
