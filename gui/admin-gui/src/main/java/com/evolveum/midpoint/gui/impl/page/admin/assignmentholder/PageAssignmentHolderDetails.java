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

    protected Collection<CompiledObjectCollectionView> findAllApplicableArchetypeViews() {
        return getCompiledGuiProfile().findAllApplicableArchetypeViews(getType(), OperationTypeType.ADD);
    }


    @Override
    protected AssignmentHolderOperationalButtonsPanel<AH> createButtonsPanel(String id, LoadableModel<PrismObjectWrapper<AH>> wrapperModel) {
        return new AssignmentHolderOperationalButtonsPanel<>(id, wrapperModel) {

            @Override
            protected void refresh(AjaxRequestTarget target) {
                PageAssignmentHolderDetails.this.refresh(target);
            }

            @Override
            protected void savePerformed(AjaxRequestTarget target) {
                PageAssignmentHolderDetails.this.savePerformed(target);
            }

            @Override
            protected boolean hasUnsavedChanges(AjaxRequestTarget target) {
                return PageAssignmentHolderDetails.this.hasUnsavedChanges(target);
            }
        };
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
