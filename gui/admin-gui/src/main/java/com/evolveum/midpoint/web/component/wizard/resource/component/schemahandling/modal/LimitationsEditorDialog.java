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

import com.evolveum.midpoint.web.component.form.CheckFormGroup;
import com.evolveum.midpoint.web.component.form.TextFormGroup;
import com.evolveum.midpoint.web.component.input.ThreeStateBooleanPanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.wizard.resource.dto.PropertyLimitationsTypeDto;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PropertyAccessType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PropertyLimitationsType;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

import java.util.ArrayList;
import java.util.List;

/**
 *  @author shood
 * */
public class LimitationsEditorDialog extends ModalWindow{

    private static enum ChangeState{
        SKIP, FIRST, LAST
    }

    private static final String ID_REPEATER = "repeater";
    private static final String ID_LIMITATIONS_LINK = "limitationsLink";
    private static final String ID_LIMITATIONS_LABEL = "limitationsLinkName";
    private static final String ID_LIMITATION_DELETE = "deleteLimitation";
    private static final String ID_BODY = "accountBodyContainer";
    private static final String ID_LAYER_SCHEMA = "layerSchema";
    private static final String ID_LAYER_MODEL = "layerModel";
    private static final String ID_LAYER_PRESENTATION = "layerPresentation";
    private static final String ID_ACCESS_ADD = "addAccess";
    private static final String ID_ACCESS_READ = "readAccess";
    private static final String ID_ACCESS_MODIFY = "modifyAccess";
    private static final String ID_MIN_OCCURS = "minOccurs";
    private static final String ID_MAX_OCCURS = "maxOccurs";
    private static final String ID_IGNORE = "ignore";
    private static final String ID_BUTTON_ADD = "addButton";
    private static final String ID_BUTTON_SAVE = "saveButton";
    private static final String ID_BUTTON_CANCEL = "cancelButton";
    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_T_LAYERS = "layersTooltip";
    private static final String ID_T_PROPERTY = "propertyAccessTooltip";

    private static final String ID_LABEL_SIZE = "col-md-4";
    private static final String ID_INPUT_SIZE = "col-md-8";

    private ChangeState changeState = ChangeState.FIRST;
    private boolean initialized;
    private IModel<List<PropertyLimitationsTypeDto>> model;
    private IModel<List<PropertyLimitationsType>> inputModel;

    public LimitationsEditorDialog(String id, final IModel<List<PropertyLimitationsType>> limitation){
        super(id);

        inputModel = limitation;
        model = new LoadableModel<List<PropertyLimitationsTypeDto>>(false) {

            @Override
            protected List<PropertyLimitationsTypeDto> load() {
                return loadLimitationsModel(limitation);
            }
        };

        setOutputMarkupId(true);
        setTitle(createStringResource("LimitationsEditorDialog.label"));
        showUnloadConfirmation(false);
        setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        setCookieName(LimitationsEditorDialog.class.getSimpleName() + ((int) (Math.random() * 100)));
        setInitialWidth(600);
        setInitialHeight(700);
        setWidthUnit("px");

        WebMarkupContainer content = new WebMarkupContainer(getContentId());
        content.setOutputMarkupId(true);
        setContent(content);
    }

