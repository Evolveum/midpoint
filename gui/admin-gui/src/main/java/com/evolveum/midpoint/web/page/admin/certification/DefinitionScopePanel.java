/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.certification;

import java.util.List;
import javax.xml.namespace.QName;

import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.input.ListMultipleChoicePanel;
import com.evolveum.midpoint.web.component.input.QNameObjectTypeChoiceRenderer;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.certification.dto.DefinitionScopeDto;
import com.evolveum.midpoint.web.page.admin.certification.dto.DefinitionScopeObjectType;

/**
 * @author mederly
 */

public class DefinitionScopePanel extends BasePanel<DefinitionScopeDto> {
    private static final long serialVersionUID = 1L;

    private static final String ID_NAME = "name";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_OBJECT_TYPE_CHOOSER = "objectTypeChooser";
    private static final String ID_OBJECT_TYPE_HELP = "scopeObjectTypeHelp";
    private static final String ID_SEARCH_FILTER = "searchFilterEditor";
    private static final String ID_SEARCH_FILTER_HELP = "scopeSearchFilterHelp";
    private static final String ID_INCLUDE_INDUCEMENTS = "includeInducements";
    private static final String ID_INCLUDE_ASSIGNMENTS = "includeAssignments";
    private static final String ID_ASSIGNMENTS_INDUCEMENTS_HELP = "scopeAssignmentsInducementsHelp";
    private static final String ID_INCLUDE_RESOURCES = "includeResources";
    private static final String ID_INCLUDE_ROLES = "includeRoles";
    private static final String ID_INCLUDE_ORGS = "includeOrgs";
    private static final String ID_INCLUDE_USERS = "includeUsers";
    private static final String ID_INCLUDE_SERVICES = "includeServices";
    private static final String ID_INCLUDE_TARGET_TYPES_HELP = "scopeIncludeTargetTypesHelp";
    private static final String ID_INCLUDE_ENABLED_ITEMS_ONLY = "includeEnabledItemsOnly";
    private static final String ID_INCLUDE_BY_STATUS_HELP = "scopeIncludeByStatusHelp";
    private static final String ID_SCOPE_RELATIONS = "relations";
    private static final String ID_SCOPE_RELATION_HELP = "scopeRelationHelp";

    public DefinitionScopePanel(String id, IModel<DefinitionScopeDto> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    protected void initLayout() {
        final TextField<?> nameField = new TextField<>(ID_NAME, new PropertyModel<>(getModel(), DefinitionScopeDto.F_NAME));
        nameField.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isEnabled() {
                return true;
            }
        });
        add(nameField);

        final TextArea<?> descriptionField = new TextArea<>(ID_DESCRIPTION, new PropertyModel<>(getModel(),
                DefinitionScopeDto.F_DESCRIPTION));
        descriptionField.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isEnabled() {
                return true;
            }
        });
        add(descriptionField);

        DropDownChoicePanel<?> objectTypeChooser = new DropDownChoicePanel<>(ID_OBJECT_TYPE_CHOOSER,
                new PropertyModel<>(getModel(), DefinitionScopeDto.F_OBJECT_TYPE),
                WebComponentUtil.createReadonlyModelFromEnum(DefinitionScopeObjectType.class),
                new EnumChoiceRenderer<>());
        add(objectTypeChooser);
        add(WebComponentUtil.createHelp(ID_OBJECT_TYPE_HELP));

        TextArea<?> filterTextArea = new TextArea<>(ID_SEARCH_FILTER, new PropertyModel<String>(getModel(),
                DefinitionScopeDto.F_SEARCH_FILTER_TEXT));
        filterTextArea.setOutputMarkupId(true);
        add(filterTextArea);
        add(WebComponentUtil.createHelp(ID_SEARCH_FILTER_HELP));

        add(new CheckBox(ID_INCLUDE_ASSIGNMENTS, new PropertyModel<>(getModel(), DefinitionScopeDto.F_INCLUDE_ASSIGNMENTS)));
        add(new CheckBox(ID_INCLUDE_INDUCEMENTS, new PropertyModel<>(getModel(), DefinitionScopeDto.F_INCLUDE_INDUCEMENTS)));
        add(WebComponentUtil.createHelp(ID_ASSIGNMENTS_INDUCEMENTS_HELP));

        add(new CheckBox(ID_INCLUDE_RESOURCES, new PropertyModel<>(getModel(), DefinitionScopeDto.F_INCLUDE_RESOURCES)));
        add(new CheckBox(ID_INCLUDE_ROLES, new PropertyModel<>(getModel(), DefinitionScopeDto.F_INCLUDE_ROLES)));
        add(new CheckBox(ID_INCLUDE_ORGS, new PropertyModel<>(getModel(), DefinitionScopeDto.F_INCLUDE_ORGS)));
        add(new CheckBox(ID_INCLUDE_SERVICES, new PropertyModel<>(getModel(), DefinitionScopeDto.F_INCLUDE_SERVICES)));
        add(new CheckBox(ID_INCLUDE_USERS, new PropertyModel<>(getModel(), DefinitionScopeDto.F_INCLUDE_USERS)));
        add(WebComponentUtil.createHelp(ID_INCLUDE_TARGET_TYPES_HELP));

        add(new CheckBox(ID_INCLUDE_ENABLED_ITEMS_ONLY, new PropertyModel<>(getModel(),
                DefinitionScopeDto.F_INCLUDE_ENABLED_ITEMS_ONLY)));
        add(WebComponentUtil.createHelp(ID_INCLUDE_BY_STATUS_HELP));

        List<QName> relationsList = WebComponentUtil.getAllRelations(getPageBase());
        relationsList.add(0, new QName(PrismConstants.NS_QUERY, "any"));
        ListMultipleChoicePanel<QName> relationsPanel = new ListMultipleChoicePanel<>(ID_SCOPE_RELATIONS,
                new ListModel<>(getModelObject().getRelationList()),
                new ListModel<>(relationsList), new QNameObjectTypeChoiceRenderer(), null);
        relationsPanel.setOutputMarkupId(true);
        add(relationsPanel);

        add(WebComponentUtil.createHelp(ID_SCOPE_RELATION_HELP));
    }
}
