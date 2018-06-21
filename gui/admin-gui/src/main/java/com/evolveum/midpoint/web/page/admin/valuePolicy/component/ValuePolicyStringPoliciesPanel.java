package com.evolveum.midpoint.web.page.admin.valuePolicy.component;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectTabPanel;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StringPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.Model;

/**
 * Created by matus on 2/20/2018.
 */
public class ValuePolicyStringPoliciesPanel extends AbstractObjectTabPanel {


    private static final String ID_STRING_POLICIES_LIST="stringPoliciesList";
    private static final String ID_NEW_STRINGPOLICY_BUTTON="newStringPolicy";

    public static final String ID_DETAILS = "details";

    protected boolean detailsVisible;
    public ValuePolicyStringPoliciesPanel(String id, Form mainForm, LoadableModel objectWrapperModel, PageBase pageBase) {
        super(id, mainForm, objectWrapperModel, pageBase);
        initPanelLayout();
    }

    private void initPanelLayout(){
    initStringPolicyList();
    initStringPolicyDetails();

    }

    private void initStringPolicyList(){
        WebMarkupContainer stringPolicyContainer = new WebMarkupContainer(ID_STRING_POLICIES_LIST);
        stringPolicyContainer.setOutputMarkupId(true);
        BoxedTablePanel<ObjectWrapper<ValuePolicyType>> stringPolicyTablePanel = initTable();
        stringPolicyContainer.add(stringPolicyTablePanel);

        AjaxIconButton newObjectIcon = new AjaxIconButton(ID_NEW_STRINGPOLICY_BUTTON, new Model<>("fa fa-plus"),
                createStringResource("MainObjectListPanel.newObject")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                addNewStringPolicy(target);
            }
        };
        stringPolicyContainer.add(newObjectIcon);
        stringPolicyContainer.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isVisible() {
                return !detailsVisible;
            }
        });
            }
    private BoxedTablePanel initTable(){

    return null;
    }
    private void initStringPolicyDetails(){
        WebMarkupContainer detailsContainer = new WebMarkupContainer(ID_DETAILS);
        detailsContainer.setOutputMarkupId(true);

        detailsContainer.add(new VisibleEnableBehaviour() {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return detailsVisible;
            }
        });
        add(detailsContainer);
    }
    private void addNewStringPolicy(AjaxRequestTarget target){


    }
}
