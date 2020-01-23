/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.valuePolicy.component;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectTabPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;

/**
 * Created by matus on 2/20/2018.
 */
public class ValuePolicyBasicPanel extends AbstractObjectTabPanel<ValuePolicyType> {

    private static final String ID_VALUE_POLICY_BASIC_DETAIL = "valuePolicyBasic";
    private static final String ID_MAIN_FORM_BASIC = "mainFormBasic";

    public ValuePolicyBasicPanel(String id, Form mainForm, LoadableModel<PrismObjectWrapper<ValuePolicyType>> objectWrapperModel) {
        super(id, mainForm, objectWrapperModel);

    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
         initPanelLayout();
    }


    private void initPanelLayout(){

 List<ItemPath> itemPath = new ArrayList<>();

        itemPath.add(ItemPath.EMPTY_PATH);
        //itemPath.add(prismContext.path(ValuePolicyType.F_STRING_POLICY));
//        PrismPanel<ValuePolicyType> valuePolicyForm = new PrismPanel<>(ID_VALUE_POLICY_BASIC_DETAIL, new ContainerWrapperListFromObjectWrapperModel<ValuePolicyType,ValuePolicyType>(getObjectWrapperModel(), itemPath),null, getMainForm(), null, getPageBase());
//        add(valuePolicyForm);
    }
}
