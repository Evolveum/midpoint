/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.common.Utils;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.menu.listGroup.ListGroupMenu;
import com.evolveum.midpoint.gui.impl.component.menu.listGroup.ListGroupMenuItem;
import com.evolveum.midpoint.gui.impl.component.menu.listGroup.ListGroupMenuPanel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.error.PageError404;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Viliam Repan (lazyman).
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/simulations/result/${RESULT_OID}/objects",
                        matchUrlForSecurity = "/admin/simulations/result/?*/objects"),
                @Url(mountUrl = "/admin/simulations/result/${RESULT_OID}/tag/${TAG_OID}/objects",
                        matchUrlForSecurity = "/admin/simulations/result/?*/tag/?*/objects")
        },
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SIMULATIONS_ALL_URL,
                        label = "PageSimulationResults.auth.simulationsAll.label",
                        description = "PageSimulationResults.auth.simulationsAll.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SIMULATION_PROCESSED_OBJECTS_URL,
                        label = "PageSimulationResultObjects.auth.simulationProcessedObjects.label",
                        description = "PageSimulationResultObjects.auth.simulationProcessedObjects.description")
        }
)
public class PageSimulationResultObjects extends PageAdmin implements SimulationPage {

    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = SimulationPage.class.getName() + ".";

    private static final String OPERATION_LOAD_RESULT = DOT_CLASS + "loadResult";
    private static final String OPERATION_LOAD_TAG = DOT_CLASS + "loadTag";

    private static final String ID_MENU = "menu";
    private static final String ID_TABLE = "table";

    private IModel<SimulationResultType> resultModel;

    private IModel<ListGroupMenu<String>> menuModel;

    public PageSimulationResultObjects() {
        this(new PageParameters());
    }

    public PageSimulationResultObjects(PageParameters parameters) {
        super(parameters);

        initModels();
        initLayout();
    }

    private void initModels() {
        resultModel = new LoadableDetachableModel<>() {

            @Override
            protected SimulationResultType load() {
                String resultOid = getPageParameterResultOid();

                Task task = getPageTask();
                OperationResult result = task.getResult().createSubresult(OPERATION_LOAD_RESULT);

                PrismObject<SimulationResultType> object = WebModelServiceUtils.loadObject(
                        SimulationResultType.class, resultOid, PageSimulationResultObjects.this, task, result);
                if (object == null) {
                    throw new RestartResponseException(PageError404.class);
                }
                result.computeStatusIfUnknown();

                return object.asObjectable();
            }
        };

        menuModel = new LoadableDetachableModel<>() {

            @Override
            protected ListGroupMenu<String> load() {
                ListGroupMenu<String> menu = new ListGroupMenu<>();

                SimulationResultType result = resultModel.getObject();
                List<SimulationMetricValuesType> metrics = result.getMetric().stream()
                        .filter(m -> m.getRef() != null && m.getRef().getEventTagRef() != null)
                        .collect(Collectors.toList());

                Task task = getPageTask();

                int allCount = 0;
                for (SimulationMetricValuesType metric : metrics) {
                    ObjectReferenceType ref = metric.getRef().getEventTagRef();

                    OperationResult opResult = task.getResult().createSubresult(OPERATION_LOAD_TAG);

                    PrismObject<TagType> object = WebModelServiceUtils.loadObject(ref, PageSimulationResultObjects.this, task, opResult);
                    opResult.computeStatusIfUnknown();

                    if (object == null) {
                        continue;
                    }

                    int count = 0;
                    ListGroupMenuItem<String> item = createTagMenuItem(object, count);
                    menu.getItems().add(item);

                    allCount += count;
                }

                ListGroupMenuItem<String> all = new ListGroupMenuItem<>("PageSimulationResultObjects.all");
                all.setIconCss(GuiStyleConstants.CLASS_TAG + " d-none");
                all.setBadge(Integer.toString(allCount));
                all.setBadgeCss("badge badge-danger");
                menu.getItems().add(0, all);

                return menu;
            }
        };
    }

    private ListGroupMenuItem<String> createTagMenuItem(PrismObject<TagType> object, int count) {
        String name = WebComponentUtil.getDisplayNameOrName(object, true);
        ListGroupMenuItem<String> item = new ListGroupMenuItem<>(name);

        TagType tag = object.asObjectable();
        DisplayType display = tag.getDisplay();
        item.setIconCss(getTagMenuIcon(display));
        item.setBadge(Integer.toString(count));
        item.setBadge("badge badge-danger");

        item.setValue(object.getOid());

        return item;
    }

    private String getTagMenuIcon(DisplayType display) {
        if (display == null || display.getIcon() == null) {
            return GuiStyleConstants.CLASS_TAG;
        }

        String iconCssClass = display.getIcon().getCssClass();
        return iconCssClass != null ? iconCssClass : GuiStyleConstants.CLASS_TAG;
    }

    private void initLayout() {
        ListGroupMenuPanel menu = new ListGroupMenuPanel(ID_MENU, menuModel) {

            @Override
            protected void onMenuClickPerformed(AjaxRequestTarget target, ListGroupMenuItem item) {
                super.onMenuClickPerformed(target, item);

                if (!item.isActive()) {
                    return;
                }

                // todo add action
            }
        };
        add(menu);

        ProcessedObjectsPanel table = new ProcessedObjectsPanel(ID_TABLE) {

            @Override
            protected @NotNull String getSimulationResultOid() {
                String oid = getPageParameterResultOid();
                if (!Utils.isPrismObjectOidValid(oid)) {
                    throw new RestartResponseException(PageError404.class);
                }

                return oid;
            }

            @Override
            protected String getTagOid() {
                String oid = getPageParameterTagOid();
                if (oid != null && !Utils.isPrismObjectOidValid(oid)) {
                    throw new RestartResponseException(PageError404.class);
                }

                return oid;
            }
        };
        add(table);
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        return () -> {
            String oid = getPageParameterResultOid();

            if (!Utils.isPrismObjectOidValid(oid)) {
                throw new RestartResponseException(PageError404.class);
            }

            String name = WebModelServiceUtils.resolveReferenceName(
                    new ObjectReferenceType().oid(oid).type(SimulationResultType.COMPLEX_TYPE), this);

            return getString("PageSimulationResultObjects.title", name);
        };
    }
}
