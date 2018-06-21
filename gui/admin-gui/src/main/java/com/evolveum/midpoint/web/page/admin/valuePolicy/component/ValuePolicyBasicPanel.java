package com.evolveum.midpoint.web.page.admin.valuePolicy.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectTabPanel;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.PrismPanel;
import com.evolveum.midpoint.web.model.ContainerWrapperListFromObjectWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by matus on 2/20/2018.
 */
public class ValuePolicyBasicPanel extends AbstractObjectTabPanel {

    private static final String ID_VALUE_POLICY_BASIC_DETAIL = "valuePolicyBasic";
    private static final String ID_MAIN_FORM_BASIC = "mainFormBasic";

    public ValuePolicyBasicPanel(String id, Form mainForm, LoadableModel objectWrapperModel, PageBase pageBase) {
        super(id, mainForm, objectWrapperModel, pageBase);
        initPanelLayout();
    }


    private void initPanelLayout(){

 List<ItemPath> itemPath = new ArrayList<>();

        itemPath.add(ItemPath.EMPTY_PATH);
        //itemPath.add(new ItemPath(ValuePolicyType.F_STRING_POLICY));
        PrismPanel<ValuePolicyType> valuePolicyForm = new PrismPanel<>(ID_VALUE_POLICY_BASIC_DETAIL, new ContainerWrapperListFromObjectWrapperModel<ValuePolicyType,ValuePolicyType>(getObjectWrapperModel(), itemPath),null, getMainForm(), null, getPageBase());
        add(valuePolicyForm);
    }
}
