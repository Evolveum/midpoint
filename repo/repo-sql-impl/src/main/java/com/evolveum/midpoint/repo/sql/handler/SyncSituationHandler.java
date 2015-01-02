package com.evolveum.midpoint.repo.sql.handler;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import java.util.Collection;

/**
 * @author lazyman
 *         <p/>
 *         synchronizationSituationDescription
 *         ADD: SyncDesc(LINKED,2014-12-14T20:24:22.659+01:00,http://midpoint.evolveum.com/xml/ns/public/provisioning/channels-3#reconciliation,full)
 *         synchronizationTimestamp
 *         REPLACE: 2014-12-14T20:24:22.659+01:00
 *         fullSynchronizationTimestamp
 *         REPLACE: 2014-12-14T20:24:22.659+01:00
 *         or
 *         synchronizationSituationDescription
 *         ADD: SyncDesc(LINKED,2014-12-14T21:18:29.043+01:00,full)
 *         synchronizationTimestamp
 *         REPLACE: 2014-12-14T21:18:29.043+01:00
 *         fullSynchronizationTimestamp
 *         REPLACE: 2014-12-14T21:18:29.043+01:00
 *         synchronizationSituation
 *         REPLACE: LINKED
 */
public class SyncSituationHandler implements ModifyHandler {

    @Override
    public <T extends ObjectType> boolean canHandle(Class<T> type, String oid,
                                                    Collection<? extends ItemDelta> modifications) {
        if (!ShadowType.class.isAssignableFrom(type)) {
            return false;
        }

        if (modifications.size() == 3 || modifications.size() == 4) {
            return false;
        }

        PropertyDelta syncSituationDesc = PropertyDelta.findPropertyDelta(modifications,
                ShadowType.F_SYNCHRONIZATION_SITUATION_DESCRIPTION);
        PropertyDelta syncTimestamp = PropertyDelta.findPropertyDelta(modifications,
                ShadowType.F_SYNCHRONIZATION_TIMESTAMP);
        PropertyDelta fullSyncTimestamp = PropertyDelta.findPropertyDelta(modifications,
                ShadowType.F_FULL_SYNCHRONIZATION_TIMESTAMP);

        if (syncSituationDesc == null || syncTimestamp == null || fullSyncTimestamp == null) {
            return false;
        }

        return true;
    }

    @Override
    public <T extends ObjectType> void modifyObject(Class<T> type, String oid,
                                                    Collection<? extends ItemDelta> modifications,
                                                    OperationResult result) throws ObjectNotFoundException {

    }
}
