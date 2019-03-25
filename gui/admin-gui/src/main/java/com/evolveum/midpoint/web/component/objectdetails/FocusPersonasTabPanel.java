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

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.prism.ObjectWrapperOld;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.QueryFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;

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

    private static final String ID_PERSONAS_TABLE = "personasTable";

    public FocusPersonasTabPanel(String id, Form mainForm, LoadableModel<PrismObjectWrapper<F>> focusModel,
                                 PageBase page) {
        super(id, mainForm, focusModel, page);
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        MainObjectListPanel<F, CompiledObjectCollectionView> userListPanel =
                new MainObjectListPanel<F, CompiledObjectCollectionView>(ID_PERSONAS_TABLE,
                (Class<F>) FocusType.class, null, null, getPageBase()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected IColumn<SelectableBean<F>, String> createCheckboxColumn(){
                return null;
            }

            @Override
            protected List<IColumn<SelectableBean<F>, String>> createColumns() {
                return new ArrayList<>();
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                List<InlineMenuItem> menuItems = new ArrayList<>();
                menuItems.add(new ButtonInlineMenuItem(createStringResource("AssignmentPanel.viewTargetObject")) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public String getButtonIconCssClass() {
                        return GuiStyleConstants.CLASS_NAVIGATE_ARROW;
                    }

                    @Override
                    public InlineMenuItemAction initAction() {
                        return new ColumnMenuAction<SelectableBean<F>>() {
                            private static final long serialVersionUID = 1L;

                            @Override
                            public void onClick(AjaxRequestTarget target) {
                                SelectableBean<F> personaRefSelectableBean = getRowModel().getObject();
                                F personaRefObj = personaRefSelectableBean.getValue();
                                ObjectReferenceType ort = new ObjectReferenceType();
                                ort.setOid(personaRefObj.getOid());
                                ort.setType(WebComponentUtil.classToQName(FocusPersonasTabPanel.this.getPrismContext(), personaRefObj.getClass()));
                                WebComponentUtil.dispatchToObjectDetailsPage(ort, FocusPersonasTabPanel.this, false);
                            }
                        };
                    }

                    @Override
                    public boolean isHeaderMenuItem(){
                        return false;
                    }
                });
                return menuItems;          }

            @Override
            protected void objectDetailsPerformed(AjaxRequestTarget target, F object) {
            }

            @Override
            protected void newObjectPerformed(AjaxRequestTarget target, CompiledObjectCollectionView collectionView) {
            }

            @Override
            protected ObjectQuery createContentQuery() {
                List<String> personaOidsList = getPersonasOidsList();
                QueryFactory factory = FocusPersonasTabPanel.this.getPageBase().getPrismContext().queryFactory();
                ObjectQuery query = factory.createQuery(factory.createInOid(personaOidsList));
                return query;
            }

            @Override
            protected boolean isClickable(IModel<SelectableBean<F>> rowModel) {
                return false;
            }
        };
        userListPanel.setOutputMarkupId(true);
        add(userListPanel);


//
//        WebMarkupContainer container = new WebMarkupContainer(ID_PERSONAS_CONTAINER);
//        container.setOutputMarkupId(true);
//        add(container);
//
//        RepeatingView view = new RepeatingView(ID_PERSONAS_TABLE);
//        view.setOutputMarkupId(true);
//        container.add(view);
//
//        LoadableModel<List<PrismObject<FocusType>>> personasListModel = loadModel();
//        if (personasListModel.getObject() == null || personasListModel.getObject().size() == 0){
//            WebMarkupContainer viewChild = new WebMarkupContainer(view.newChildId());
//            viewChild.setOutputMarkupId(true);
//            view.add(viewChild);
//
//            WebMarkupContainer emptyContainer = new WebMarkupContainer(ID_PERSONAS_SUMMARY);
//            emptyContainer.setOutputMarkupId(true);
//            viewChild.add(emptyContainer);
//            return;
//        }
//        Task task = pageBase.createSimpleTask(OPERATION_LOAD_PERSONAS);
//        for (PrismObject<FocusType> personaObject : personasListModel.getObject()){
//            ObjectWrapper<FocusType> personaWrapper = ObjectWrapperUtil.createObjectWrapper(
//                    WebComponentUtil.getEffectiveName(personaObject, RoleType.F_DISPLAY_NAME), "", personaObject,
//                    ContainerStatus.MODIFYING, task, getPageBase());
//
//            WebMarkupContainer personaPanel = new WebMarkupContainer(view.newChildId());
//            personaPanel.setOutputMarkupId(true);
//            view.add(personaPanel);
//
//            FocusSummaryPanel.addSummaryPanel(personaPanel, personaObject, personaWrapper, ID_PERSONAS_SUMMARY, serviceLocator);
//        }

    }

    private LoadableModel<List<PrismObject<FocusType>>> loadModel(){
        return new LoadableModel<List<PrismObject<FocusType>>>(false) {
            @Override
            protected List<PrismObject<FocusType>> load() {
                List<String> personaOidsList = getPersonasOidsList();
                List<PrismObject<FocusType>> personasList = new ArrayList<>();
                if (personaOidsList.size() > 0){
                    QueryFactory factory = getPrismContext().queryFactory();
                    ObjectQuery query = factory.createQuery(factory.createInOid(personaOidsList));
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
