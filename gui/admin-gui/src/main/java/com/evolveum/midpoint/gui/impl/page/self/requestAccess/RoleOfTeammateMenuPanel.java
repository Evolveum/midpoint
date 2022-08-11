/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.wicketstuff.select2.ChoiceProvider;
import org.wicketstuff.select2.Response;
import org.wicketstuff.select2.Select2Choice;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Viliam Repan (lazyman).
 */
public class RoleOfTeammateMenuPanel<T extends Serializable> extends BasePanel<ListGroupMenuItem<T>> {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(RoleCatalogPanel.class);

    private static final String DOT_CLASS = RoleCatalogPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_USERS = DOT_CLASS + "loadUsers";

    private static final int MULTISELECT_PAGE_SIZE = 10;

    private static final String ID_LINK = "link";
    private static final String ID_CONTAINER = "container";
    private static final String ID_INPUT = "input";
    private static final String ID_MANUAL = "manual";

    public RoleOfTeammateMenuPanel(String id, IModel<ListGroupMenuItem<T>> model) {
        super(id, model);

        initLayout();
    }

    private void initLayout() {
        add(AttributeAppender.append("class", () -> getModelObject().isOpen() ? "open" : null));

        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        container.setOutputMarkupPlaceholderTag(true);
        container.add(new VisibleBehaviour(() -> getModelObject().isActive()));
        add(container);

        MenuItemLinkPanel link = new MenuItemLinkPanel(ID_LINK, getModel()) {

            @Override
            protected void onClickPerformed(AjaxRequestTarget target, ListGroupMenuItem item) {
                target.add(container);

                RoleOfTeammateMenuPanel.this.onClickPerformed(target, item);
            }
        };
        add(link);

        IModel<ObjectReferenceType> selectModel = Model.of((ObjectReferenceType) null);

        Select2Choice select = new Select2Choice(ID_INPUT, selectModel, new ObjectReferenceProvider(this));
        select.getSettings()
                .setMinimumInputLength(2);
        select.add(new AjaxFormComponentUpdatingBehavior("change") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                onSelectionUpdate(target, selectModel.getObject());
            }
        });
        container.add(select);

        AjaxLink manual = new AjaxLink<>(ID_MANUAL) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                onManualSelectionPerformed(target);
            }
        };
        container.add(manual);
    }

    protected void onManualSelectionPerformed(AjaxRequestTarget target) {

    }

    protected void onSelectionUpdate(AjaxRequestTarget target, ObjectReferenceType newSelection) {

    }

    protected void onClickPerformed(AjaxRequestTarget target, ListGroupMenuItem item) {

    }

    public static class ObjectReferenceProvider extends ChoiceProvider<ObjectReferenceType> {

        private static final long serialVersionUID = 1L;

        private BasePanel panel;

        public ObjectReferenceProvider(BasePanel panel) {
            this.panel = panel;
        }

        @Override
        public String getDisplayValue(ObjectReferenceType ref) {
            return WebComponentUtil.getDisplayNameOrName(ref);
        }

        @Override
        public String getIdValue(ObjectReferenceType ref) {
            return ref != null ? ref.getOid() : null;
        }

        @Override
        public void query(String text, int page, Response<ObjectReferenceType> response) {
            ObjectFilter substring = panel.getPrismContext().queryFor(UserType.class)
                    .item(UserType.F_NAME).containsPoly(text).matchingNorm().buildFilter();

            ObjectQuery query = panel.getPrismContext()
                    .queryFor(UserType.class)
                    .filter(substring)
                    .asc(UserType.F_NAME)
                    .maxSize(MULTISELECT_PAGE_SIZE).offset(page * MULTISELECT_PAGE_SIZE).build();

            Task task = panel.getPageBase().createSimpleTask(OPERATION_LOAD_USERS);
            OperationResult result = task.getResult();

            try {
                List<PrismObject<UserType>> objects = WebModelServiceUtils.searchObjects(UserType.class, query, result, panel.getPageBase());

                response.addAll(objects.stream()
                        .map(o -> new ObjectReferenceType()
                                .oid(o.getOid())
                                .type(UserType.COMPLEX_TYPE)
                                .targetName(WebComponentUtil.getDisplayNameOrName(o))).collect(Collectors.toList()));
            } catch (Exception ex) {
                LOGGER.debug("Couldn't search users for multiselect", ex);
            }
        }

        @Override
        public Collection<ObjectReferenceType> toChoices(Collection<String> collection) {
            return collection.stream()
                    .map(oid -> new ObjectReferenceType()
                            .oid(oid)
                            .type(UserType.COMPLEX_TYPE)).collect(Collectors.toList());
        }
    }
}
