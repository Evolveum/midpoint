/*
 * Copyright (c) 2010-2018 Evolveum
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
package com.evolveum.midpoint.gui.api.component;

import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.RoleSelectionSpecification;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by honchar.
 */
public abstract class AbstractAssignmentPopupTabPanel<O extends ObjectType> extends BasePanel {

    private static final long serialVersionUID = 1L;

    private static final String ID_OBJECT_LIST_PANEL = "objectListPanel";

    private static final String DOT_CLASS = AbstractAssignmentPopupTabPanel.class.getName();
    private static final Trace LOGGER = TraceManager.getTrace(AbstractAssignmentPopupTabPanel.class);
    private static final String OPERATION_LOAD_ASSIGNABLE_ROLES = DOT_CLASS + "loadAssignableRoles";

    private ObjectTypes type;

    public AbstractAssignmentPopupTabPanel(String id, ObjectTypes type){
        super(id);
        this.type = type;
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        add(initObjectListPanel());
        initParametersPanel();
    }

    private PopupObjectListPanel initObjectListPanel(){
        PopupObjectListPanel<O> listPanel = new PopupObjectListPanel<O>(ID_OBJECT_LIST_PANEL, (Class)type.getClassDefinition(), true, getPageBase()) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdateCheckbox(AjaxRequestTarget target) {
//                if (type.equals(ObjectTypes.RESOURCE)) {
//                    target.add(AbstractAssignmentPopupTabPanel.this);
//                }
                onSelectionPerformed(target);
            }

            @Override
            protected IModel<Boolean> getCheckBoxEnableModel(IModel<SelectableBean<O>> rowModel){
                return getObjectSelectCheckBoxEnableModel(rowModel);
            }

            @Override
            protected ObjectQuery addFilterToContentQuery(ObjectQuery query) {
                if (type.equals(RoleType.COMPLEX_TYPE)) {
                    LOGGER.debug("Loading roles which the current user has right to assign");
                    Task task = AbstractAssignmentPopupTabPanel.this.getPageBase().createSimpleTask(OPERATION_LOAD_ASSIGNABLE_ROLES);
                    OperationResult result = task.getResult();
                    ObjectFilter filter = null;
                    try {
                        ModelInteractionService mis = AbstractAssignmentPopupTabPanel.this.getPageBase().getModelInteractionService();
                        RoleSelectionSpecification roleSpec =
                                mis.getAssignableRoleSpecification(SecurityUtils.getPrincipalUser().getUser().asPrismObject(), task, result);
                        filter = roleSpec.getFilter();
                    } catch (Exception ex) {
                        LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load available roles", ex);
                        result.recordFatalError("Couldn't load available roles", ex);
                    } finally {
                        result.recomputeStatus();
                    }
                    if (!result.isSuccess() && !result.isHandledError()) {
                        AbstractAssignmentPopupTabPanel.this.getPageBase().showResult(result);
                    }
                    if (query == null){
                        query = new ObjectQuery();
                    }
                    query.addFilter(filter);
                }
                return query;
            }

        };

        listPanel.setOutputMarkupId(true);
        return listPanel;
    }

    protected PopupObjectListPanel getObjectListPanel(){
        return (PopupObjectListPanel)get(ID_OBJECT_LIST_PANEL);
    }

    protected List getSelectedObjectsList(){
        PopupObjectListPanel objectListPanel = getObjectListPanel();
        if (objectListPanel == null){
            return new ArrayList();
        }
        return objectListPanel.getSelectedObjects();
    }

    protected IModel<Boolean> getObjectSelectCheckBoxEnableModel(IModel<SelectableBean<O>> rowModel){
        return Model.of(true);
    }

    protected abstract void initParametersPanel();

    protected void onSelectionPerformed(AjaxRequestTarget target){}
}
