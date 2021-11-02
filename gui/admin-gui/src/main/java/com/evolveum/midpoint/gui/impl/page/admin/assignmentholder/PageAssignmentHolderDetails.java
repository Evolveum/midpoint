/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.assignmentholder;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractPageObjectDetails;
import com.evolveum.midpoint.gui.impl.page.admin.CreateTemplatePanel;
import com.evolveum.midpoint.gui.impl.page.admin.component.AssignmentHolderOperationalButtonsPanel;
import com.evolveum.midpoint.gui.impl.util.ObjectCollectionViewUtil;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

public abstract class PageAssignmentHolderDetails<AH extends AssignmentHolderType, AHDM extends AssignmentHolderDetailsModel<AH>> extends AbstractPageObjectDetails<AH, AHDM> {

    private static final Trace LOGGER = TraceManager.getTrace(PageAssignmentHolderDetails.class);
    private static final String ID_TEMPLATE_VIEW = "templateView";
    private static final String ID_TEMPLATE = "template";

    public PageAssignmentHolderDetails() {
        super();
    }

    public PageAssignmentHolderDetails(PageParameters pageParameters) {
        super(pageParameters);
    }

    public PageAssignmentHolderDetails(PrismObject<AH> assignmentHolder) {
        super(assignmentHolder);
    }

    @Override
    protected void initLayout() {
        Collection<CompiledObjectCollectionView> applicableArchetypes = findAllApplicableArchetypeViews();
        if (isAdd() && applicableArchetypes.size() > 1) {
            TemplateFragment templateFragment = new TemplateFragment(ID_DETAILS_VIEW, ID_TEMPLATE_VIEW, PageAssignmentHolderDetails.this);
            add(templateFragment);
        } else {
            super.initLayout();
        }
    }

    class TemplateFragment extends Fragment {

        public TemplateFragment(String id, String markupId, MarkupContainer markupProvider) {
            super(id, markupId, markupProvider);
            setOutputMarkupId(true);
        }

        @Override
        protected void onInitialize() {
            super.onInitialize();
            initTemplateLayout();
        }

        private void initTemplateLayout() {
            add(createTemplatePanel());
        }
    }

    private CreateTemplatePanel<AH> createTemplatePanel() {
        return new CreateTemplatePanel<>(ID_TEMPLATE) {

            @Override
            protected Collection<CompiledObjectCollectionView> findAllApplicableArchetypeViews() {
                return PageAssignmentHolderDetails.this.findAllApplicableArchetypeViews();
            }

            @Override
            protected QName getType() {
                return ObjectTypes.getObjectType(PageAssignmentHolderDetails.this.getType()).getTypeQName();
            }

            @Override
            protected void onTemplateChosePerformed(CompiledObjectCollectionView collectionViews, AjaxRequestTarget target) {
                PrismObject<AH> assignmentHolder;
                try {
                    assignmentHolder = getPrismContext().createObject(PageAssignmentHolderDetails.this.getType());
                } catch (Throwable e) {
                    LOGGER.error("Cannot create prism object for {}. Using object from page model.", PageAssignmentHolderDetails.this.getType());
                    assignmentHolder = getObjectDetailsModels().getObjectWrapperModel().getObject().getObjectOld().clone();
                }
                List<ObjectReferenceType> archetypeRef = ObjectCollectionViewUtil.getArchetypeReferencesList(collectionViews);
                if (archetypeRef != null) {
                    AssignmentHolderType holder = assignmentHolder.asObjectable();
                    archetypeRef.forEach(a -> holder.getAssignment().add(ObjectTypeUtil.createAssignmentTo(a, getPrismContext())));

                }

                reloadObjectDetailsModel(assignmentHolder);
                Fragment fragment = createDetailsFragment();
                fragment.setOutputMarkupId(true);
                PageAssignmentHolderDetails.this.replace(fragment);
                target.add(fragment);
            }
        };
    }

    private Collection<CompiledObjectCollectionView> findAllApplicableArchetypeViews() {
        return getCompiledGuiProfile().findAllApplicableArchetypeViews(getType(), OperationTypeType.ADD);
    }


