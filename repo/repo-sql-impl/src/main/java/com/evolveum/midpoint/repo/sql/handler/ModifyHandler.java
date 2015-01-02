package com.evolveum.midpoint.repo.sql.handler;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.util.Collection;

/**
 * @author lazyman
 *         <p/>
 *         This interface provides abstraction for different modify operation handlers and
 *         can be used for optimization. For example some simple modification operations can be done
 *         without merging full entity state, which should save a few select queries during merge
 *         (in case of complex objects it could be 10-20 select queries).
 *         <p/>
 *         Instances of his interface should be stateless.
 */
public interface ModifyHandler {

    /**
     *
     * @param type
     * @param oid
     * @param modifications
     * @param <T>
     * @return
     */
    <T extends ObjectType> boolean canHandle(Class<T> type, String oid,
                                             Collection<? extends ItemDelta> modifications);

    /**
     *
     * @param type
     * @param oid
     * @param modifications
     * @param result
     * @param <T>
     * @throws ObjectNotFoundException
     */
    <T extends ObjectType> void modifyObject(Class<T> type, String oid,
                                             Collection<? extends ItemDelta> modifications,
                                             OperationResult result) throws ObjectNotFoundException;
}
