package com.evolveum.midpoint.web.page.admin.certification;

import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.certification.dto.DefinitionScopeDto;
import com.evolveum.midpoint.web.page.admin.certification.dto.StageDefinitionDto;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Kate Honchar.
 */
public class StageDefinitionPanel extends SimplePanel<StageDefinitionDto> {
    private static final String ID_NAME = "name";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_DURATION = "duration";
    private static final String ID_NOTIFY_BEFORE_DEADLINE = "notifyBeforeDeadline";
    private static final String ID_NOTIFY_ONLY_WHEN_NO_DECISION = "notifyOnlyWhenNoDecision";
    private static final String ID_REVIEWER_NAME= "reviewerName";
    private static final String ID_REVIEWER_DESCRIPTION = "reviewerDescription";
    private static final String ID_USE_TARGET_OWNER = "useTargetOwner";
    private static final String ID_USE_TARGET_APPROVER = "useTargetApprover";
    private static final String ID_USE_OBJECT_OWNER = "useObjectOwner";
    private static final String ID_USE_OBJECT_APPROVER = "useObjectApprover";
    private static final String ID_USE_OBJECT_MANAGER = "useObjectManager";
    private static final String ID_DEFAULT_REVIEWER_REF_CONTAINER = "defaultReviewerRefContainer";
    private static final String ID_DEFAULT_REVIEWER_REF = "defaultReviewerRef";
    private static final String ID_ADDITIONAL_REVIEWER_REF_CONTAINER = "additionalReviewerRefContainer";
    private static final String ID_ADDITIONAL_REVIEWER_REF = "additionalReviewerRef";
    private static final String ID_APPROVAL_STRATEGY_CHECKBOX = "approvalStrategyCheckbox";
    private static final String ID_APPROVAL_STRATEGY_INPUT = "approvalStrategyInput";

    public StageDefinitionPanel(String id, IModel<StageDefinitionDto> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {
        TextField nameField = new TextField(ID_NAME, new PropertyModel<>(getModel(), StageDefinitionDto.F_NAME));
        add(nameField);

        TextArea descriptionField = new TextArea(ID_DESCRIPTION, new PropertyModel<>(getModel(), StageDefinitionDto.F_DESCRIPTION));
        add(descriptionField);

        TextField durationField = new TextField(ID_DURATION, new PropertyModel<>(getModel(), StageDefinitionDto.F_DAYS));
        add(durationField);

//        TextField notifyBeforeDeadlineField = new TextField(ID_NOTIFY_BEFORE_DEADLINE, new IModel<String>() {
//            @Override
//            public String getObject() {
//                List<Integer> list = getModelObject().getNotifyBeforeDeadline();
//                String notifyBeforeDeadlineValue = "";
//                for (Integer listItem : list){
//                    notifyBeforeDeadlineValue += Integer.toString(listItem);
//                    if(list.indexOf(listItem) < list.size() - 1){
//                        notifyBeforeDeadlineValue += ", ";
//                    }
//                }
//                return notifyBeforeDeadlineValue;
//            }
//
//            @Override
//            public void setObject(String object) {
//                List<Integer> list = new ArrayList<>();
//                String[] values = object.split(",");
//                for (String value : values){
//                    if (! value.trim().equals("")){
//                        list.add(Integer.parseInt(value.trim()));
//                    }
//                }
//            }
//
//            @Override
//            public void detach() {
//
//            }
//        });
//        add(notifyBeforeDeadlineField);

    }

}
