/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.resources.dto;

import java.util.List;

import com.evolveum.midpoint.schema.result.OperationResultStatus;

import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.schema.constants.ConnectorTestOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AvailabilityStatusType;

/**
 * @author mserbak
 */
public class ResourceController {

    public static void updateResourceState(ResourceState state, OperationResult result) {
        Validate.notNull(result, "Operation result must not be null.");

        List<OperationResult> subResults = result.getSubresults();
        state.setConConnection(getStatusFromResultType(ConnectorTestOperation.CONNECTOR_CONNECTION,
                subResults));
        state.setConfValidation(getStatusFromResultType(ConnectorTestOperation.CONNECTOR_CONFIGURATION,
                subResults));
        state.setConInitialization(getStatusFromResultType(ConnectorTestOperation.CONNECTOR_INITIALIZATION,
                subResults));
        state.setConSanity(getStatusFromResultType(ConnectorTestOperation.RESOURCE_SANITY, subResults));
        state.setConSchema(getStatusFromResultType(ConnectorTestOperation.RESOURCE_SCHEMA, subResults));
    }

    private static OperationResultStatus getStatusFromResultType(ConnectorTestOperation operation,
                                                          List<OperationResult> results) {
        OperationResultStatus status = OperationResultStatus.UNKNOWN;

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
            case UNKNOWN:
                status = OperationResultStatus.UNKNOWN;
                break;
            case SUCCESS:
                status = OperationResultStatus.SUCCESS;
                break;
            case WARNING:
                status = OperationResultStatus.WARNING;
                break;
            case FATAL_ERROR:
                status = OperationResultStatus.FATAL_ERROR;
                break;
            case PARTIAL_ERROR:
                status = OperationResultStatus.PARTIAL_ERROR;
                break;
            case HANDLED_ERROR:
                status = OperationResultStatus.HANDLED_ERROR;
                break;
            case IN_PROGRESS:
                status = OperationResultStatus.IN_PROGRESS;
                break;
            default:
                status = OperationResultStatus.UNKNOWN;
        }
        return status;
    }

    public static void updateLastAvailabilityState(ResourceState state, AvailabilityStatusType lastAvailabilityStatus) {
        OperationResultStatus lastAvailability = OperationResultStatus.UNKNOWN;

        if (lastAvailabilityStatus == null) {
            if (state.getOverall().equals(OperationResultStatus.SUCCESS)) {
                lastAvailability = OperationResultStatus.SUCCESS;
            } else if ((state.getOverall().equals(OperationResultStatus.PARTIAL_ERROR)
                    || state.getOverall().equals(OperationResultStatus.FATAL_ERROR)
                    || state.getOverall().equals(OperationResultStatus.HANDLED_ERROR))) {
                lastAvailability = OperationResultStatus.PARTIAL_ERROR;
            }
            state.setLastAvailability(lastAvailability);
            return;
        }

        if (state.getOverall().equals(OperationResultStatus.SUCCESS)
                && !lastAvailabilityStatus.equals(AvailabilityStatusType.UP)) {
            lastAvailability = OperationResultStatus.SUCCESS;
        } else if ((state.getOverall().equals(OperationResultStatus.PARTIAL_ERROR)
                || state.getOverall().equals(OperationResultStatus.FATAL_ERROR)
                || state.getOverall().equals(OperationResultStatus.HANDLED_ERROR))
                && !lastAvailabilityStatus.equals(AvailabilityStatusType.DOWN)) {
            lastAvailability = OperationResultStatus.PARTIAL_ERROR;
        }

        if (!lastAvailability.equals(OperationResultStatus.UNKNOWN)) {
            state.setLastAvailability(lastAvailability);
            return;
        }

        switch (lastAvailabilityStatus) {
            case UP:
                lastAvailability = OperationResultStatus.SUCCESS;
                break;
            case DOWN:
                lastAvailability = OperationResultStatus.PARTIAL_ERROR;
                break;
        }
        state.setLastAvailability(lastAvailability);
    }
}
