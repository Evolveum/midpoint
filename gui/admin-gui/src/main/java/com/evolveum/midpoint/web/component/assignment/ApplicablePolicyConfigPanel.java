/*
 * Copyright (c) 2015-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.assignment;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;

import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Created by honchar.
 */
public class ApplicablePolicyConfigPanel<F extends FocusType> extends BasePanel<PrismContainerWrapper<AssignmentType>>{
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ApplicablePolicyConfigPanel.class);
    private static final String DOT_CLASS = ApplicablePolicyConfigPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_SYS_CONFIG = DOT_CLASS + "loadSystemConfiguration";

    private static final String ID_POLICY_GROUPS = "policiesGroups";
    private static final String ID_POLICY_GROUP_PANEL = "policyGroupPanel";

    private LoadableModel<List<ObjectReferenceType>> policyGroupsListModel;
    private LoadableModel<PrismObjectWrapper<F>> abstractRoleModel;

    public ApplicablePolicyConfigPanel(String id, IModel<PrismContainerWrapper<AssignmentType>> model,
            LoadableModel<PrismObjectWrapper<F>> abstractRoleModel){
        super(id, model);
        this.abstractRoleModel = abstractRoleModel;
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
                    ArchetypePolicyType archetypePolicy = getPageBase().getModelInteractionService().determineArchetypePolicy(getAbstractRoleModelObject(), result);
                    if (archetypePolicy == null){
                        return policyGroupsList;
                    } else {
                        if (archetypePolicy.getApplicablePolicies() != null) {
                            return archetypePolicy.getApplicablePolicies().getPolicyGroupRef();
                        }
                    }
                } catch (Exception ex){
                    LoggingUtils.logUnexpectedException(LOGGER, "Cannot retrieve archetype policy for " + getAbstractRoleModelObject(), ex);
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

    private PrismObject<F> getAbstractRoleModelObject(){
        if (abstractRoleModel != null && abstractRoleModel.getObject() != null){
            return abstractRoleModel.getObject().getObject();
        }
        return null;
    }

}
