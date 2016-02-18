package com.evolveum.midpoint.web.page.admin.certification.dto;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ReferenceWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import java.io.Serializable;
import java.util.List;

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
    private ReferenceWrapper defaultReviewers;
    private ReferenceWrapper additionalReviewers;

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

    public ManagerSearchDto getUseObjectManager() {
        return useObjectManager;
    }

    public void setUseObjectManager(ManagerSearchDto useObjectManager) {
        this.useObjectManager = useObjectManager;
    }

    public ReferenceWrapper getDefaultReviewers() {
        return defaultReviewers;
    }

    public void setDefaultReviewers(ReferenceWrapper defaultReviewers) {
        this.defaultReviewers = defaultReviewers;
    }

    public ReferenceWrapper getAdditionalReviewers() {
        return additionalReviewers;
    }

    public void setAdditionalReviewers(ReferenceWrapper additionalReviewers) {
        this.additionalReviewers = additionalReviewers;
    }

    public List<ObjectReferenceType> getDefaultReviewersAsObjectReferenceList(PrismContext prismContext) throws SchemaException {
        return ObjectTypeUtil.getAsObjectReferenceTypeList(defaultReviewers.getUpdatedItem(prismContext));
    }

    public List<ObjectReferenceType> getAdditionalReviewersAsObjectReferenceList(PrismContext prismContext) throws SchemaException {
        return ObjectTypeUtil.getAsObjectReferenceTypeList(additionalReviewers.getUpdatedItem(prismContext));
    }

}
