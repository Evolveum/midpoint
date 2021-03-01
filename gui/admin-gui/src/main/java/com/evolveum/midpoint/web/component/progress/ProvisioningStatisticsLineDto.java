/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.progress;

import com.evolveum.midpoint.schema.statistics.EnvironmentalPerformanceInformation;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Pavol Mederly
 */
public class ProvisioningStatisticsLineDto {

//    public static final String F_RESOURCE = "resource";
//    public static final String F_OBJECT_CLASS = "objectClass";
//    public static final String F_GET_SUCCESS = "getSuccess";
//    public static final String F_GET_FAILURE = "getFailure";
//    public static final String F_SEARCH_SUCCESS = "searchSuccess";
//    public static final String F_SEARCH_FAILURE = "searchFailure";
//    public static final String F_CREATE_SUCCESS = "createSuccess";
//    public static final String F_CREATE_FAILURE = "createFailure";
//    public static final String F_UPDATE_SUCCESS = "updateSuccess";
//    public static final String F_UPDATE_FAILURE = "updateFailure";
//    public static final String F_DELETE_SUCCESS = "deleteSuccess";
//    public static final String F_DELETE_FAILURE = "deleteFailure";
//    public static final String F_SYNC_SUCCESS = "syncSuccess";
//    public static final String F_SYNC_FAILURE = "syncFailure";
//    public static final String F_SCRIPT_SUCCESS = "scriptSuccess";
//    public static final String F_SCRIPT_FAILURE = "scriptFailure";
//    public static final String F_OTHER_SUCCESS = "otherSuccess";
//    public static final String F_OTHER_FAILURE = "otherFailure";
//    public static final String F_TOTAL_OPERATIONS_COUNT = "totalOperationsCount";
//    public static final String F_AVERAGE_TIME = "averageTime";
//    public static final String F_MIN_TIME = "minTime";
//    public static final String F_MAX_TIME = "maxTime";
//    public static final String F_TOTAL_TIME = "totalTime";

//    private String resource;
//    private QName objectClass;
//    private int getSuccess;
//    private int getFailure;
//    private int searchSuccess;
//    private int searchFailure;
//    private int createSuccess;
//    private int createFailure;
//    private int updateSuccess;
//    private int updateFailure;
//    private int deleteSuccess;
//    private int deleteFailure;
//    private int syncSuccess;
//    private int syncFailure;
//    private int scriptSuccess;
//    private int scriptFailure;
//    private int otherSuccess;
//    private int otherFailure;
//
//    private Long minTime;
//    private Long maxTime;
//    private long totalTime;

    public static final String F_RESOURCE_REF = "resourceRef";
    public static final String F_OBJECT_CLASS = "objectClass";
    public static final String F_OPERATIONS = "operations";

    private ObjectReferenceType resourceRef;
    private QName objectClass;
    private List<ProvisioningStatisticsOperationEntryType> operations;

    public ProvisioningStatisticsLineDto(ProvisioningStatisticsEntryType entry) {

        this.resourceRef = entry.getResourceRef();
        this.objectClass = entry.getObjectClass();
        this.operations = entry.getOperation();



//        resource = entry.getResource();
//        objectClass = entry.getObjectClass();
//        getSuccess = entry.getGetSuccess();
//        getFailure = entry.getGetFailure();
//        searchSuccess = entry.getSearchSuccess();
//        searchFailure = entry.getSearchFailure();
//        createSuccess = entry.getCreateSuccess();
//        createFailure = entry.getCreateFailure();
//        updateSuccess = entry.getUpdateSuccess();
//        updateFailure = entry.getUpdateFailure();
//        deleteSuccess = entry.getDeleteSuccess();
//        deleteFailure = entry.getDeleteFailure();
//        syncSuccess = entry.getSyncSuccess();
//        syncFailure = entry.getSyncFailure();
//        scriptSuccess = entry.getScriptSuccess();
//        scriptFailure = entry.getScriptFailure();
//        otherSuccess = entry.getOtherSuccess();
//        otherFailure = entry.getOtherFailure();
//
//        minTime = entry.getMinTime();
//        maxTime = entry.getMaxTime();
//        totalTime = entry.getTotalTime();
    }

    public ObjectReferenceType getResourceRef() {
        return resourceRef;
    }

