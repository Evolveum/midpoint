/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.home.component;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.page.admin.home.dto.PasswordQuestionsDto;
import com.evolveum.midpoint.web.page.admin.home.dto.SecurityQuestionAnswerDTO;

public class MyPasswordQuestionsPanel extends InputPanel {

    public static final String F_QUESTION = "questionTF";
    public static final String F_ANSWER = "answerTF";
    private static final String ID_QA_PANEL = "questionAnswerPanel";
    PasswordQuestionsDto mod = new PasswordQuestionsDto();

    public MyPasswordQuestionsPanel(String id, SecurityQuestionAnswerDTO model) {
        super(ID_QA_PANEL);

        mod.setPwdQuestion(model.getQuestionItself());

        mod.setPwdAnswer(model.getPwdAnswer());

        initLayout();
    }

    public void initLayout() {
        // final Label question = new Label (F_QUESTION, mod.getPwdQuestion());
        final Label question = new Label(F_QUESTION, new PropertyModel<PasswordQuestionsDto>(mod,
                PasswordQuestionsDto.F_MY_QUESTIONS_QUESTIONITSELF));
        question.setOutputMarkupId(true);
        add(question);

        final TextField<String> answer = new TextField<String>(F_ANSWER, new PropertyModel(mod,
                SecurityQuestionAnswerDTO.F_PASSWORD_QUESTION_ANSWER));
        answer.setRequired(true);
        answer.setOutputMarkupId(true);
        add(answer);

    }

    @Override
    public List<FormComponent> getFormComponents() {
        List<FormComponent> list = new ArrayList<>();
        list.add((FormComponent) get(F_QUESTION));
        list.add((FormComponent) get(F_ANSWER));

        return list;
    }

    @Override
    public FormComponent getBaseFormComponent() {
        return (FormComponent) get(F_QUESTION);
    }

}
