package com.evolveum.midpoint.web.component.input;

import com.evolveum.midpoint.web.component.assignment.SimpleRoleSelector;
import com.evolveum.midpoint.web.component.sample.SampleFormFocusTabPanel;
import com.evolveum.midpoint.web.component.util.BasePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Honchar on 13.01.2016.
 */
public class ListMultipleChoiceTransferPanel<T extends String> extends BasePanel {

    private static final String ID_EXISTING_VALUES = "existingValuesPanel";
    private static final String ID_CHOICES = "choicesPanel";
    private static final String ID_BUTTON_REMOVE = "remove";
    private static final String ID_BUTTON_ADD = "add";
    private static final String ID_FORM = "form";
    private static final String ID_ROLE_SELECTOR = "roleSelector";
    private ArrayList<String> selectedOriginals = new ArrayList<>();
    private ArrayList<String> selectedDestinations = new ArrayList<>();

    public ListMultipleChoiceTransferPanel(String id, IModel<List<T>> existingValuesModel, IModel<List<T>> choicesModel){
        //TODO what model set for this class? it should contain both of existingValuesModel, choicesModel?
        super(id, existingValuesModel);
        initLayout(existingValuesModel, choicesModel);
    }

    private void initLayout(IModel<List<T>> existingValuesModel, IModel<List<T>> choicesModel) {


        final ListMultipleChoice choicePanel = new ListMultipleChoice(ID_CHOICES, new PropertyModel(this, "selectedOriginals"), existingValuesModel);
        choicePanel.setOutputMarkupId(true);
//        add(choicePanel);
        final ListMultipleChoice existingValuesPanel = new ListMultipleChoice(ID_EXISTING_VALUES, new PropertyModel(this, "selectedDestinations")
                , choicesModel);

//        ListMultipleChoicePanel existingValuesPanel = new ListMultipleChoicePanel(ID_EXISTING_VALUES, new Model(), existingValuesModel);
        existingValuesPanel.setOutputMarkupId(true);
//        add(existingValuesPanel);
//        AjaxLink add = new AjaxLink(ID_BUTTON_ADD) {
//
//            @Override
//            public void onClick(AjaxRequestTarget target) {
//                update(target, selectedOriginals, choicePanel, existingValuesPanel);
//            }
//        };
        AjaxButton add = new AjaxButton(ID_BUTTON_ADD) {
            @Override

            protected void onSubmit(AjaxRequestTarget target, Form form) {
                update(target, selectedOriginals, existingValuesPanel, choicePanel);
            }
        };
//        add(add);

        Form<?> form = new Form<Void>(ID_FORM) {
            @Override
            protected void onSubmit() {

//                info("Selected Number : " + selectedNumber);

            }
        };

        AjaxButton remove = new AjaxButton(ID_BUTTON_REMOVE) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                update(target, selectedDestinations, choicePanel, existingValuesPanel);
            }
        };
        form.add(remove);
        form.add(existingValuesPanel);
        form.add(choicePanel);
        form.add(add);
        add(form);
    }

        /**
         * Updates the select boxes.
         * @param target The {@link AjaxRequestTarget}.
         */
        private void update(AjaxRequestTarget target, List<String> selections, ListMultipleChoice from, ListMultipleChoice to) {
            for (String destination : selections) {
                List choices =from.getChoices();
                List toChoices = to.getChoices();
                if (choices != null && toChoices != null ){

                }
                if (!to.getChoices().contains(destination)) {
                    to.getChoices().add(destination);
                    choices.remove(destination);
                    from.setChoices(choices);
                }
            }
            target.add(to);
            target.add(from);
        }

    private List<String> getChoices(ListMultipleChoice list){
        if (list != null){
            return (List<String>)list.getChoicesModel().getObject();
        }
        return null;
    }

    }
