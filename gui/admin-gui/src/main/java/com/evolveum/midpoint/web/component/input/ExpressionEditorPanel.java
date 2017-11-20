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

package com.evolveum.midpoint.web.component.input;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.web.component.input.dto.ExpressionTypeDto;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.resources.PageResourceWizard;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  @author shood
 * */
public class ExpressionEditorPanel extends BasePanel<ExpressionType> {

    private static final Trace LOGGER = TraceManager.getTrace(ExpressionEditorPanel.class);

    private static final String DOT_CLASS = ExpressionEditorPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_PASSWORD_POLICIES = DOT_CLASS + "createPasswordPolicyList";

    private static final String ID_DESCRIPTION = "description";
    private static final String ID_TYPE = "type";
    private static final String ID_LANGUAGE = "language";
    private static final String ID_POLICY_REF = "policyRef";
    private static final String ID_EXPRESSION = "expression";
    private static final String ID_LANGUAGE_CONTAINER = "languageContainer";
    private static final String ID_POLICY_CONTAINER = "policyRefContainer";
    private static final String ID_BUTTON_UPDATE = "update";
    private static final String ID_LABEL_DESCRIPTION = "descriptionLabel";
    private static final String ID_LABEL_TYPE = "typeLabel";
    private static final String ID_LABEL_EXPRESSION = "expressionLabel";
    private static final String ID_LABEL_UPDATE = "updateLabel";
    private static final String ID_T_TYPE = "typeTooltip";
    private static final String ID_T_LANGUAGE = "languageTooltip";
    private static final String ID_T_POLICY = "policyRefTooltip";
    private static final String ID_T_EXPRESSION = "expressionTooltip";

    private IModel<ExpressionTypeDto> dtoModel;
    private Map<String, String> policyMap = new HashMap<>();

    public ExpressionEditorPanel(String id, IModel<ExpressionType> model, PageBase parentPage) {
        super(id, model);
		initLayout(parentPage);
    }

    protected IModel<ExpressionTypeDto> getExpressionDtoModel(){
        return dtoModel;
    }

    private void loadDtoModel() {
        if (dtoModel == null) {
            dtoModel = new LoadableModel<ExpressionTypeDto>(false) {
                @Override
                protected ExpressionTypeDto load() {
                    return new ExpressionTypeDto(getModel().getObject(), getPageBase().getPrismContext());
                }
            };
        }
    }

