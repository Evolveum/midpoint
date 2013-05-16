package com.evolveum.midpoint.notifications.events;

import com.evolveum.midpoint.notifications.events.Event;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.provisioning.api.ResourceOperationDescription;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.EventCategoryType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.EventOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * @author mederly
 */
public class AccountEvent extends Event {

    private UserType accountOwner;

    private ResourceOperationDescription accountOperationDescription;

    private ChangeType changeType;

    // the following two are currently unused
    private boolean activationRequested;
    private boolean deactivationRequested;

    public ResourceOperationDescription getAccountOperationDescription() {
        return accountOperationDescription;
    }

    public void setAccountOperationDescription(ResourceOperationDescription accountOperationDescription) {
        this.accountOperationDescription = accountOperationDescription;
    }

    public boolean isActivationRequested() {
        return activationRequested;
    }

    public void setActivationRequested(boolean activationRequested) {
        this.activationRequested = activationRequested;
    }

    public boolean isDeactivationRequested() {
        return deactivationRequested;
    }

    public void setDeactivationRequested(boolean deactivationRequested) {
        this.deactivationRequested = deactivationRequested;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public void setChangeType(ChangeType changeType) {
        this.changeType = changeType;
    }

    public UserType getAccountOwner() {
        return accountOwner;
    }

    public void setAccountOwner(UserType accountOwner) {
        this.accountOwner = accountOwner;
    }

    @Override
    public boolean isOperationType(EventOperationType eventOperationType) {
        switch (eventOperationType) {
            case ADD: return changeType == ChangeType.ADD;
            case MODIFY: return changeType == ChangeType.MODIFY;
            case DELETE: return changeType == ChangeType.DELETE;
            default: throw new IllegalStateException("Unexpected EventOperationType: " + eventOperationType);
        }
    }

    @Override
    public boolean isCategoryType(EventCategoryType eventCategoryType) {
        return eventCategoryType == EventCategoryType.ACCOUNT_OPERATION
                && ShadowUtil.isAccount(accountOperationDescription.getCurrentShadow().asObjectable());
    }

    @Override
    public String toString() {
        return "AccountEvent{" +
                "base=" + super.toString() +
                ", accountOwner=" + accountOwner +
                ", changeType=" + changeType +
                '}';
    }
}
