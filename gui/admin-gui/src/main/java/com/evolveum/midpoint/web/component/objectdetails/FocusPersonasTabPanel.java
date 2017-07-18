/*
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.web.component.objectdetails;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.InOidFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.FocusSummaryPanel;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.util.ObjectWrapperUtil;
import com.evolveum.midpoint.web.page.admin.roles.component.RoleSummaryPanel;
import com.evolveum.midpoint.web.page.admin.users.component.OrgSummaryPanel;
import com.evolveum.midpoint.web.page.admin.users.component.ServiceSummaryPanel;
import com.evolveum.midpoint.web.page.admin.users.component.UserSummaryPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author honchar
 */
public class FocusPersonasTabPanel<F extends FocusType> extends AbstractObjectTabPanel<F> {
    private static final long serialVersionUID = 1L;
    private static final String DOT_CLASS = FocusPersonasTabPanel.class.getName() + ".";
    private static final String OPERATION_SEARCH_PERSONAS_OBJECTS = DOT_CLASS + "searchPersonas";
    private static final String OPERATION_LOAD_PERSONAS = DOT_CLASS + "loadPersonas";

    private static final Trace LOGGER = TraceManager.getTrace(FocusPersonasTabPanel.class);

    private static final String ID_PERSONAS_CONTAINER = "personasContainer";
    private static final String ID_PERSONAS_TABLE = "personasTable";
    private static final String ID_PERSONAS_SUMMARY = "personaSummary";

    private PageBase pageBase;

    public FocusPersonasTabPanel(String id, Form mainForm, LoadableModel<ObjectWrapper<F>> focusModel,
                                 PageBase page) {
        super(id, mainForm, focusModel, page);
        this.pageBase = page;
        initLayout(page);
    }

    private void initLayout(ModelServiceLocator serviceLocator) {
        WebMarkupContainer container = new WebMarkupContainer(ID_PERSONAS_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        RepeatingView view = new RepeatingView(ID_PERSONAS_TABLE);
        view.setOutputMarkupId(true);
        container.add(view);

        LoadableModel<List<PrismObject<FocusType>>> personasListModel = loadModel();
        if (personasListModel.getObject() == null || personasListModel.getObject().size() == 0){
            WebMarkupContainer viewChild = new WebMarkupContainer(view.newChildId());
            viewChild.setOutputMarkupId(true);
            view.add(viewChild);

            WebMarkupContainer emptyContainer = new WebMarkupContainer(ID_PERSONAS_SUMMARY);
            emptyContainer.setOutputMarkupId(true);
            viewChild.add(emptyContainer);
            return;
        }
        Task task = pageBase.createSimpleTask(OPERATION_LOAD_PERSONAS);
        for (PrismObject<FocusType> personaObject : personasListModel.getObject()){
            ObjectWrapper<FocusType> personaWrapper = ObjectWrapperUtil.createObjectWrapper(
                    WebComponentUtil.getEffectiveName(personaObject, RoleType.F_DISPLAY_NAME), "", personaObject,
                    ContainerStatus.MODIFYING, task, getPageBase());

            WebMarkupContainer personaPanel = new WebMarkupContainer(view.newChildId());
            personaPanel.setOutputMarkupId(true);
            view.add(personaPanel);

            FocusSummaryPanel.addSummaryPanel(personaPanel, personaObject, personaWrapper, ID_PERSONAS_SUMMARY, serviceLocator);
        }

    }

    private LoadableModel<List<PrismObject<FocusType>>> loadModel(){
        return new LoadableModel<List<PrismObject<FocusType>>>(false) {
            @Override
            protected List<PrismObject<FocusType>> load() {
                List<String> personaOidsList = getPersonasOidsList();
                List<PrismObject<FocusType>> personasList = new ArrayList<>();
                if (personaOidsList.size() > 0){
                    ObjectQuery query = ObjectQuery.createObjectQuery(InOidFilter.createInOid(personaOidsList));
                    OperationResult result = new OperationResult(OPERATION_SEARCH_PERSONAS_OBJECTS);
                    personasList = WebModelServiceUtils.searchObjects(FocusType.class, query, result, pageBase);

                }
                return personasList == null ? new ArrayList<>() : personasList;
            }
        };
    }

    private List<String> getPersonasOidsList(){
        List<ObjectReferenceType> personasRefList = getObjectWrapper().getObject().asObjectable().getPersonaRef();
        List<String> oidsList = new ArrayList<>();
        if (personasRefList != null){
            for (ObjectReferenceType ref : personasRefList){
                oidsList.add(ref.getOid());
            }
        }
        return oidsList;
    }
}
