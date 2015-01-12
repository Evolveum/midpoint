package com.evolveum.midpoint.repo.sql.handler;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.util.Collection;

/**
 * @author lazyman
 */
public class DefaultModifyHandler implements ModifyHandler {

    @Override
    public <T extends ObjectType> boolean canHandle(Class<T> type, String oid,
                                                    Collection<? extends ItemDelta> modifications) {
        return true;
    }

    @Override
    public <T extends ObjectType> void modifyObject(Class<T> type, String oid,
                                                    Collection<? extends ItemDelta> modifications,
                                                    OperationResult result) throws ObjectNotFoundException {

    }
}
