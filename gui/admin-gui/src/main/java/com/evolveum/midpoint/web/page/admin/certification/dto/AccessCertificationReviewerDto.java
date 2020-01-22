/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.certification.dto;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.impl.factory.WrapperContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationReviewerSpecificationType.F_ADDITIONAL_REVIEWER_REF;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationReviewerSpecificationType.F_DEFAULT_REVIEWER_REF;

/**
 * Created by Kate on 15.12.2015.
 */
public class AccessCertificationReviewerDto implements Serializable {
    public static final String F_NAME =  "name";
    public static final String F_DESCRIPTION =  "description";
    public static final String F_USE_TARGET_OWNER =  "useTargetOwner";
    public static final String F_USE_TARGET_APPROVER =  "useTargetApprover";
    public static final String F_USE_OBJECT_OWNER =  "useObjectOwner";
    public static final String F_USE_OBJECT_APPROVER =  "useObjectApprover";
    public static final String F_USE_OBJECT_MANAGER =  "useObjectManager";
    public static final String F_USE_OBJECT_MANAGER_PRESENT = "useObjectManagerPresent";
    public static final String F_REVIEWER_EXPRESSION =  "reviewerExpression";
    public static final String F_DEFAULT_REVIEWERS =  "defaultReviewers";
    public static final String F_ADDITIONAL_REVIEWERS =  "additionalReviewers";

    private String name;
    private String description;
    private boolean useTargetOwner;
    private boolean useTargetApprover;
    private boolean useObjectOwner;
    private boolean useObjectApprover;
    private ManagerSearchDto useObjectManager;
    private boolean useObjectManagerPresent;

    private ItemWrapper defaultReviewers;
    private ItemWrapper additionalReviewers;
    private List<ExpressionType> reviewerExpressionList;

    public AccessCertificationReviewerDto(AccessCertificationReviewerSpecificationType reviewerType, ModelServiceLocator pageBase) throws SchemaException {
        final PrismReference defaultReviewersReference;
        final PrismReference additionalReviewersReference;
        if (reviewerType != null) {
            name = reviewerType.getName();
            description = reviewerType.getDescription();
            useTargetOwner = Boolean.TRUE.equals(reviewerType.isUseTargetOwner());
            useTargetApprover = Boolean.TRUE.equals(reviewerType.isUseTargetApprover());
            useObjectOwner = Boolean.TRUE.equals(reviewerType.isUseObjectOwner());
            useObjectApprover = Boolean.TRUE.equals(reviewerType.isUseObjectApprover());
            useObjectManager = new ManagerSearchDto(reviewerType.getUseObjectManager());
            useObjectManagerPresent = reviewerType.getUseObjectManager() != null;
            reviewerExpressionList = reviewerType.getReviewerExpression();
            defaultReviewersReference = reviewerType.asPrismContainerValue().findOrCreateReference(AccessCertificationReviewerSpecificationType.F_DEFAULT_REVIEWER_REF);
            additionalReviewersReference = reviewerType.asPrismContainerValue().findOrCreateReference(AccessCertificationReviewerSpecificationType.F_ADDITIONAL_REVIEWER_REF);
        } else {
            useObjectManager = new ManagerSearchDto(null);
            useObjectManagerPresent = false;
            PrismReferenceDefinition defReviewerDef = pageBase.getPrismContext().getSchemaRegistry().findItemDefinitionByFullPath(AccessCertificationDefinitionType.class,
                    PrismReferenceDefinition.class,
                    AccessCertificationDefinitionType.F_STAGE_DEFINITION, AccessCertificationStageDefinitionType.F_REVIEWER_SPECIFICATION, F_DEFAULT_REVIEWER_REF);
            defaultReviewersReference = defReviewerDef.instantiate();
            PrismReferenceDefinition additionalReviewerDef = pageBase.getPrismContext().getSchemaRegistry().findItemDefinitionByFullPath(AccessCertificationDefinitionType.class,
                    PrismReferenceDefinition.class,
                    AccessCertificationDefinitionType.F_STAGE_DEFINITION, AccessCertificationStageDefinitionType.F_REVIEWER_SPECIFICATION, F_ADDITIONAL_REVIEWER_REF);
            additionalReviewersReference = additionalReviewerDef.instantiate();
            reviewerExpressionList = new ArrayList<>();
        }


        Task task = pageBase.createSimpleTask("Create reviewers wrappers");
        OperationResult result = task.getResult();
        WrapperContext ctx = new WrapperContext(task, result);
        setDefaultReviewers(pageBase.createItemWrapper(defaultReviewersReference, ItemStatus.NOT_CHANGED, ctx));
        setAdditionalReviewers(pageBase.createItemWrapper(additionalReviewersReference, ItemStatus.NOT_CHANGED, ctx));

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isUseTargetOwner() {
        return useTargetOwner;
    }

    public void setUseTargetOwner(boolean useTargetOwner) {
        this.useTargetOwner = useTargetOwner;
    }

    public boolean isUseTargetApprover() {
        return useTargetApprover;
    }

    public void setUseTargetApprover(boolean useTargetApprover) {
        this.useTargetApprover = useTargetApprover;
    }

    public boolean isUseObjectOwner() {
        return useObjectOwner;
    }

    public void setUseObjectOwner(boolean useObjectOwner) {
        this.useObjectOwner = useObjectOwner;
    }

    public boolean isUseObjectApprover() {
        return useObjectApprover;
    }

    public void setUseObjectApprover(boolean useObjectApprover) {
        this.useObjectApprover = useObjectApprover;
    }

    public boolean isUseObjectManagerPresent() {
        return useObjectManagerPresent;
    }

    public void setUseObjectManagerPresent(boolean useObjectManagerPresent) {
        this.useObjectManagerPresent = useObjectManagerPresent;
    }

    public ManagerSearchDto getUseObjectManager() {
        return useObjectManager;
    }

    public void setUseObjectManager(ManagerSearchDto useObjectManager) {
        this.useObjectManager = useObjectManager;
    }

    public List<ExpressionType> getReviewerExpressionList() {
        return reviewerExpressionList;
    }

    public void setReviewerExpressionList(
            List<ExpressionType> reviewerExpressionList) {
        this.reviewerExpressionList = reviewerExpressionList;
    }


    public ItemWrapper getDefaultReviewers() {
        return defaultReviewers;
    }

    public void setDefaultReviewers(ItemWrapper defaultReviewers) {
        this.defaultReviewers = defaultReviewers;
    }

    public ItemWrapper getAdditionalReviewers() {
        return additionalReviewers;
    }

    public void setAdditionalReviewers(ItemWrapper additionalReviewers) {
        this.additionalReviewers = additionalReviewers;
    }

    public List<ObjectReferenceType> getDefaultReviewersAsObjectReferenceList(PrismContext prismContext) throws SchemaException {
        return ObjectTypeUtil.getAsObjectReferenceTypeList((PrismReference)defaultReviewers.getItem());
    }

    public List<ObjectReferenceType> getAdditionalReviewersAsObjectReferenceList(PrismContext prismContext) throws SchemaException {
        return ObjectTypeUtil.getAsObjectReferenceTypeList((PrismReference) additionalReviewers.getItem());
    }

}
