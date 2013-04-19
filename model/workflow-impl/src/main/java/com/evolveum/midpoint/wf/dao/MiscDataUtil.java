package com.evolveum.midpoint.wf.dao;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author mederly
 */

@Component
public class MiscDataUtil {

    private static final transient Trace LOGGER = TraceManager.getTrace(MiscDataUtil.class);

    @Autowired
    private RepositoryService repositoryService;

    // returns oid when user cannot be retrieved
    String getUserNameByOid(String oid, OperationResult result) {
        try {
            PrismObject<UserType> user = repositoryService.getObject(UserType.class, oid, result);
            return user.asObjectable().getName().getOrig();
        } catch (ObjectNotFoundException e) {
            // there should be a note in result by now
            LoggingUtils.logException(LOGGER, "Couldn't get user {} details because it couldn't be found", e, oid);
            return oid;
        } catch (SchemaException e) {
            // there should be a note in result by now
            LoggingUtils.logException(LOGGER, "Couldn't get user {} details due to schema exception", e, oid);
            return oid;
        }
    }

}
