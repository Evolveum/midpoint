/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.resources.dto;

import java.util.List;

import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.schema.constants.ConnectorTestOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AvailabilityStatusType;

/**
 * @author mserbak
 */
public class ResourceController {

    public static void updateResourceState(ResourceState state, OperationResult result) {
        Validate.notNull(result, "Operation result must not be null.");

        List<OperationResult> subResults = result.getSubresults();
        state.setConConnection(getStatusFromResultType(ConnectorTestOperation.CONNECTOR_CONNECTION,
                subResults));
        state.setConfValidation(getStatusFromResultType(ConnectorTestOperation.CONFIGURATION_VALIDATION,
                subResults));
        state.setConInitialization(getStatusFromResultType(ConnectorTestOperation.CONNECTOR_INITIALIZATION,
                subResults));
        state.setConSanity(getStatusFromResultType(ConnectorTestOperation.CONNECTOR_SANITY, subResults));
        state.setConSchema(getStatusFromResultType(ConnectorTestOperation.CONNECTOR_SCHEMA, subResults));
    }

    private static ResourceStatus getStatusFromResultType(ConnectorTestOperation operation,
                                                          List<OperationResult> results) {
        ResourceStatus status = ResourceStatus.NOT_TESTED;

        OperationResult resultFound = null;
        for (OperationResult result : results) {
            try {
                if (operation.getOperation().equals(result.getOperation())) {
                    resultFound = result;
                    break;
                }
            } catch (IllegalArgumentException ex) {
                //result.recordFatalError("Result operation name " + result.getOperation() + " returned from test connection is not type of " + ConnectorTestOperation.class + ".", ex);
            }
        }

        if (resultFound == null) {
            return status;
        }

        switch (resultFound.getStatus()) {
            case SUCCESS:
                status = ResourceStatus.SUCCESS;
                break;
            case WARNING:
                status = ResourceStatus.WARNING;
                break;
            case FATAL_ERROR:
            case PARTIAL_ERROR:
                status = ResourceStatus.ERROR;
                break;
            default:
                status = ResourceStatus.NOT_TESTED;
        }
        return status;
    }

    public static void updateLastAvailabilityState(ResourceState state, AvailabilityStatusType lastAvailabilityStatus) {
        ResourceStatus lastAvailability = ResourceStatus.NOT_TESTED;

        if (lastAvailabilityStatus == null) {
            if (state.getOverall().equals(ResourceStatus.SUCCESS)) {
                lastAvailability = ResourceStatus.SUCCESS;
            } else if (state.getOverall().equals(ResourceStatus.ERROR)) {
                lastAvailability = ResourceStatus.ERROR;
            }
            state.setLastAvailability(lastAvailability);
            return;
        }

        if (state.getOverall().equals(ResourceStatus.SUCCESS)
                && !lastAvailabilityStatus.equals(AvailabilityStatusType.UP)) {
            lastAvailability = ResourceStatus.SUCCESS;
        } else if (state.getOverall().equals(ResourceStatus.ERROR)
                && !lastAvailabilityStatus.equals(AvailabilityStatusType.DOWN)) {
            lastAvailability = ResourceStatus.ERROR;
        }

        if (!lastAvailability.equals(ResourceStatus.NOT_TESTED)) {
            state.setLastAvailability(lastAvailability);
            return;
        }

        switch (lastAvailabilityStatus) {
            case UP:
                lastAvailability = ResourceStatus.SUCCESS;
                break;
            case DOWN:
                lastAvailability = ResourceStatus.ERROR;
                break;
        }
        state.setLastAvailability(lastAvailability);
    }
}