    protected void initLayout(PageBase parentPage) {
		setOutputMarkupId(true);

        loadDtoModel();

		Label descriptionLabel = new Label(ID_LABEL_DESCRIPTION, createStringResource(getDescriptionLabelKey()));
		add(descriptionLabel);

		TextArea description = new TextArea<>(ID_DESCRIPTION, new PropertyModel<String>(dtoModel, ExpressionTypeDto.F_DESCRIPTION));
		description.setOutputMarkupId(true);
		//parentPage.addEditingEnabledBehavior(description);
		add(description);

		Label typeLabel = new Label(ID_LABEL_TYPE, createStringResource(getTypeLabelKey()));
        add(typeLabel);

        DropDownChoice type = new DropDownChoice<>(ID_TYPE,
                new PropertyModel<ExpressionUtil.ExpressionEvaluatorType>(dtoModel, ExpressionTypeDto.F_TYPE),
                WebComponentUtil.createReadonlyModelFromEnum(ExpressionUtil.ExpressionEvaluatorType.class),
                new EnumChoiceRenderer<ExpressionUtil.ExpressionEvaluatorType>(this));
		//parentPage.addEditingEnabledBehavior(type);
		type.add(new AjaxFormComponentUpdatingBehavior("change") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                dtoModel.getObject().updateExpressionType();
                //target.add(get(ID_LANGUAGE_CONTAINER), get(ID_POLICY_CONTAINER), get(ID_EXPRESSION));
				target.add(ExpressionEditorPanel.this);				// because of ACE editor
            }
        });
        type.setOutputMarkupId(true);
        type.setOutputMarkupPlaceholderTag(true);
        type.setNullValid(true);
        add(type);

        WebMarkupContainer languageContainer = new WebMarkupContainer(ID_LANGUAGE_CONTAINER);
        languageContainer.setOutputMarkupId(true);
        languageContainer.setOutputMarkupPlaceholderTag(true);
        languageContainer.add(new VisibleEnableBehaviour(){

            @Override
            public boolean isVisible(){
                return ExpressionUtil.ExpressionEvaluatorType.SCRIPT.equals(dtoModel.getObject().getType());
            }
        });
		//parentPage.addEditingEnabledBehavior(languageContainer);
		add(languageContainer);

        DropDownChoice language = new DropDownChoice<>(ID_LANGUAGE,
                new PropertyModel<ExpressionUtil.Language>(dtoModel, ExpressionTypeDto.F_LANGUAGE),
                WebComponentUtil.createReadonlyModelFromEnum(ExpressionUtil.Language.class),
                new EnumChoiceRenderer<ExpressionUtil.Language>(this));
		//parentPage.addEditingEnabledBehavior(language);
		language.add(new AjaxFormComponentUpdatingBehavior("change") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                dtoModel.getObject().updateExpressionLanguage();
                //target.add(get(ID_LANGUAGE_CONTAINER), get(ID_POLICY_CONTAINER), get(ID_EXPRESSION));
				target.add(ExpressionEditorPanel.this);			// because of ACE editor
            }
        });
        language.setNullValid(false);
        languageContainer.add(language);

        WebMarkupContainer policyContainer = new WebMarkupContainer(ID_POLICY_CONTAINER);
        policyContainer.setOutputMarkupId(true);
        policyContainer.setOutputMarkupPlaceholderTag(true);
        policyContainer.add(new VisibleEnableBehaviour(){

            @Override
            public boolean isVisible(){
                return ExpressionUtil.ExpressionEvaluatorType.GENERATE.equals(dtoModel.getObject().getType());
            }
        });
        add(policyContainer);

        DropDownChoice policyRef = new DropDownChoice<>(ID_POLICY_REF,
                new PropertyModel<ObjectReferenceType>(dtoModel, ExpressionTypeDto.F_POLICY_REF),
                new AbstractReadOnlyModel<List<ObjectReferenceType>>() {

                    @Override
                    public List<ObjectReferenceType> getObject() {
                        return WebModelServiceUtils.createObjectReferenceList(ValuePolicyType.class, getPageBase(), policyMap);
                    }
                }, new ObjectReferenceChoiceRenderer(policyMap));
		//parentPage.addEditingEnabledBehavior(policyRef);
		policyRef.add(new AjaxFormComponentUpdatingBehavior("change") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                dtoModel.getObject().updateExpressionValuePolicyRef();
                target.add(get(ID_LANGUAGE_CONTAINER), get(ID_POLICY_CONTAINER), get(ID_EXPRESSION));
            }
        });
        policyRef.setNullValid(true);
        policyContainer.add(policyRef);

        Label expressionLabel = new Label(ID_LABEL_EXPRESSION, createStringResource(getExpressionLabelKey()));
        add(expressionLabel);

        AceEditor expression = new AceEditor(ID_EXPRESSION, new PropertyModel<String>(dtoModel, ExpressionTypeDto.F_EXPRESSION));
        expression.setOutputMarkupId(true);
		//parentPage.addEditingEnabledBehavior(expression);
		add(expression);

        AjaxSubmitLink update = new AjaxSubmitLink(ID_BUTTON_UPDATE) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                updateExpressionPerformed(target);
            }
        };
		Label updateLabel = new Label(ID_LABEL_UPDATE, createStringResource(getUpdateLabelKey()));
		updateLabel.setRenderBodyOnly(true);
		update.add(updateLabel);
        if (parentPage instanceof PageResourceWizard) {
            ((PageResourceWizard)parentPage).addEditingEnabledBehavior(this);
            ((PageResourceWizard)parentPage).addEditingVisibleBehavior(update);
        }
		add(update);

        add(WebComponentUtil.createHelp(ID_T_TYPE));
        languageContainer.add(WebComponentUtil.createHelp(ID_T_LANGUAGE));
        policyContainer.add(WebComponentUtil.createHelp(ID_T_POLICY));
        add(WebComponentUtil.createHelp(ID_T_EXPRESSION));
    }

    protected void updateExpressionPerformed(AjaxRequestTarget target){
        try {
            dtoModel.getObject().updateExpression(getPageBase().getPrismContext());

            success(getString("ExpressionEditorPanel.message.expressionSuccess"));
        } catch (Exception e){
            LoggingUtils.logUnexpectedException(LOGGER, "Could not create JAXBElement<?> from provided xml expression.", e);
            error(getString("ExpressionEditorPanel.message.cantSerialize", e));
        }

        performExpressionHook(target);
        target.add(getPageBase().getFeedbackPanel());
    }

    /**
     *  Override this in component with ExpressionEditorPanel to provide additional functionality when expression is updated
     * */
    public void performExpressionHook(AjaxRequestTarget target){}

    /**
     *  Provide key for expression type label
     * */
    public String getTypeLabelKey(){
        return "ExpressionEditorPanel.label.type";
    }

	public String getDescriptionLabelKey(){
		return "ExpressionEditorPanel.label.description";
	}

	/**
     *  Provide key for expression label
     * */
    public String getExpressionLabelKey(){
        return "ExpressionEditorPanel.label.expression";
    }

	public String getUpdateLabelKey() {
        return "ExpressionEditorPanel.button.expressionSave";
    }
}
