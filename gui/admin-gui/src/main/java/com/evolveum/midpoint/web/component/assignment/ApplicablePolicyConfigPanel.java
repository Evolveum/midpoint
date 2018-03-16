/**
 * Copyright (c) 2015-2018 Evolveum
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
package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.util.ModelContextUtil;
import com.evolveum.midpoint.model.api.util.ModelUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectMainPanel;
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by honchar.
 */
public class ApplicablePolicyConfigPanel extends BasePanel<ContainerWrapper<AssignmentType>>{
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ApplicablePolicyConfigPanel.class);
    private static final String DOT_CLASS = ApplicablePolicyConfigPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_SYS_CONFIG = DOT_CLASS + "loadSystemConfiguration";

    private static final String ID_POLICY_GROUPS = "policiesGroups";
    private static final String ID_POLICY_GROUP_PANEL = "policyGroupPanel";

    private LoadableModel<List<ObjectReferenceType>> policyGroupsListModel;

    public ApplicablePolicyConfigPanel(String id, IModel<ContainerWrapper<AssignmentType>> model){
        super(id, model);
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initModels();
        initLayout();
    }

    private void initModels(){
        policyGroupsListModel = new LoadableModel<List<ObjectReferenceType>>(false) {
            private static final long serialVersionUID = 1L;

            @Override
            protected List<ObjectReferenceType> load() {
                List<ObjectReferenceType> policyGroupsList = new ArrayList<>();
                OperationResult result = new OperationResult(OPERATION_LOAD_SYS_CONFIG);
                try {
                    SystemConfigurationType sysConfig = getPageBase().getModelInteractionService().getSystemConfiguration(result);
                    if (sysConfig == null){
                        return policyGroupsList;
                    } else {
                        ObjectPolicyConfigurationType policyConfig = ModelUtils.determineObjectPolicyConfiguration(getMainPanelFocusObject(), sysConfig);
                        if (policyConfig != null && policyConfig.getApplicablePolicies() != null){
                            return policyConfig.getApplicablePolicies().getPolicyGroupRef();
                        }
                    }
                } catch (Exception ex){
                    LoggingUtils.logUnexpectedException(LOGGER, "Cannot retrieve system configuration", ex);
                }
                return policyGroupsList;
            }
        };
    }

    private void initLayout(){
        ListView<ObjectReferenceType> policyGroupsPanel = new ListView<ObjectReferenceType>(ID_POLICY_GROUPS, policyGroupsListModel) {
            @Override
            protected void populateItem(ListItem<ObjectReferenceType> listItem) {
                ApplicablePolicyGroupPanel groupPanel = new ApplicablePolicyGroupPanel(ID_POLICY_GROUP_PANEL, listItem.getModel(),
                        ApplicablePolicyConfigPanel.this.getModel());
                groupPanel.setOutputMarkupId(true);
                listItem.add(groupPanel);
            }
        };
        policyGroupsPanel.setOutputMarkupId(true);
        add(policyGroupsPanel);
    }

    private PrismObject<FocusType> getMainPanelFocusObject(){
        AbstractObjectMainPanel mainPanel = ApplicablePolicyConfigPanel.this.findParent(AbstractObjectMainPanel.class);
        if (mainPanel != null){
            return mainPanel.getObject();
        }
        return null;
    }

}
