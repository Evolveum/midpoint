package com.evolveum.midpoint.ninja.action.worker;

import com.evolveum.midpoint.ninja.action.AbstractRepositorySearchAction;
import com.evolveum.midpoint.ninja.action.ExportOptions;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.util.concurrent.BlockingQueue;

public class ExportConfigurationWorker extends ExportConsumerWorker {

    public ExportConfigurationWorker(NinjaContext context, ExportOptions options, BlockingQueue<ObjectType> queue, OperationStatus operation) {
        super(context, options, queue, operation);
    }

    @Override
    protected boolean shouldSkipObject(PrismObject<? extends ObjectType> prismObject) {
        // FIXME: Allows you to skip exporting object (eg. no interesting information inside)
        // This is called before edit object
        // eg. we can see if object contains mapping / expressions
        return false;

    }

    @Override
    protected void editObject(PrismObject<? extends ObjectType> prismObject) {

        // FIXME here we can manually edit object / remove items we do not want for export
        prismObject.removeItem(ObjectType.F_METADATA, PrismContainer.class);
        prismObject.removeItem(ObjectType.F_OPERATION_EXECUTION, PrismContainer.class);

        // FIXME: maybe we want to remove value metadata (there is probably already some utility method existing)

        if (AssignmentHolderType.class.isAssignableFrom(prismObject.getCompileTimeClass())) {
             prismObject.removeItem(AssignmentHolderType.F_ROLE_MEMBERSHIP_REF, PrismReference.class);
        }

        // Based on object type we can remove additional items, which are not interesting to us
    }
}
