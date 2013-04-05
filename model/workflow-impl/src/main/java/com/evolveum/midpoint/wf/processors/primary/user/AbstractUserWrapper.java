package com.evolveum.midpoint.wf.processors.primary.user;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.processors.primary.PrimaryApprovalProcessWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Pavol
 */
public abstract class AbstractUserWrapper extends AbstractWrapper {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractUserWrapper.class);

    String getUserName(ModelContext<?,?> modelContext) {
        ModelElementContext<UserType> fc = (ModelElementContext<UserType>) modelContext.getFocusContext();
        UserType newUser = fc.getObjectNew() != null ? fc.getObjectNew().asObjectable() : null;

        Validate.notNull(newUser);
        Validate.notNull(newUser.getName());
        return newUser.getName().getOrig();
    }


}
