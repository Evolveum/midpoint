/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.page.admin.services;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.LambdaColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.common.archetypes.ArchetypeManager;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.RelationTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author katkav
 * @author lazyman
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/applicationServices")
        },
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SERVICES_DEFAULT_APPLICATIONS_URL,
                        label = "PageAdminServices.auth.applicationServices.label",
                        description = "PageAdminServices.auth.servicesAll.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SERVICES_DEFAULT_APPLICATIONS_URL,
                        label = "PageServices.auth.applicationServices.label",
                        description = "PageServices.auth.applicationServices.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SERVICES_DEFAULT_APPLICATIONS_VIEW_URL,
                        label = "PageServices.auth.applicationServices.view.label",
                        description = "PageServices.auth.applicationServices.view.description") })
@SuppressWarnings("unused")
public class PageDefaultApplicationServices extends PageApplicationServices {

    @Serial private static final long serialVersionUID = 1L;

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.TABLE_SERVICES_APPLICATIONS;
    }

    @Override
    protected List<IColumn<SelectableBean<ServiceType>, String>> createDefaultColumns() {
        List<IColumn<SelectableBean<ServiceType>, String>> columns = new ArrayList<>();

        columns.add(new LambdaColumn<>(
                createStringResource("ObjectType.description"), null, c -> c.getValue().getDescription()));

        columns.add(new AbstractColumn<>(createStringResource("PageDefaultApplicationServices.column.owner")) {

            @Override
            public void populateItem(
                    Item<ICellPopulator<SelectableBean<ServiceType>>> item,
                    String id,
                    IModel<SelectableBean<ServiceType>> model) {

                IModel<String> dataModel = new LoadableDetachableModel<>() {

                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    protected String load() {
                        ServiceType service = model.getObject().getValue();

                        // @formatter:off
                        ObjectQuery query = PrismContext.get().queryFor(FocusType.class)
                                .item(FocusType.F_ROLE_MEMBERSHIP_REF)
                                .ref(service.getOid(), ServiceType.COMPLEX_TYPE, RelationTypes.OWNER.getRelation())
                                .build();
                        // @formatter:on

                        OperationResult result = new OperationResult("Search service owners");

                        List<PrismObject<FocusType>> objects = WebModelServiceUtils.searchObjects(
                                FocusType.class, query, List.of(), result, PageDefaultApplicationServices.this);

                        return objects.stream()
                                .map(o -> {
                                    String name = WebComponentUtil.getDisplayName(o);
                                    if (name != null) {
                                        return name;
                                    }

                                    return WebComponentUtil.getName(o);
                                })
                                .collect(Collectors.joining(", "));
                    }
                };

                item.add(new Label(id, dataModel));
            }
        });
        columns.add(new AbstractColumn<>(createStringResource("PageDefaultApplicationServices.column.classification")) {

            @Override
            public void populateItem(
                    Item<ICellPopulator<SelectableBean<ServiceType>>> item,
                    String id,
                    IModel<SelectableBean<ServiceType>> model) {

                IModel<String> dataModel = new LoadableDetachableModel<>() {

                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    protected String load() {
                        ArchetypeManager archetypeManager = MidPointApplication.get().getArchetypeManager();

                        ServiceType service = model.getObject().getValue();

                        for (AssignmentType assignment : service.getAssignment()) {
                            if (PolicyType.COMPLEX_TYPE == assignment.getTargetRef().getType()) {
                                PrismObject<PolicyType> policy =
                                        WebModelServiceUtils.loadObject(assignment.getTargetRef(), PageDefaultApplicationServices.this);
                                Set<String> oids = archetypeManager.determineArchetypeOids(policy.asObjectable());
                                if (oids.contains(SystemObjectsType.ARCHETYPE_CLASSIFICATION.value())) {
                                    return policy.getName().getOrig();
                                }
                            }
                        }

                        return null;
                    }
                };

                item.add(new Label(id, dataModel));
            }
        });

        return columns;
    }
}
