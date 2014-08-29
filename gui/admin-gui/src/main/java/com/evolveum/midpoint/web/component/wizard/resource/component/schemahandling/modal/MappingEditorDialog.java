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

package com.evolveum.midpoint.web.component.wizard.resource.component.schemahandling.modal;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.form.CheckFormGroup;
import com.evolveum.midpoint.web.component.form.DropDownFormGroup;
import com.evolveum.midpoint.web.component.form.TextAreaFormGroup;
import com.evolveum.midpoint.web.component.form.TextFormGroup;
import com.evolveum.midpoint.web.component.form.multivalue.MultiValueTextPanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.wizard.resource.dto.MappingTypeDto;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingStrengthType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

import java.util.List;

/**
 *  @author shood
 * */
public class MappingEditorDialog extends ModalWindow{

    private static final Trace LOGGER = TraceManager.getTrace(MappingEditorDialog.class);

    private static final String DOT_CLASS = MappingEditorDialog.class.getName() + ".";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_NAME = "name";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_AUTHORITATIVE = "authoritative";
    private static final String ID_EXCLUSIVE = "exclusive";
    private static final String ID_STRENGTH = "strength";
    private static final String ID_CHANNEL = "channel";
    private static final String ID_EXCEPT_CHANNEL = "exceptChannel";
    private static final String ID_SOURCE = "source";
    private static final String ID_TARGET = "target";
    private static final String ID_EXPRESSION_TYPE = "expressionType";
    private static final String ID_EXPRESSION = "expression";
    private static final String ID_CONDITION_TYPE = "conditionType";
    private static final String ID_CONDITION = "condition";
    private static final String ID_TIME_FROM = "timeFrom";
    private static final String ID_TIME_TO = "timeTo";
    private static final String ID_BUTTON_SAVE = "saveButton";
    private static final String ID_BUTTON_CANCEL = "cancelButton";

    private static final String ID_LABEL_SIZE = "col-md-4";
    private static final String ID_INPUT_SIZE = "col-md-8";

    private static final int CODE_ROW_COUNT = 4;

    private boolean initialized;
    private IModel<MappingTypeDto> model;

    public MappingEditorDialog(String id, final IModel<MappingType> mapping){
        super(id);

        model = new LoadableModel<MappingTypeDto>(false) {

            @Override
            protected MappingTypeDto load() {
                if(mapping != null){
                    return new MappingTypeDto(mapping.getObject());
                } else {
                    return new MappingTypeDto(new MappingType());
                }
            }
        };

        setOutputMarkupId(true);
        setTitle(createStringResource("MappingEditorDialog.label"));
        showUnloadConfirmation(false);
        setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        setCookieName(MappingEditorDialog.class.getSimpleName() + ((int) (Math.random() * 100)));
        setInitialWidth(700);
        setInitialHeight(700);
        setWidthUnit("px");

        WebMarkupContainer content = new WebMarkupContainer(getContentId());
        content.setOutputMarkupId(true);
        setContent(content);
    }

    public void updateModel(AjaxRequestTarget target, IModel<MappingType> mapping){
        model.setObject(new MappingTypeDto(mapping.getObject()));
        target.add(getContent());
    }

    public void updateModel(AjaxRequestTarget target, MappingType mapping){
        model.setObject(new MappingTypeDto(mapping));
        target.add(getContent());
    }

    public StringResourceModel createStringResource(String resourceKey, Object... objects) {
        return new StringResourceModel(resourceKey, this, null, resourceKey, objects);
    }

    @Override
    protected void onBeforeRender(){
        super.onBeforeRender();

        if(initialized){
            return;
        }

        initLayout((WebMarkupContainer) get(getContentId()));
        initialized = true;
    }

