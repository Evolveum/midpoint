package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;
import com.evolveum.midpoint.web.component.prism.ContainerWrapperFactory;
import com.evolveum.midpoint.web.component.prism.PrismContainerPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractPolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * Created by honchar
 */
public class PolicyRuleConstraintsExpandablePanel<P extends AbstractPolicyConstraintType> extends BasePanel<P>{
    private static final String ID_BOX_TITLE = "boxTitle";
    private static final String ID_REMOVE_BUTTON = "removeButton";
    private static final String ID_PROPERTIES_CONTAINER = "propertiesContainer";

    private ContainerWrapper policyRuleConstraintsContainerWrapper;

    public PolicyRuleConstraintsExpandablePanel(String id, IModel<P> model){
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initContainerWrapper();
        initLayout();
    }

    private void initContainerWrapper(){
        ContainerWrapperFactory cwf = new ContainerWrapperFactory(getPageBase());
        ItemPath exclusionContainerPath = new ItemPath(AssignmentType.F_POLICY_RULE, PolicyRuleType.F_POLICY_CONSTRAINTS,
                PolicyConstraintsType.F_EXCLUSION);

//                    if (exclusionContainer != null) {
        policyRuleConstraintsContainerWrapper = cwf.createCustomContainerWrapper(getModelObject(), ContainerStatus.MODIFYING, exclusionContainerPath, false, true);
//                    } else {
//                    exclusionContainer = containerDef.instantiate();
//                        containerWrapper = cwf.createContainerWrapper(exclusionContainer, ContainerStatus.ADDING, exclusionContainerPath, false);
//                    }

    }

    private void initLayout(){
        Label boxTitle = new Label(ID_BOX_TITLE, getModel().getObject().asPrismContainerValue().getPath().last());
        add(boxTitle);

        AjaxButton removeRowButton = new AjaxButton(ID_REMOVE_BUTTON) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
//                policyRuleConstraintsContainerWrapper.getStatus().
            }
        };
        add(removeRowButton);

        PrismContainerPanel propertiesPanel = new PrismContainerPanel(ID_PROPERTIES_CONTAINER,
                Model.of(policyRuleConstraintsContainerWrapper), false, null, getPageBase());
        propertiesPanel.setOutputMarkupId(true);
        add(propertiesPanel);

    }
}
