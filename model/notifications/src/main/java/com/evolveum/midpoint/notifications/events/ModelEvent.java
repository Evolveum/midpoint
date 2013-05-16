package com.evolveum.midpoint.notifications.events;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.EventCategoryType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.EventOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * @author mederly
 */
public class ModelEvent extends Event {

    private ModelContext<? extends UserType, ? extends ShadowType> modelContext;

    public ModelContext<? extends UserType, ? extends ShadowType> getModelContext() {
        return modelContext;
    }

    public void setModelContext(ModelContext<? extends UserType, ? extends ShadowType> modelContext) {
        this.modelContext = modelContext;
    }

    @Override
    public boolean isOperationType(EventOperationType eventOperationType) {
        if (modelContext.getFocusContext() != null && modelContext.getFocusContext().getPrimaryDelta() != null) {
            ObjectDelta primaryDelta = modelContext.getFocusContext().getPrimaryDelta();
            switch (eventOperationType) {
                case ADD: return primaryDelta.isAdd();
                case MODIFY: return primaryDelta.isModify();
                case DELETE: return primaryDelta.isDelete();
                default: throw new IllegalStateException("Unknown EventOperationType: " + eventOperationType);
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean isCategoryType(EventCategoryType eventCategoryType) {
        return eventCategoryType == EventCategoryType.USER_OPERATION;
    }
}
