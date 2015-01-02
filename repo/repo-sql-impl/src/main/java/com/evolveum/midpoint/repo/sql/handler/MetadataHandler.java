package com.evolveum.midpoint.repo.sql.handler;

/**
 * @author lazyman
 */

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.util.Collection;

/**
 * @author lazyman
 *
 *         <p/>
 *         iteration
 *         REPLACE: 0
 *         iterationToken
 *         REPLACE:
 *         metadata/modifyChannel
 *         REPLACE: http://midpoint.evolveum.com/xml/ns/public/provisioning/channels-3#import
 *         metadata/modifyTimestamp
 *         REPLACE: 2014-12-14T21:18:28.932+01:00
 *         metadata/modifierRef
 *         REPLACE: oid=00000000-0000-0000-0000-00000000000
 */
public class MetadataHandler implements ModifyHandler {

    @Override
    public <T extends ObjectType> boolean canHandle(Class<T> type, String oid,
                                                    Collection<? extends ItemDelta> modifications) {
        return false;
    }

    @Override
    public <T extends ObjectType> void modifyObject(Class<T> type, String oid,
                                                    Collection<? extends ItemDelta> modifications,
                                                    OperationResult result) throws ObjectNotFoundException {

    }
}