    public void initLayout(WebMarkupContainer content){
        Form form = new Form(ID_MAIN_FORM);
        form.setOutputMarkupId(true);
        content.add(form);

        TextFormGroup name = new TextFormGroup(ID_NAME, new PropertyModel<String>(model, MappingTypeDto.F_MAPPING + ".name"),
                createStringResource("MappingEditorDialog.label.name"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        form.add(name);

        TextAreaFormGroup description = new TextAreaFormGroup(ID_DESCRIPTION, new PropertyModel<String>(model, MappingTypeDto.F_MAPPING + ".description"),
                createStringResource("MappingEditorDialog.label.description"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        form.add(description);

        CheckFormGroup authoritative = new CheckFormGroup(ID_AUTHORITATIVE, new PropertyModel<Boolean>(model, MappingTypeDto.F_MAPPING + ".authoritative"),
                createStringResource("MappingEditorDialog.label.authoritative"), ID_LABEL_SIZE, ID_INPUT_SIZE);
        form.add(authoritative);

        CheckFormGroup exclusive = new CheckFormGroup(ID_EXCLUSIVE, new PropertyModel<Boolean>(model, MappingTypeDto.F_MAPPING + ".exclusive"),
                createStringResource("MappingEditorDialog.label.exclusive"), ID_LABEL_SIZE, ID_INPUT_SIZE);
        form.add(exclusive);

        DropDownFormGroup strength = new DropDownFormGroup<>(ID_STRENGTH,
                new PropertyModel<MappingStrengthType>(model, MappingTypeDto.F_MAPPING + ".strength"),
                WebMiscUtil.createReadonlyModelFromEnum(MappingStrengthType.class),
                new EnumChoiceRenderer<MappingStrengthType>(this), createStringResource("MappingEditorDialog.label.strength"),
                ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        form.add(strength);

        //TODO - shouldn't this list be also AutoCompleteTextField from some static list with channels?
        MultiValueTextPanel channel = new MultiValueTextPanel<>(ID_CHANNEL,
                new PropertyModel<List<String>>(model, MappingTypeDto.F_MAPPING + ".channel"));
        form.add(channel);

        //TODO - shouldn't this list be also AutoCompleteTextField from some static list with channels?
        MultiValueTextPanel exceptChannel = new MultiValueTextPanel<>(ID_EXCEPT_CHANNEL,
                new PropertyModel<List<String>>(model, MappingTypeDto.F_MAPPING + ".exceptChannel"));
        form.add(exceptChannel);

        //TODO - create some nice ItemPathType editor in near future
        MultiValueTextPanel source = new MultiValueTextPanel<>(ID_SOURCE,
                new PropertyModel<List<String>>(model, MappingTypeDto.F_SOURCE));
        form.add(source);

        TextFormGroup target = new TextFormGroup(ID_TARGET, new PropertyModel<String>(model, MappingTypeDto.F_TARGET),
                createStringResource("MappingEditorDialog.label.target"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        form.add(target);

        DropDownFormGroup expressionType = new DropDownFormGroup<>(ID_EXPRESSION_TYPE,
                new PropertyModel<MappingTypeDto.ExpressionEvaluatorType>(model, MappingTypeDto.F_EXPRESSION_TYPE),
                WebMiscUtil.createReadonlyModelFromEnum(MappingTypeDto.ExpressionEvaluatorType.class),
                new EnumChoiceRenderer<MappingTypeDto.ExpressionEvaluatorType>(this),
                createStringResource("MappingEditorDialog.label.expressionType"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        form.add(expressionType);

        //TODO - add some enableDisable and visibility behavior based on status of expressionType component
        TextAreaFormGroup expression = new TextAreaFormGroup(ID_EXPRESSION, new PropertyModel<String>(model, MappingTypeDto.F_EXPRESSION),
                createStringResource("MappingEditorDialog.label.expression"), ID_LABEL_SIZE, ID_INPUT_SIZE, false, CODE_ROW_COUNT);
        form.add(expression);

        DropDownFormGroup conditionType = new DropDownFormGroup<>(ID_CONDITION_TYPE,
                new PropertyModel<MappingTypeDto.ExpressionEvaluatorType>(model, MappingTypeDto.F_CONDITION_TYPE),
                WebMiscUtil.createReadonlyModelFromEnum(MappingTypeDto.ExpressionEvaluatorType.class),
                new EnumChoiceRenderer<MappingTypeDto.ExpressionEvaluatorType>(this),
                createStringResource("MappingEditorDialog.label.conditionType"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        form.add(conditionType);

        //TODO - add some enableDisable and visibility behavior based on status of conditionType component
        TextAreaFormGroup condition = new TextAreaFormGroup(ID_CONDITION, new PropertyModel<String>(model, MappingTypeDto.F_CONDITION),
                createStringResource("MappingEditorDialog.label.condition"), ID_LABEL_SIZE, ID_INPUT_SIZE, false, CODE_ROW_COUNT);
        form.add(condition);

        AjaxSubmitButton cancel = new AjaxSubmitButton(ID_BUTTON_CANCEL,
                createStringResource("MappingEditorDialog.button.cancel")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                cancelPerformed(target);
            }
        };
        form.add(cancel);

        AjaxSubmitButton save = new AjaxSubmitButton(ID_BUTTON_SAVE,
                createStringResource("MappingEditorDialog.button.save")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                savePerformed(target);
            }
        };
        form.add(save);
    }

    private void cancelPerformed(AjaxRequestTarget target){
        close(target);
    }

    private void savePerformed(AjaxRequestTarget target){
        //TODO - implement
    }
}