    @Override
    protected AssignmentHolderOperationalButtonsPanel<AH> createButtonsPanel(String id, LoadableModel<PrismObjectWrapper<AH>> wrapperModel) {
        return new AssignmentHolderOperationalButtonsPanel<>(id, wrapperModel) {

            @Override
            protected void addArchetypePerformed(AjaxRequestTarget target, List<AssignmentType> newAssignmentsList) {
                OperationResult result = new OperationResult(OPERATION_EXECUTE_ARCHETYPE_CHANGES);
                if (newAssignmentsList.size() > 1) {
                    result.recordWarning(getString("PageAdminObjectDetails.change.archetype.more.than.one.selected"));
                    getPageBase().showResult(result);
                    target.add(getPageBase().getFeedbackPanel());
                    return;
                }

                AssignmentType oldArchetypAssignment = getOldArchetypeAssignment(result);
                if (oldArchetypAssignment == null) {
                    getPageBase().showResult(result);
                    target.add(getPageBase().getFeedbackPanel());
                    return;
                }

                changeArchetype(oldArchetypAssignment, newAssignmentsList, result, target);
            }

            @Override
            protected void savePerformed(AjaxRequestTarget target) {
                PageAssignmentHolderDetails.this.savePerformed(target);
            }
        };
    }

    private void changeArchetype(AssignmentType oldArchetypAssignment, List<AssignmentType> newAssignmentsList, OperationResult result, AjaxRequestTarget target) {
        try {
            ObjectDelta<AH> delta = getPrismContext().deltaFor(getModelPrismObject().getCompileTimeClass())
                    .item(AssignmentHolderType.F_ASSIGNMENT)
                    .delete(oldArchetypAssignment.clone())
                    .asObjectDelta(getModelPrismObject().getOid());
            delta.addModificationAddContainer(AssignmentHolderType.F_ASSIGNMENT, newAssignmentsList.iterator().next());

            Task task = createSimpleTask(OPERATION_EXECUTE_ARCHETYPE_CHANGES);
            getModelService().executeChanges(MiscUtil.createCollection(delta), null, task, result);

        } catch (Exception e) {
            LOGGER.error("Cannot find assignment wrapper: {}", e.getMessage(), e);
            result.recordFatalError(getString("PageAdminObjectDetails.change.archetype.failed", e.getMessage()), e);

        }
        result.computeStatusIfUnknown();
        showResult(result);
        target.add(getFeedbackPanel());
        PageAssignmentHolderDetails.this.refresh(target);
    }

    private AssignmentType getOldArchetypeAssignment(OperationResult result) {
        PrismContainer<AssignmentType> assignmentContainer = getModelWrapperObject().getObjectOld().findContainer(AssignmentHolderType.F_ASSIGNMENT);
        if (assignmentContainer == null) {
            //should not happen either
            result.recordWarning(getString("PageAdminObjectDetails.archetype.change.not.supported"));
            return null;
        }

        List<AssignmentType> oldAssignments = assignmentContainer.getRealValues().stream().filter(WebComponentUtil::isArchetypeAssignment).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(oldAssignments)) {
            result.recordWarning(getString("PageAdminObjectDetails.archetype.change.not.supported"));
            return null;
        }

        if (oldAssignments.size() > 1) {
            result.recordFatalError(getString("PageAdminObjectDetails.archetype.change.no.single.archetype"));
            return null;
        }
        return oldAssignments.iterator().next();
    }

    protected AHDM createObjectDetailsModels(PrismObject<AH> object) {
        return (AHDM) new AssignmentHolderDetailsModel<>(createPrismObjectModel(object), this);
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        String objectCollectionName = getObjectCollectionName();
        if (objectCollectionName != null) {
            return () -> {
                if (getObjectDetailsModels() != null && getObjectDetailsModels().getObjectStatus() == ItemStatus.ADDED) {
                    return createStringResource("PageAdminObjectDetails.title.new", objectCollectionName).getString();
                }

                String name = null;
                if (getModelWrapperObject() != null && getModelWrapperObject().getObject() != null) {
                    name = WebComponentUtil.getName(getModelWrapperObject().getObject());
                }

                return createStringResource("PageAdminObjectDetails.title.edit.readonly.${readOnly}", getModel(), objectCollectionName, name).getString();
            };
        }

        return super.createPageTitleModel();
    }

    private String getObjectCollectionName() {
        if (getModelWrapperObject() == null || getModelWrapperObject().getObject() == null) {
            return null;
        }

        PrismObject<AH> assignmentHolderObj = getModelWrapperObject().getObject();
        DisplayType displayType = GuiDisplayTypeUtil.getArchetypePolicyDisplayType(assignmentHolderObj, PageAssignmentHolderDetails.this);
        if (displayType == null || displayType.getLabel() == null) {
            return null;
        }

        String archetypeLocalizedName = getLocalizationService()
                .translate(displayType.getLabel().toPolyString(), WebComponentUtil.getCurrentLocale(), true);
        if (StringUtils.isNotEmpty(archetypeLocalizedName)) {
            return archetypeLocalizedName;
        }

        return null;
    }

}
