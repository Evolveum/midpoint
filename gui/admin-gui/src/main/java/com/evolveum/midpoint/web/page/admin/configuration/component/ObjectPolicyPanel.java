/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.configuration.component;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemConstraintType;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.form.DropDownFormGroup;
import com.evolveum.midpoint.web.component.input.ChoiceableChoiceRenderer;
import com.evolveum.midpoint.web.component.input.QNameObjectTypeChoiceRenderer;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.dto.ObjectPolicyDialogDto;
import com.evolveum.midpoint.web.page.admin.configuration.dto.ObjectTemplateConfigTypeReferenceDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectPolicyConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;


/**
 * @author shood
 */
public class ObjectPolicyPanel extends BasePanel<ObjectPolicyDialogDto> implements Popupable{

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ObjectPolicyPanel.class);

    private static final String DOT_CLASS = ObjectPolicyPanel.class.getName() + ".";

    private static final String OPERATION_LOAD_ALL_OBJECT_TEMPLATES = DOT_CLASS + "loadObjectTemplates";

    private static final String ID_FORM = "mainForm";
    private static final String ID_TYPE = "type";
    private static final String ID_SUBTYPE = "subtype";
    private static final String ID_OBJECT_TEMPLATE = "objectTemplate";
    private static final String ID_BUTTON_SAVE = "saveButton";
    private static final String ID_BUTTON_CANCEL = "cancelButton";
    private static final String ID_OID_BOUND = "oidBound";
    private static final String ID_PROPERTY = "property";
    private static final String ID_PROPERTY_PATH = "propertyPath";
    private static final String ID_REPEATER = "repeater";
    private static final String ID_TEXT_WRAPPER = "textWrapper";
    private static final String ID_BUTTON_GROUP = "buttonGroup";
    private static final String ID_BUTTON_REMOVE = "remove";
    private static final String ID_BUTTON_ADD = "add";
    private static final String ID_FEEDBACK = "feedback";
    private static final String ID_CONFLICT_RESOLUTION_CONTAINER = "conflictResolutionContainer";

    private static final String ID_LABEL_SIZE = "col-md-4";
    private static final String ID_INPUT_SIZE = "col-md-8";

    private static final String CLASS_MULTI_VALUE = "multivalue-form";
    private static final String OFFSET_CLASS = "col-md-offset-4";

    private IModel<ObjectPolicyDialogDto> model;

    public ObjectPolicyPanel(String id, final ObjectPolicyConfigurationType config) {
        super(id);

        model = new LoadableModel<ObjectPolicyDialogDto>(false) {
            private static final long serialVersionUID = 1L;

            @Override
            protected ObjectPolicyDialogDto load() {
                return loadModel(config);
            }
        };

        initLayout(config);

        setOutputMarkupId(true);
    }

    private ObjectPolicyDialogDto loadModel(ObjectPolicyConfigurationType config) {
        ObjectPolicyDialogDto dto = new ObjectPolicyDialogDto(config, getPageBase());
        return dto;
    }

    public StringResourceModel createStringResource(String resourceKey, Object... objects) {
        return PageBase.createStringResourceStatic(this, resourceKey, objects);
    }

    public void initLayout(ObjectPolicyConfigurationType config) {
        Form<?> form = new Form<>(ID_FORM);
        form.setOutputMarkupId(true);
        add(form);

        DropDownFormGroup<QName> type = new DropDownFormGroup<>(ID_TYPE,
            new PropertyModel<>(model, ObjectPolicyDialogDto.F_TYPE), createTypeChoiceList(),
                new QNameObjectTypeChoiceRenderer(), createStringResource("ObjectPolicyDialog.type"), ID_LABEL_SIZE,
                ID_INPUT_SIZE, false);
        form.add(type);
        type.getInput().setNullValid(config.getConflictResolution() != null);
        type.getInput().setRequired(config.getConflictResolution() == null);           // traditional template entries still require object type
        TextField<String> fieldSubtype = new TextField<>(ID_SUBTYPE, new PropertyModel<String>(model, ObjectPolicyDialogDto.F_SUBTYPE));
        form.add(fieldSubtype);
        form.add(fieldSubtype);

        DropDownFormGroup<ObjectTemplateConfigTypeReferenceDto> template = new DropDownFormGroup<>(ID_OBJECT_TEMPLATE,
            new PropertyModel<>(model, ObjectPolicyDialogDto.F_TEMPLATE_REF),
                createObjectTemplateList(), new ChoiceableChoiceRenderer<>(),
                createStringResource("ObjectPolicyDialog.template"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        form.add(template);
        template.getInput().setNullValid(config.getConflictResolution() != null);
        template.getInput().setRequired(config.getConflictResolution() == null);

        WebMarkupContainer conflictResolutionContainer = new WebMarkupContainer(ID_CONFLICT_RESOLUTION_CONTAINER);
        conflictResolutionContainer.setVisible(config.getConflictResolution() != null);
        form.add(conflictResolutionContainer);

        ListView<ItemConstraintType> repeater = new ListView<ItemConstraintType>(ID_REPEATER,
            new PropertyModel<>(model, ObjectPolicyDialogDto.F_PROPERTY_LIST)) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<ItemConstraintType> item) {
                WebMarkupContainer textWrapper = new WebMarkupContainer(ID_TEXT_WRAPPER);
                textWrapper.setOutputMarkupId(true);
                textWrapper.add(AttributeAppender.prepend("class", new IModel<String>() {

                    private static final long serialVersionUID = 1L;
                    @Override
                    public String getObject() {
                        if (item.getIndex() > 0) {
                            return OFFSET_CLASS + " " + CLASS_MULTI_VALUE;
                        }

                        return null;
                    }
                }));
                item.add(textWrapper);


                ItemPathType itemPathType = (item.getModelObject() != null ) ? item.getModelObject().getPath() : null;
                String pathToShow = itemPathType != null ? itemPathType.getItemPath().toString() : null;

                TextField<String> property = new TextField<>(ID_PROPERTY, Model.of(pathToShow));

                property.add(new AjaxFormComponentUpdatingBehavior("blur") {
                    private static final long serialVersionUID = 1L;
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        Component component = this.getComponent();
                        String newValue = (String) component.getDefaultModelObject();
                        ItemPathType itemPathType = null;
                        if (StringUtils.isNotBlank(newValue)) {
                          itemPathType = getPrismContext().itemPathParser().asItemPathType(newValue);
                        }
                        item.getModelObject().setPath(itemPathType);
                    }
                });
                property.add(AttributeAppender.replace("placeholder",
                        createStringResource("ObjectPolicyDialog.property.placeholder")));
                textWrapper.add(property);



                CheckBox oidBound = new CheckBox(ID_OID_BOUND,
                    new PropertyModel<>(item.getModel(), ItemConstraintType.F_OID_BOUND.getLocalPart()));

                oidBound.add(AttributeModifier.replace("title",
                        createStringResource("ObjectPolicyDialog.label.oidBound.help")));
                                textWrapper.add(oidBound);
                oidBound.add(new PropertyConstraintValidator(item.getModelObject()));
                oidBound.add(new EmptyOnChangeAjaxFormUpdatingBehavior());
                FeedbackPanel feedback = new FeedbackPanel(ID_FEEDBACK, new ComponentFeedbackMessageFilter(oidBound));
                textWrapper.add(feedback);

                WebMarkupContainer buttonGroup = new WebMarkupContainer(ID_BUTTON_GROUP);
                buttonGroup.add(AttributeAppender.append("class", new IModel<String>() {

                    private static final long serialVersionUID = 1L;
                    @Override
                    public String getObject() {
                        if (item.getIndex() > 0) {
                            return CLASS_MULTI_VALUE;
                        }

                        return null;
                    }
                }));
                item.add(buttonGroup);
                initButtons(buttonGroup, item);
            }

        };
        form.add(repeater);

        AjaxSubmitButton cancel = new AjaxSubmitButton(ID_BUTTON_CANCEL,
                createStringResource("ObjectPolicyDialog.button.cancel")) {

            private static final long serialVersionUID = 1L;
            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                cancelPerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                cancelPerformed(target);
            }
        };
        form.add(cancel);

        AjaxSubmitButton save = new AjaxSubmitButton(ID_BUTTON_SAVE,
                createStringResource("ObjectPolicyDialog.button.save")) {

            private static final long serialVersionUID = 1L;
            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                savePerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(getPageBase().getFeedbackPanel());
                target.add(form);
            }
        };
        form.add(save);
    }

    private void initButtons(WebMarkupContainer buttonGroup, final ListItem<ItemConstraintType> item) {
        AjaxLink<Void> add = new AjaxLink<Void>(ID_BUTTON_ADD) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                addPerformed(target);
            }
        };

        add.add(new VisibleEnableBehaviour() {

            private static final long serialVersionUID = 1L;
            @Override
            public boolean isVisible() {
                return isAddButtonVisible(item);
            }
        });
        buttonGroup.add(add);

        AjaxLink<Void> remove = new AjaxLink<Void>(ID_BUTTON_REMOVE) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                removePerformed(target, item);
            }
        };
        remove.add(new VisibleEnableBehaviour() {

            private static final long serialVersionUID = 1L;
            @Override
            public boolean isVisible() {
                return isRemoveButtonVisible();
            }
        });
        buttonGroup.add(remove);
    }

    private void addPerformed(AjaxRequestTarget target) {
        List<ItemConstraintType> list = model.getObject().getPropertyConstraintsList();
        list.add(new ItemConstraintType());
    }

    private void removePerformed(AjaxRequestTarget target, ListItem<ItemConstraintType> item) {
        List<ItemConstraintType> list = model.getObject().getPropertyConstraintsList();
        Iterator<ItemConstraintType> iterator = list.iterator();

        while (iterator.hasNext()) {
            ItemConstraintType object = iterator.next();

            if (object.equals(item.getModelObject())) {
                iterator.remove();
                break;
            }
        }

        if (list.size() == 0) {
            list.add(new ItemConstraintType());
        }
    }

    protected boolean isAddButtonVisible(ListItem<ItemConstraintType> item) {
        int size = model.getObject().getPropertyConstraintsList().size();
        if (size <= 1) {
            return true;
        }
        if (item.getIndex() == size - 1) {
            return true;
        }

        return false;
    }

    protected boolean isRemoveButtonVisible() {
        int size = model.getObject().getPropertyConstraintsList().size();
        if (size > 0) {
            return true;
        }

        return false;
    }

    protected IModel<List<ObjectTemplateConfigTypeReferenceDto>> createObjectTemplateList() {
        return new IModel<List<ObjectTemplateConfigTypeReferenceDto>>() {

            private static final long serialVersionUID = 1L;
            @Override
            public List<ObjectTemplateConfigTypeReferenceDto> getObject() {
                List<PrismObject<ObjectTemplateType>> templateList = null;
                List<ObjectTemplateConfigTypeReferenceDto> list = new ArrayList<>();
                OperationResult result = new OperationResult(OPERATION_LOAD_ALL_OBJECT_TEMPLATES);
                Task task = getPageBase().createSimpleTask(OPERATION_LOAD_ALL_OBJECT_TEMPLATES);

                try {
                    templateList = getPageBase().getModelService().searchObjects(ObjectTemplateType.class,
                            null, null, task, result);
                    result.recomputeStatus();
                } catch (Exception e) {
                    result.recordFatalError(getString("ObjectPolicyPanel.message.createObjectTemplateList.fatalError"), e);
                    LoggingUtils.logUnexpectedException(LOGGER, "Could not get list of object templates", e);
                    // TODO - show this error in GUI
                }

                if (templateList != null) {
                    ObjectTemplateType template;
                    for (PrismObject<ObjectTemplateType> obj : templateList) {
                        template = obj.asObjectable();
                        list.add(new ObjectTemplateConfigTypeReferenceDto(template.getOid(),
                                WebComponentUtil.getName(template)));
                    }
                }
                return list;
            }
        };
    }

    // TODO - to what types can be ObjectTemplate bound?
    private IModel<List<QName>> createTypeChoiceList() {
        return new IModel<List<QName>>() {

            private static final long serialVersionUID = 1L;
            @Override
            public List<QName> getObject() {
                return WebComponentUtil.createFocusTypeList();
            }
        };
    }

    private static class PropertyConstraintValidator implements IValidator<Boolean> {

        private static final long serialVersionUID = 1L;

        private ItemConstraintType itemConstraintType;

        private PropertyConstraintValidator(ItemConstraintType propertyConstraint) {
            this.itemConstraintType = propertyConstraint;

        }

        @Override
        public void validate(IValidatable<Boolean> validatable) {

            if (itemConstraintType == null) {
                return;
            }

            if (BooleanUtils.isTrue(validatable.getValue()) && (itemConstraintType == null || itemConstraintType.getPath() == null)) {
                ValidationError err = new ValidationError();
                err.addKey("propertyConstraintValidator.error");
                validatable.error(err);
            }

        }
    }


    private void cancelPerformed(AjaxRequestTarget target) {
        getPageBase().hideMainPopup(target);
    }

    protected void savePerformed(AjaxRequestTarget target) {
        target.add(getPageBase().getFeedbackPanel());
        getPageBase().hideMainPopup(target);
    }

    public IModel<ObjectPolicyDialogDto> getModel() {
        return model;
    }

    @Override
    public int getWidth() {
        return 625;
    }

    @Override
    public int getHeight() {
        return 400;
    }

    @Override
    public String getWidthUnit(){
        return "px";
    }

    @Override
    public String getHeightUnit(){
        return "px";
    }

    @Override
    public StringResourceModel getTitle() {
        return createStringResource("ObjectPolicyDialog.label");
    }

    @Override
    public Component getComponent() {
        return this;
    }
}
