/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.wicketstuff.select2.ChoiceProvider;
import org.wicketstuff.select2.Response;
import org.wicketstuff.select2.Select2Choice;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.autocomplete.AutocompleteConfigurationMixin;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.menu.listGroup.ListGroupMenuItem;
import com.evolveum.midpoint.gui.impl.component.menu.listGroup.MenuItemLinkPanel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AutocompleteSearchConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleCatalogType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class RoleOfTeammateMenuPanel<T extends Serializable>
        extends BasePanel<ListGroupMenuItem<T>> implements AccessRequestMixin {

    @Serial private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(RoleCatalogPanel.class);

    private static final int AUTOCOMPLETE_MIN_INPUT_LENGTH = 2;

    private static final String DOT_CLASS = RoleCatalogPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_USERS = DOT_CLASS + "loadUsers";

    private static final int MULTISELECT_PAGE_SIZE = 10;

    private static final String ID_LINK = "link";
    private static final String ID_CONTAINER = "container";
    private static final String ID_INPUT = "input";
    private static final String ID_MANUAL = "manual";

    private final IModel<ObjectReferenceType> selectionModel;

    private final IModel<RoleCatalogType> roleCatalogConfigurationModel;

    public RoleOfTeammateMenuPanel(
            String id, IModel<ListGroupMenuItem<T>> model, IModel<ObjectReferenceType> selectionModel,
            IModel<RoleCatalogType> roleCatalogConfigurationModel) {
        super(id, model);

        this.selectionModel = selectionModel != null ? selectionModel : Model.of((ObjectReferenceType) null);
        this.roleCatalogConfigurationModel = roleCatalogConfigurationModel;

        initLayout();
    }

    private void initLayout() {
        add(AttributeAppender.append("class", () -> getModelObject().isOpen() ? "open" : null));

        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER){
            @Override
            public void renderHead(IHeaderResponse response) {
                super.renderHead(response);
                response.render(OnDomReadyHeaderItem.forScript("MidPointTheme.initSelect2MultiChoice(" + getMarkupId() + ");"));
            }
        };
        container.setOutputMarkupId(true);
        container.setOutputMarkupPlaceholderTag(true);
        container.add(new VisibleBehaviour(() -> getModelObject().isActive()));
        add(container);

        MenuItemLinkPanel<?> link = new MenuItemLinkPanel<>(ID_LINK, getModel(), 0) {

            @Override
            protected void onClickPerformed(AjaxRequestTarget target, ListGroupMenuItem item) {
                target.add(container);

                RoleOfTeammateMenuPanel.this.onClickPerformed(target, item);
            }
        };
        add(link);

        Select2Choice<ObjectReferenceType> select = new Select2Choice<>(ID_INPUT, selectionModel, new ObjectReferenceProvider(this));

        Integer minInput = getAutocompleteConfiguration().getAutocompleteMinChars();
        if (minInput == null) {
            minInput = AUTOCOMPLETE_MIN_INPUT_LENGTH;
        }
        select.getSettings()
                .setMinimumInputLength(minInput);
        select.add(new AjaxFormComponentUpdatingBehavior("change") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                onSelectionUpdate(target, selectionModel.getObject());
                target.add(RoleOfTeammateMenuPanel.this.get(createComponentPath(ID_CONTAINER, ID_INPUT)));
            }
        });
        container.add(select);

        AjaxLink<?> manual = new AjaxLink<>(ID_MANUAL) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                onManualSelectionPerformed(target);
            }
        };
        container.add(manual);
    }

    private AutocompleteSearchConfigurationType getAutocompleteConfiguration() {
        RoleCatalogType config = roleCatalogConfigurationModel.getObject();

        AutocompleteSearchConfigurationType autocomplete = null;
        if (config.getRolesOfTeammate() != null) {
            autocomplete = config.getRolesOfTeammate().getAutocompleteConfiguration();
        }

        if (autocomplete == null) {
            autocomplete = new AutocompleteSearchConfigurationType();
        }

        return autocomplete;
    }

    protected void onManualSelectionPerformed(AjaxRequestTarget target) {

    }

    protected void onSelectionUpdate(AjaxRequestTarget target, ObjectReferenceType newSelection) {

    }

    protected void onClickPerformed(AjaxRequestTarget target, ListGroupMenuItem item) {

    }

    private ObjectFilter getAutocompleteFilter(String text) {
        SearchFilterType template = getAutocompleteConfiguration().getSearchFilterTemplate();

        return createAutocompleteFilter(text, template, (t) -> createDefaultFilter(t), getPageBase());
    }

    private ObjectFilter createDefaultFilter(String text) {
        return getPrismContext().queryFor(UserType.class)
                .item(UserType.F_NAME).containsPoly(text).matchingNorm().buildFilter();
    }

    public static class ObjectReferenceProvider extends ChoiceProvider<ObjectReferenceType> implements AutocompleteConfigurationMixin {

        @Serial private static final long serialVersionUID = 1L;

        private Map<String, String> mapOidToName = new HashMap<>();

        private final RoleOfTeammateMenuPanel<?> panel;

        public ObjectReferenceProvider(RoleOfTeammateMenuPanel<?> panel) {
            this.panel = panel;
        }

        @Override
        public String getDisplayValue(ObjectReferenceType ref) {
            if (ref == null) {
                return null;
            }

            AutocompleteSearchConfigurationType config = panel.getAutocompleteConfiguration();
            if (config.getDisplayExpression() != null) {
                PrismObject<UserType> obj = WebModelServiceUtils.loadObject(ref, panel.getPageBase());
                if (obj != null) {
                    String name = getDisplayNameFromExpression(
                            "User display name (teammate)", config.getDisplayExpression(),
                            o -> panel.getDefaultUserDisplayName(o), obj, panel);
                    if (StringUtils.isNotEmpty(name)) {
                        ref.setTargetName(new PolyStringType(name));
                    } else {
                        ref.setTargetName(new PolyStringType(obj.getName()));
                    }
                }
            }

            if (ref.getTargetName() != null) {
                return ref.getTargetName().getOrig();
            }

            return WebComponentUtil.getDisplayNameOrName(ref);
        }

        @Override
        public String getIdValue(ObjectReferenceType ref) {
            return ref != null ? ref.getOid() : null;
        }

        @Override
        public void query(String text, int page, Response<ObjectReferenceType> response) {
            ObjectFilter substring = panel.getAutocompleteFilter(text);

            ObjectQuery query = panel.getPrismContext()
                    .queryFor(UserType.class)
                    .filter(substring)
                    .asc(UserType.F_NAME)
                    .maxSize(MULTISELECT_PAGE_SIZE).offset(page * MULTISELECT_PAGE_SIZE).build();

            Task task = panel.getPageBase().createSimpleTask(OPERATION_LOAD_USERS);
            OperationResult result = task.getResult();

            try {
                List<PrismObject<UserType>> objects = WebModelServiceUtils.searchObjects(UserType.class, query, result, panel.getPageBase());

                mapOidToName.clear();

                response.addAll(objects.stream()
                        .map(o ->  {
                            String name = WebComponentUtil.getDisplayNameOrName(o);
                            mapOidToName.put(o.getOid(), name);
                            return new ObjectReferenceType()
                                .oid(o.getOid())
                                .type(UserType.COMPLEX_TYPE)
                                .targetName(name);
                        }).collect(Collectors.toList()));
            } catch (Exception ex) {
                LOGGER.debug("Couldn't search users for multiselect", ex);
            }
        }

        @Override
        public Collection<ObjectReferenceType> toChoices(Collection<String> collection) {
            return collection.stream()
                    .map(oid -> new ObjectReferenceType()
                            .oid(oid)
                            .type(UserType.COMPLEX_TYPE)
                            .targetName(mapOidToName.containsKey(oid) ? mapOidToName.get(oid) : null))
                    .collect(Collectors.toList());
        }
    }
}
