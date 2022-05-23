/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resources;

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AvailabilityStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationalStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * Manages resource operational state record and its history.
 */
@Component
public class ResourceOperationalStateManager {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceOperationalStateManager.class);

    // TODO make this configurable
    private static final int MAX_OPERATIONAL_HISTORY_SIZE = 5;

    @Autowired private PrismContext prismContext;
    @Autowired private TaskManager taskManager;
    @Autowired private Clock clock;

    List<ItemDelta<?, ?>> createAndLogOperationalStateDeltas(
            AvailabilityStatusType previousStatus,
            AvailabilityStatusType newStatus,
            String resourceDesc,
            String statusChangeReason,
            ResourceType resource) throws SchemaException {

        String stateChangeClause = getChangeClauseAndLogState(previousStatus, newStatus, resourceDesc, statusChangeReason);

        OperationalStateType changeStateRecord = createNewOperationalState(newStatus, stateChangeClause, statusChangeReason);

        List<ItemDelta<?, ?>> deltas = new ArrayList<>();
        deltas.add(createOperationalStateDelta(changeStateRecord.clone()));
        if (resource != null) {
            deltas.addAll(createHistoryCleanupDeltas(resource));
        } else {
            // no resource loaded -> no history cleanup can be done
        }
        deltas.add(createHistoryAddDelta(changeStateRecord));
        return deltas;
    }

    OperationalStateType createAndLogOperationalState(AvailabilityStatusType previousStatus,
            AvailabilityStatusType newStatus, String resourceDesc, String statusChangeReason) {

        String stateChangeClause = getChangeClauseAndLogState(previousStatus, newStatus, resourceDesc, statusChangeReason);
        return createNewOperationalState(newStatus, stateChangeClause, statusChangeReason);
    }

    private OperationalStateType createNewOperationalState(
            AvailabilityStatusType newStatus, String stateChangeClause, String statusChangeReason) {
        return new OperationalStateType()
                .lastAvailabilityStatus(newStatus)
                .message("Status " + stateChangeClause + " because " + statusChangeReason)
                .nodeId(taskManager.getNodeId())
                .timestamp(clock.currentTimeXMLGregorianCalendar());
    }

    private String getChangeClauseAndLogState(
            AvailabilityStatusType previousStatus,
            AvailabilityStatusType newStatus,
            String resourceDesc,
            String statusChangeReason) {
        String stateChangeClause;
        if (previousStatus != null) {
            stateChangeClause = "changed from " + previousStatus + " to " + newStatus;
        } else {
            stateChangeClause = "set to " + newStatus;
        }

        // The level is INFO because it's needed for diagnosing the issues with resource availability.
        LOGGER.info("Availability status {} for {} because {}", stateChangeClause, resourceDesc, statusChangeReason);
        return stateChangeClause;
    }

    private ItemDelta<?, ?> createOperationalStateDelta(OperationalStateType newState) throws SchemaException {
        return prismContext.deltaFor(ResourceType.class)
                .item(ResourceType.F_OPERATIONAL_STATE).replace(newState)
                .asItemDelta();
    }

    private ItemDelta<?, ?> createHistoryAddDelta(OperationalStateType newState) throws SchemaException {
        return prismContext.deltaFor(ResourceType.class)
                .item(ResourceType.F_OPERATIONAL_STATE_HISTORY).add(newState)
                .asItemDelta();
    }

    @SuppressWarnings("SameParameterValue")
    ItemDelta<?, ?> createAvailabilityStatusDelta(AvailabilityStatusType status) throws SchemaException {
        return prismContext.deltaFor(ResourceType.class)
                .item(SchemaConstants.PATH_OPERATIONAL_STATE_LAST_AVAILABILITY_STATUS).replace(status)
                .asItemDelta();
    }

    private List<ItemDelta<?, ?>> createHistoryCleanupDeltas(ResourceType resource) throws SchemaException {
        List<OperationalStateType> history = new ArrayList<>(resource.getOperationalStateHistory());
        int historySize = history.size();
        if (historySize >= MAX_OPERATIONAL_HISTORY_SIZE) {
            history.sort(Comparator.comparing(state -> XmlTypeConverter.toMillis(state.getTimestamp())));
            int numberOfRecordsToDelete = historySize - MAX_OPERATIONAL_HISTORY_SIZE + 1;
            List<OperationalStateType> recordsToDelete = history.subList(0, numberOfRecordsToDelete);
            return prismContext.deltaFor(ResourceType.class)
                    .item(ResourceType.F_OPERATIONAL_STATE_HISTORY)
                    .deleteRealValues(CloneUtil.cloneCollectionMembers(recordsToDelete))
                    .asItemDeltas();
        } else {
            return emptyList();
        }
    }
}