    private List<PropertyLimitationsTypeDto> loadLimitationsModel(IModel<List<PropertyLimitationsType>> limList){
        List<PropertyLimitationsTypeDto> limitations = new ArrayList<>();
        List<PropertyLimitationsType> limitationTypeList = limList.getObject();

        for(PropertyLimitationsType limitation: limitationTypeList){
            limitations.add(new PropertyLimitationsTypeDto(limitation));
        }

        return limitations;
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

        ListView repeater = new ListView<PropertyLimitationsTypeDto>(ID_REPEATER, model){

            @Override
            protected void populateItem(final ListItem<PropertyLimitationsTypeDto> item){
                WebMarkupContainer linkContainer = new WebMarkupContainer(ID_LIMITATIONS_LINK);
                linkContainer.setOutputMarkupId(true);
                linkContainer.add(new AttributeModifier("href", createCollapseItemId(item, true)));
                item.add(linkContainer);

                Label linkLabel = new Label(ID_LIMITATIONS_LABEL, createLimitationsLabelModel(item));
                linkContainer.add(linkLabel);

                AjaxLink delete = new AjaxLink(ID_LIMITATION_DELETE) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        deleteLimitationPerformed(target, item);
                    }
                };
                linkContainer.add(delete);

                WebMarkupContainer limitationBody = new WebMarkupContainer(ID_BODY);
                limitationBody.setOutputMarkupId(true);
                limitationBody.setMarkupId(createCollapseItemId(item, false).getObject());
                if(changeState != ChangeState.SKIP){
                    limitationBody.add(new AttributeModifier("class", new AbstractReadOnlyModel<String>() {

                        @Override
                        public String getObject() {
                            if(changeState == ChangeState.FIRST && item.getIndex() == 0){
                                return "panel-collapse collapse in";
                            } else if(changeState == ChangeState.LAST && item.getIndex() == (getModelObject().size()-1)){
                                return "panel-collapse collapse in";
                            } else {
                                return "panel-collapse collapse";
                            }
                        }
                    }));
                }
                item.add(limitationBody);
                initLimitationBody(limitationBody, item);

            }
        };
        repeater.setOutputMarkupId(true);
        form.add(repeater);

        initButtons(form);
    }

    private void initButtons(Form form){
        AjaxLink add = new AjaxLink(ID_BUTTON_ADD) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                addLimitationsPerformed(target);
            }
        };
        form.add(add);

        AjaxLink cancel = new AjaxLink(ID_BUTTON_CANCEL) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                cancelPerformed(target);
            }
        };
        form.add(cancel);

        AjaxLink save = new AjaxLink(ID_BUTTON_SAVE) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                savePerformed(target);
            }
        };
        form.add(save);
    }

    private void initLimitationBody(final WebMarkupContainer body, ListItem<PropertyLimitationsTypeDto> item){
        CheckFormGroup schema = new CheckFormGroup(ID_LAYER_SCHEMA, new PropertyModel<Boolean>(item.getModelObject(), PropertyLimitationsTypeDto.F_SCHEMA),
                createStringResource("LimitationsEditorDialog.label.schema"), ID_LABEL_SIZE, ID_INPUT_SIZE);
        schema.getCheck().add(prepareAjaxOnComponentTagUpdateBehavior());
        body.add(schema);

        CheckFormGroup model = new CheckFormGroup(ID_LAYER_MODEL, new PropertyModel<Boolean>(item.getModelObject(), PropertyLimitationsTypeDto.F_MODEL),
                createStringResource("LimitationsEditorDialog.label.model"), ID_LABEL_SIZE, ID_INPUT_SIZE);
        model.getCheck().add(prepareAjaxOnComponentTagUpdateBehavior());
        body.add(model);

        CheckFormGroup presentation = new CheckFormGroup(ID_LAYER_PRESENTATION, new PropertyModel<Boolean>(item.getModelObject(), PropertyLimitationsTypeDto.F_PRESENTATION),
                createStringResource("LimitationsEditorDialog.label.presentation"), ID_LABEL_SIZE, ID_INPUT_SIZE);
        presentation.getCheck().add(prepareAjaxOnComponentTagUpdateBehavior());
        body.add(presentation);

        ThreeStateBooleanPanel add = new ThreeStateBooleanPanel(ID_ACCESS_ADD, new PropertyModel<Boolean>(item.getModelObject(), PropertyLimitationsTypeDto.F_LIMITATION + ".access.add"),
                "LimitationsEditorDialog.allow", "LimitationsEditorDialog.inherit", "LimitationsEditorDialog.deny", null);
        body.add(add);

        ThreeStateBooleanPanel read = new ThreeStateBooleanPanel(ID_ACCESS_READ, new PropertyModel<Boolean>(item.getModelObject(), PropertyLimitationsTypeDto.F_LIMITATION + ".access.read"),
                "LimitationsEditorDialog.allow", "LimitationsEditorDialog.inherit", "LimitationsEditorDialog.deny", null);
        body.add(read);

        ThreeStateBooleanPanel modify = new ThreeStateBooleanPanel(ID_ACCESS_MODIFY, new PropertyModel<Boolean>(item.getModelObject(), PropertyLimitationsTypeDto.F_LIMITATION + ".access.modify"),
                "LimitationsEditorDialog.allow", "LimitationsEditorDialog.inherit", "LimitationsEditorDialog.deny", null);
        body.add(modify);

        TextFormGroup minOccurs = new TextFormGroup(ID_MIN_OCCURS, new PropertyModel<String>(item.getModelObject(), PropertyLimitationsTypeDto.F_LIMITATION + ".minOccurs"),
                createStringResource("LimitationsEditorDialog.label.minOccurs"), "SchemaHandlingStep.limitations.tooltip.minOccurs", true, ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        minOccurs.getField().add(prepareAjaxOnComponentTagUpdateBehavior());
        body.add(minOccurs);

        TextFormGroup maxOccurs = new TextFormGroup(ID_MAX_OCCURS, new PropertyModel<String>(item.getModelObject(), PropertyLimitationsTypeDto.F_LIMITATION + ".maxOccurs"),
                createStringResource("LimitationsEditorDialog.label.maxOccurs"), "SchemaHandlingStep.limitations.tooltip.maxOccurs", true, ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        maxOccurs.getField().add(prepareAjaxOnComponentTagUpdateBehavior());
        body.add(maxOccurs);

        CheckFormGroup ignore = new CheckFormGroup(ID_IGNORE, new PropertyModel<Boolean>(item.getModelObject(), PropertyLimitationsTypeDto.F_LIMITATION + ".ignore"),
                createStringResource("LimitationsEditorDialog.label.ignore"), "SchemaHandlingStep.limitations.tooltip.ignore", true, ID_LABEL_SIZE, ID_INPUT_SIZE);
        ignore.getCheck().add(prepareAjaxOnComponentTagUpdateBehavior());
        body.add(ignore);

        Label layersTooltip = new Label(ID_T_LAYERS);
        layersTooltip.add(new InfoTooltipBehavior(true){

            @Override
            public String getModalContainer(Component component) {
                return body.getMarkupId();
            }
        });
        body.add(layersTooltip);

        Label propertyTooltip = new Label(ID_T_PROPERTY);
        propertyTooltip.add(new InfoTooltipBehavior(true){

            @Override
            public String getModalContainer(Component component) {
                return body.getMarkupId();
            }
        });
        body.add(propertyTooltip);
    }

    private AjaxFormComponentUpdatingBehavior prepareAjaxOnComponentTagUpdateBehavior(){
        return new AjaxFormComponentUpdatingBehavior("onBlur") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {}
        };
    }

    private IModel<String> createLimitationsLabelModel(final ListItem<PropertyLimitationsTypeDto> item){
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                StringBuilder sb = new StringBuilder();
                PropertyLimitationsTypeDto dto = item.getModelObject();
                sb.append("#").append(item.getIndex()+1).append(" - ");

                if(dto.isModel()){
                    sb.append(LayerType.MODEL).append(", ");
                }
                if(dto.isPresentation()){
                    sb.append(LayerType.PRESENTATION).append(", ");
                }
                if(dto.isSchema()){
                    sb.append(LayerType.SCHEMA).append(", ");
                }
                sb.append(":");

                if(dto.getLimitationObject().getAccess() != null){
                    PropertyAccessType access = dto.getLimitationObject().getAccess();

                    if(access.isRead() != null && access.isRead()){
                        sb.append(getString("LimitationsEditorDialog.label.read")).append(", ");
                    }
                    if(access.isAdd() != null && access.isAdd()){
                        sb.append(getString("LimitationsEditorDialog.label.add")).append(", ");
                    }
                    if(access.isModify() != null && access.isModify()){
                        sb.append(getString("LimitationsEditorDialog.label.modify")).append(", ");
                    }
                }

                return sb.toString();
            }
        };
    }

    private IModel<String> createCollapseItemId(final ListItem<PropertyLimitationsTypeDto> item, final boolean appendSelector){
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                StringBuilder sb = new StringBuilder();

                if(appendSelector){
                    sb.append("#");
                }

                sb.append("collapse").append(item.getId());

                return sb.toString();
            }
        };
    }

    public StringResourceModel createStringResource(String resourceKey, Object... objects) {
        return new StringResourceModel(resourceKey, this, null, resourceKey, objects);
    }

    private void addLimitationsPerformed(AjaxRequestTarget target){
        changeState = ChangeState.LAST;
        model.getObject().add(new PropertyLimitationsTypeDto(new PropertyLimitationsType()));
        target.add(getContent());
    }

    private void deleteLimitationPerformed(AjaxRequestTarget target, ListItem<PropertyLimitationsTypeDto> item){
        changeState = ChangeState.SKIP;
        model.getObject().remove(item.getModelObject());
        target.add(getContent());
    }

    private void cancelPerformed(AjaxRequestTarget target){
        close(target);
    }

    protected void savePerformed(AjaxRequestTarget target){
        List<PropertyLimitationsTypeDto> list = model.getObject();
        List<PropertyLimitationsType> outputList = new ArrayList<>();

        for(PropertyLimitationsTypeDto dto: list){
            outputList.add(dto.prepareDtoForSave());
        }

        inputModel.setObject(outputList);
        close(target);
    }
}
