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

package com.evolveum.midpoint.web.component.wizard.resource.component.synchronization;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.form.DropDownFormGroup;
import com.evolveum.midpoint.web.component.form.TextAreaFormGroup;
import com.evolveum.midpoint.web.component.form.TextFormGroup;
import com.evolveum.midpoint.web.component.wizard.resource.dto.SynchronizationActionTypeDto;
import com.evolveum.midpoint.web.page.admin.resources.PageResourceWizard;
import com.evolveum.midpoint.xml.ns._public.common.common_3.BeforeAfterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationActionType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

import java.util.List;

/**
 *  @author shood
 * */
public class SynchronizationActionEditorDialog extends ModalWindow{

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_NAME = "name";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_HANDLER_URI = "handlerUri";
    private static final String ID_ORDER = "order";
    private static final String ID_BUTTON_SAVE = "saveButton";
    private static final String ID_BUTTON_CANCEL = "cancelButton";

    private static final String ID_LABEL_SIZE = "col-md-4";
    private static final String ID_INPUT_SIZE = "col-md-8";

    private IModel<SynchronizationActionTypeDto> model;
    private IModel<SynchronizationActionType> inputModel;
    private boolean initialized;

    public SynchronizationActionEditorDialog(String id, final IModel<SynchronizationActionType> action){
        super(id);

        inputModel = action;
        model = new LoadableModel<SynchronizationActionTypeDto>(false) {

            @Override
            protected SynchronizationActionTypeDto load() {
                if(action != null){
                    return new SynchronizationActionTypeDto(action.getObject());
                } else {
                    return new SynchronizationActionTypeDto(null);
                }
            }
        };

        setOutputMarkupId(true);
        setTitle(createStringResource("SynchronizationActionEditorDialog.label"));
        showUnloadConfirmation(false);
        setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        setCookieName(SynchronizationActionEditorDialog.class.getSimpleName() + ((int) (Math.random() * 100)));
        setInitialWidth(600);
        setInitialHeight(400);
        setWidthUnit("px");

        WebMarkupContainer content = new WebMarkupContainer(getContentId());
        content.setOutputMarkupId(true);
        setContent(content);
    }

    public void updateModel(AjaxRequestTarget target, SynchronizationActionType action){
        model.setObject(new SynchronizationActionTypeDto(action));
        inputModel = new Model<>(action);
        target.add(getContent());
    }

    public StringResourceModel createStringResource(String resourceKey, Object... objects) {
    	return PageBase.createStringResourceStatic(this, resourceKey, objects);
//        return new StringResourceModel(resourceKey, this, null, resourceKey, objects);
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

    private void initLayout(WebMarkupContainer content){
        Form form = new com.evolveum.midpoint.web.component.form.Form(ID_MAIN_FORM);
        form.setOutputMarkupId(true);
        content.add(form);

        TextFormGroup name = new TextFormGroup(ID_NAME, new PropertyModel<String>(model, SynchronizationActionTypeDto.F_ACTION_OBJECT + ".name"),
                createStringResource("SynchronizationActionEditorDialog.label.name"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        form.add(name);

        TextAreaFormGroup description = new TextAreaFormGroup(ID_DESCRIPTION, new PropertyModel<String>(model, SynchronizationActionTypeDto.F_ACTION_OBJECT + ".description"),
                createStringResource("SynchronizationActionEditorDialog.label.description"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        form.add(description);

        DropDownFormGroup<SynchronizationActionTypeDto.HandlerUriActions> handlerUri = new DropDownFormGroup<SynchronizationActionTypeDto.HandlerUriActions>(ID_HANDLER_URI,
                new PropertyModel<SynchronizationActionTypeDto.HandlerUriActions>(model, SynchronizationActionTypeDto.F_HANDLER_URI),
                WebComponentUtil.createReadonlyModelFromEnum(SynchronizationActionTypeDto.HandlerUriActions.class),
                new EnumChoiceRenderer<SynchronizationActionTypeDto.HandlerUriActions>(this), createStringResource("SynchronizationActionEditorDialog.label.handlerUri"),
                "SynchronizationStep.action.tooltip.handlerUri", true, ID_LABEL_SIZE, ID_INPUT_SIZE, false){

            @Override
            protected DropDownChoice createDropDown(String id, IModel<List<SynchronizationActionTypeDto.HandlerUriActions>> choices,
                                                    IChoiceRenderer<SynchronizationActionTypeDto.HandlerUriActions> renderer, boolean required){
                DropDownChoice choice = new DropDownChoice<>(id, getModel(), choices, renderer);
                choice.setNullValid(true);

                return choice;
            }
        };
        form.add(handlerUri);

        DropDownFormGroup<BeforeAfterType> order = new DropDownFormGroup<BeforeAfterType>(ID_ORDER, new PropertyModel<BeforeAfterType>(model, SynchronizationActionTypeDto.F_ACTION_OBJECT + ".order"),
                WebComponentUtil.createReadonlyModelFromEnum(BeforeAfterType.class), new EnumChoiceRenderer<BeforeAfterType>(this),
                createStringResource("SynchronizationActionEditorDialog.label.order"), "SynchronizationStep.action.tooltip.order", true, ID_LABEL_SIZE, ID_INPUT_SIZE, false){

            @Override
            protected DropDownChoice createDropDown(String id, IModel<List<BeforeAfterType>> choices, IChoiceRenderer<BeforeAfterType> renderer, boolean required){
                DropDownChoice choice = new DropDownChoice<>(id, getModel(), choices, renderer);
                choice.setNullValid(true);

                return choice;
            }
        };
        form.add(order);
        initButtons(form);
    }

    private void initButtons(Form form){
        AjaxLink cancel = new AjaxLink(ID_BUTTON_CANCEL) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                cancelPerformed(target);
            }
        };
        form.add(cancel);

        AjaxSubmitLink save = new AjaxSubmitLink(ID_BUTTON_SAVE) {

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

    protected void savePerformed(AjaxRequestTarget target){
        if(inputModel != null){
            inputModel.setObject(model.getObject().prepareDtoToSave());
        } else {
            model.getObject().prepareDtoToSave();
            inputModel = new PropertyModel<>(model, SynchronizationActionTypeDto.F_ACTION_OBJECT);
        }

		((PageResourceWizard) getPage()).refreshIssues(target);
		updateComponents(target);
        close(target);
    }

    public void updateComponents(AjaxRequestTarget target){
        //Override this if update of component(s) holding this modal window is needed
    }
}