    //    public ProvisioningStatisticsLineDto(String resource, String objectClass) {
//        this.resource = resource;
//        this.objectClass = objectClass;
//    }

//    public String getResource() {
//        return resource;
//    }
//
//    public String getObjectClass() {
//        return objectClass != null ? objectClass.getLocalPart() : null;
//    }
//
//    public int getGetSuccess() {
//        return getSuccess;
//    }
//
//    public int getGetFailure() {
//        return getFailure;
//    }
//
//    public int getSearchSuccess() {
//        return searchSuccess;
//    }
//
//    public int getSearchFailure() {
//        return searchFailure;
//    }
//
//    public int getCreateSuccess() {
//        return createSuccess;
//    }
//
//    public int getCreateFailure() {
//        return createFailure;
//    }
//
//    public int getUpdateSuccess() {
//        return updateSuccess;
//    }
//
//    public int getUpdateFailure() {
//        return updateFailure;
//    }
//
//    public int getDeleteSuccess() {
//        return deleteSuccess;
//    }
//
//    public int getDeleteFailure() {
//        return deleteFailure;
//    }
//
//    public int getSyncSuccess() {
//        return syncSuccess;
//    }
//
//    public int getSyncFailure() {
//        return syncFailure;
//    }
//
//    public int getScriptSuccess() {
//        return scriptSuccess;
//    }
//
//    public int getScriptFailure() {
//        return scriptFailure;
//    }
//
//    public int getOtherSuccess() {
//        return otherSuccess;
//    }
//
//    public int getOtherFailure() {
//        return otherFailure;
//    }

//    public int getTotalOperationsCount() {
//        return getSuccess + getFailure + searchSuccess + searchFailure +
//                createSuccess + createFailure + updateSuccess + updateFailure + deleteSuccess + deleteFailure +
//                syncSuccess + syncFailure + scriptSuccess + scriptFailure + otherSuccess + otherFailure;
//    }
//
//    public Long getAverageTime() {
//        int totalCount = getTotalOperationsCount();
//        if (totalCount > 0) {
//            return totalTime / totalCount;
//        } else {
//            return null;
//        }
//    }
//
//    public Long getMinTime() {
//        return minTime;
//    }
//
//    public Long getMaxTime() {
//        return maxTime;
//    }
//
//    public long getTotalTime() {
//        return totalTime;
//    }


    public static List<ProvisioningStatisticsLineDto> extractFromOperationalInformation(EnvironmentalPerformanceInformation environmentalPerformanceInformation) {
        EnvironmentalPerformanceInformationType environmentalPerformanceInformationType = environmentalPerformanceInformation.getValueCopy();
        ProvisioningStatisticsType provisioningStatisticsType = environmentalPerformanceInformationType.getProvisioningStatistics();
        return extractFromOperationalInformation(provisioningStatisticsType);
    }

    public static List<ProvisioningStatisticsLineDto> extractFromOperationalInformation(ProvisioningStatisticsType provisioningStatisticsType) {
        List<ProvisioningStatisticsLineDto> retval = new ArrayList<>();
        if (provisioningStatisticsType == null) {
            return retval;
        }
        for (ProvisioningStatisticsEntryType entry : provisioningStatisticsType.getEntry()) {
            retval.add(new ProvisioningStatisticsLineDto(entry));
        }
        return retval;
    }

//    private static ProvisioningStatisticsLineDto findLineDto(List<ProvisioningStatisticsLineDto> list, String resource, String objectClass) {
//        for (ProvisioningStatisticsLineDto lineDto : list) {
//            if (StringUtils.equals(lineDto.getResource(), resource) && StringUtils.equals(lineDto.getObjectClass(), objectClass)) {
//                return lineDto;
//            }
//        }
//        return null;
//    }
//
//    private void setValue(ProvisioningOperation operation, ProvisioningStatusType statusType, int count, int min, int max, long totalDuration) {
//        switch (operation) {
//            case ICF_GET:
//                if (statusType == ProvisioningStatusType.SUCCESS) {
//                    getSuccess+=count;
//                } else {
//                    getFailure+=count;
//                }
//                break;
//            case ICF_SEARCH:
//                if (statusType == ProvisioningStatusType.SUCCESS) {
//                    searchSuccess+=count;
//                } else {
//                    searchFailure+=count;
//                }
//                break;
//            case ICF_CREATE:
//                if (statusType == ProvisioningStatusType.SUCCESS) {
//                    createSuccess+=count;
//                } else {
//                    createFailure+=count;
//                }
//                break;
//            case ICF_UPDATE:
//                if (statusType == ProvisioningStatusType.SUCCESS) {
//                    updateSuccess+=count;
//                } else {
//                    updateFailure+=count;
//                }
//                break;
//            case ICF_DELETE:
//                if (statusType == ProvisioningStatusType.SUCCESS) {
//                    deleteSuccess+=count;
//                } else {
//                    deleteFailure+=count;
//                }
//                break;
//            case ICF_SYNC:
//                if (statusType == ProvisioningStatusType.SUCCESS) {
//                    syncSuccess+=count;
//                } else {
//                    syncFailure+=count;
//                }
//                break;
//            case ICF_SCRIPT:
//                if (statusType == ProvisioningStatusType.SUCCESS) {
//                    scriptSuccess+=count;
//                } else {
//                    scriptFailure+=count;
//                }
//                break;
//            case ICF_GET_LATEST_SYNC_TOKEN:
//            case ICF_GET_SCHEMA:
//                if (statusType == ProvisioningStatusType.SUCCESS) {
//                    otherSuccess+=count;
//                } else {
//                    otherFailure+=count;
//                }
//                break;
//            default:
//                throw new IllegalArgumentException("Illegal operation name: " + operation);
//        }
//        if (minTime == null || min < minTime) {
//            minTime = min;
//        }
//        if (maxTime == null || max > maxTime) {
//            maxTime = max;
//        }
//        totalTime += totalDuration;
//    }
}
