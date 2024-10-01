/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component.assignmentType.assignment;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.FilterableSearchItemWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.abstractrole.PageAbstractRole;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component.AssignmentHolderAssignmentPanel;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import javax.xml.namespace.QName;
import java.util.List;

@PanelType(name = "focusMappingsAssignments")
@PanelInstance(identifier = "focusMappingsAssignments",
        applicableForType = AbstractRoleType.class,
        childOf = AssignmentHolderAssignmentPanel.class,
        display = @PanelDisplay(label = "AssignmentType.focusMappings", order = 70))
public class FocusMappingsAssignmentsPanel<AH extends AssignmentHolderType> extends AbstractAssignmentPanel<AH> {

    private static final Trace LOGGER = TraceManager.getTrace(FocusMappingsAssignmentsPanel.class);

    public FocusMappingsAssignmentsPanel(String id, IModel<PrismObjectWrapper<AH>> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected QName getAssignmentType() {
        return null;
    }

    protected ObjectQuery createCustomizeQuery() {
        return getPageBase().getPrismContext().queryFor(AssignmentType.class)
                .exists(AssignmentType.F_FOCUS_MAPPINGS).build();
    }

    @Override
    protected ObjectQuery getCustomizeQuery() {
        // CustomizeQuery is not repo indexed
        if (isRepositorySearchEnabled()) {
            return null;
        }
        return createCustomizeQuery();
    }

    @Override
    protected List<PrismContainerValueWrapper<AssignmentType>> customPostSearch(
            List<PrismContainerValueWrapper<AssignmentType>> list) {
        // customizeQuery is not repository supported, so we need to prefilter list using in-memory search
        if (isRepositorySearchEnabled()) {
            return prefilterUsingQuery(list, createCustomizeQuery());
        }
        return super.customPostSearch(list);
    }

    @Override
    protected boolean hasTargetObject() {
        return false;
    }

    @Override
    protected void newAssignmentClickPerformed(AjaxRequestTarget target) {
        PrismContainerValue<AssignmentType> newValue = getContainerModel().getObject().getItem().createNewValue();
        try {
            newValue.findOrCreateContainer(AssignmentType.F_FOCUS_MAPPINGS);
            newValue.asContainerable().setFocusMappings(new MappingsType());
        } catch (SchemaException e) {
            LOGGER.error("Cannot create focus mappings inducement: {}", e.getMessage(), e);
            getSession().error("Cannot create focus mappings inducement");
            target.add(getPageBase().getFeedbackPanel());
            return;
        }

        if (getPageBase() instanceof PageAbstractRole) {
            ((PageAbstractRole) getPageBase()).showFocusMappingWizard(newValue, AbstractRoleType.F_ASSIGNMENT, target);
            return;
        }
        super.newAssignmentClickPerformed(target);
    }

    @Override
    protected boolean isCancelButtonVisible() {
        return false;
    }

}
