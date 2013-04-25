/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.internal;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.ajaxDownload.AjaxDownloadBehaviorFromFile;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.admin.home.PageDashboard;
import com.evolveum.midpoint.web.page.admin.internal.dto.ResourceItemDto;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.file.File;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author lazyman
 */
public class PageAccounts extends PageAdmin {

    private static final Trace LOGGER = TraceManager.getTrace(PageAccounts.class);

    private static final String DOT_CLASS = PageAccounts.class.getName() + ".";
    private static final String OPERATION_LOAD_RESOURCES = DOT_CLASS + "loadResources";
    private static final String OPERATION_LOAD_ACCOUNTS = DOT_CLASS + "loadAccounts";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_RESOURCES = "resources";
    private static final String ID_LIST_SYNC_DETAILS = "listSyncDetails";
    private static final String ID_EXPORT = "export";
    private static final String ID_ACCOUNTS = "accounts";

    private IModel<List<ResourceItemDto>> resourcesModel;

    private IModel<ResourceItemDto> resourceModel = new Model<ResourceItemDto>();

    public PageAccounts() {
        resourcesModel = new LoadableModel<List<ResourceItemDto>>() {

            @Override
            protected List<ResourceItemDto> load() {
                return loadResources();
            }
        };

        initLayout();
    }

    private void initLayout() {
        Form form = new Form(ID_MAIN_FORM);
        add(form);

        DropDownChoice<ResourceItemDto> resources = new DropDownChoice<ResourceItemDto>(
                ID_RESOURCES, resourceModel, resourcesModel,
                new IChoiceRenderer<ResourceItemDto>() {

                    @Override
                    public Object getDisplayValue(ResourceItemDto object) {
                        if (object == null) {
                            return "";
                        }

                        return object.getName();
                    }

                    @Override
                    public String getIdValue(ResourceItemDto object, int index) {
                        return Integer.toString(index);
                    }
                });
        form.add(resources);

        AjaxSubmitLink listSyncDetails = new AjaxSubmitLink(ID_LIST_SYNC_DETAILS) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                listSyncDetailsPerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        form.add(listSyncDetails);


        final AjaxDownloadBehaviorFromFile ajaxDownloadBehavior = new AjaxDownloadBehaviorFromFile(true) {

            @Override
            protected File initFile() {
//                return initDownloadFile(choice);
                //todo
                return null;
            }
        };
        form.add(ajaxDownloadBehavior);

        AjaxLink export = new AjaxLink(ID_EXPORT) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                exportPerformed(target);
            }
        };
        form.add(export);

        ObjectDataProvider provider = new ObjectDataProvider(this, ShadowType.class);
        provider.setQuery(createResourceQuery());
        TablePanel accounts = new TablePanel(ID_ACCOUNTS, provider, createAccountsColumns());
        accounts.setOutputMarkupId(true);
        accounts.setItemsPerPage(50);
        form.add(accounts);
    }

    private List<IColumn> createAccountsColumns() {
        List<IColumn> columns = new ArrayList<IColumn>();

        columns.add(new PropertyColumn(createStringResource("PageAccounts.accounts.oid"),
                SelectableBean.F_VALUE + ".oid"));
        columns.add(new PropertyColumn(createStringResource("PageAccounts.accounts.name"),
                SelectableBean.F_VALUE + ".name"));
        columns.add(new PropertyColumn(createStringResource("PageAccounts.accounts.synchronizationSituation"),
                SelectableBean.F_VALUE + ".synchronizationSituation"));
        columns.add(new PropertyColumn(createStringResource("PageAccounts.accounts.synchronizationTimestamp"),
                SelectableBean.F_VALUE + ".synchronizationTimestamp"));

        return columns;
    }

    private ObjectQuery createResourceQuery() {
        ResourceItemDto dto = resourceModel.getObject();
        if (dto == null) {
            return null;
        }
        OperationResult result = new OperationResult(OPERATION_LOAD_ACCOUNTS);
        String oid = dto.getOid();
        try {
            RefFilter resourceRef = RefFilter.createReferenceEqual(ShadowType.class,
                    ShadowType.F_RESOURCE_REF, getPrismContext(), oid);

            PrismObject<ResourceType> resource = getModelService().getObject(ResourceType.class, oid, null,
                    createSimpleTask(OPERATION_LOAD_ACCOUNTS), result);
            RefinedResourceSchema schema = RefinedResourceSchema.getRefinedSchema(resource);
            QName qname = null;
            for (RefinedObjectClassDefinition def : schema.getRefinedDefinitions(ShadowKindType.ACCOUNT)) {
                if (def.isDefault()) {
                    qname = def.getObjectClassDefinition().getTypeName();
                    break;
                }
            }

            if (qname == null) {
                error("Couldn't find default object class for resource '" + WebMiscUtil.getName(resource) + "'.");
                return null;
            }

            EqualsFilter objectClass = EqualsFilter.createEqual(ShadowType.class, getPrismContext(),
                    ShadowType.F_OBJECT_CLASS, qname);

            return ObjectQuery.createObjectQuery(AndFilter.createAnd(resourceRef, objectClass));
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't create query", ex);
            error("Couldn't create query, reason: " + ex.getMessage());
        } finally {
            result.recomputeStatus();
        }

        if (!WebMiscUtil.isSuccessOrHandledError(result)) {
            showResult(result);
        }

        return null;
    }

    private List<ResourceItemDto> loadResources() {
        List<ResourceItemDto> resources = new ArrayList<ResourceItemDto>();

        OperationResult result = new OperationResult(OPERATION_LOAD_RESOURCES);
        try {
            List<PrismObject<ResourceType>> objects = getModelService().searchObjects(ResourceType.class, null, null,
                    createSimpleTask(OPERATION_LOAD_RESOURCES), result);

            if (objects != null) {
                for (PrismObject<ResourceType> object : objects) {
                    resources.add(new ResourceItemDto(object.getOid(), WebMiscUtil.getName(object)));
                }
            }
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't load resources", ex);
            result.recordFatalError("Couldn't load resources, reason: " + ex.getMessage(), ex);
        } finally {
            if (result.isUnknown()) {
                result.recomputeStatus();
            }
        }

        Collections.sort(resources);

        if (!WebMiscUtil.isSuccessOrHandledError(result)) {
            showResultInSession(result);
            throw new RestartResponseException(PageDashboard.class);
        }

        return resources;
    }

    private void listSyncDetailsPerformed(AjaxRequestTarget target) {
        TablePanel table = (TablePanel) get(createComponentPath(ID_MAIN_FORM, ID_ACCOUNTS));
        ObjectDataProvider provider = (ObjectDataProvider) table.getDataTable().getDataProvider();
        provider.setQuery(createResourceQuery());
        table.getDataTable().setCurrentPage(0);

        target.add(table, getFeedbackPanel());
    }

    private void exportPerformed(AjaxRequestTarget target) {
        //todo
    }
}
