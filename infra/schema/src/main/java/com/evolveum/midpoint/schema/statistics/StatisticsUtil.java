/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

/**
 * Utility methods related to task statistics (`OperationStatsType`).
 */
public class StatisticsUtil {

    /**
     * Returns display name for given object, e.g. fullName for a user, displayName for a role,
     * and more detailed description for a shadow.
     */
    public static <O extends ObjectType> String getDisplayName(PrismObject<O> object) {
        if (object == null) {
            return null;
        }
        O objectable = object.asObjectable();
        if (objectable instanceof UserType) {
            return PolyString.getOrig(((UserType) objectable).getFullName());
        } else if (objectable instanceof AbstractRoleType) {
            return PolyString.getOrig(((AbstractRoleType) objectable).getDisplayName());
        } else if (objectable instanceof ShadowType) {
            ShadowType shadow = (ShadowType) objectable;
            String objectName = PolyString.getOrig(shadow.getName());
            QName oc = shadow.getObjectClass();
            String ocName = oc != null ? oc.getLocalPart() : null;
            return objectName + " (" + shadow.getKind() + " - " + shadow.getIntent() + " - " + ocName + ")";
        } else {
            return null;
        }
    }

    /**
     * Returns the type name for an object.
     * (This really belongs somewhere else, not here.)
     */
    public static QName getObjectType(ObjectType object, PrismContext prismContext) {
        if (object == null) {
            return null;
        }
        PrismObjectDefinition<?> objectDef = object.asPrismObject().getDefinition();
        if (objectDef != null) {
            return objectDef.getTypeName();
        }
        Class<? extends Objectable> clazz = object.asPrismObject().getCompileTimeClass();
        if (clazz == null) {
            return null;
        }
        PrismObjectDefinition<?> defFromRegistry = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(clazz);
        if (defFromRegistry != null) {
            return defFromRegistry.getTypeName();
        } else {
            return ObjectType.COMPLEX_TYPE;
        }
    }

    public static boolean isEmpty(EnvironmentalPerformanceInformationType info) {
        return info == null ||
                (isEmpty(info.getProvisioningStatistics())
                && isEmpty(info.getMappingsStatistics())
                && isEmpty(info.getNotificationsStatistics())
                && info.getLastMessage() == null
                && info.getLastMessageTimestamp() == null);
    }

    public static boolean isEmpty(NotificationsStatisticsType notificationsStatistics) {
        return notificationsStatistics == null || notificationsStatistics.getEntry().isEmpty();
    }

    public static boolean isEmpty(MappingsStatisticsType mappingsStatistics) {
        return mappingsStatistics == null || mappingsStatistics.getEntry().isEmpty();
    }

    public static boolean isEmpty(ProvisioningStatisticsType provisioningStatistics) {
        return provisioningStatistics == null || provisioningStatistics.getEntry().isEmpty();
    }

    /**
     * Computes a sum of two operation statistics.
     * Returns a modifiable object, independent from the source ones.
     */
    public static OperationStatsType sum(OperationStatsType a, OperationStatsType b) {
        if (a == null) {
            return CloneUtil.clone(b);
        } else {
            OperationStatsType sum = CloneUtil.clone(a);
            addTo(sum, b);
            return sum;
        }
    }

    /**
     * Adds an statistics increment into given aggregate statistic information.
     */
    private static void addTo(@NotNull OperationStatsType aggregate, @Nullable OperationStatsType increment) {
        if (increment == null) {
            return;
        }
        if (increment.getEnvironmentalPerformanceInformation() != null) {
            if (aggregate.getEnvironmentalPerformanceInformation() == null) {
                aggregate.setEnvironmentalPerformanceInformation(new EnvironmentalPerformanceInformationType());
            }
            EnvironmentalPerformanceInformation.addTo(aggregate.getEnvironmentalPerformanceInformation(), increment.getEnvironmentalPerformanceInformation());
        }
        IterativeTaskInformation.addTo(aggregate.getIterativeTaskInformation(), increment.getIterativeTaskInformation());
        if (increment.getSynchronizationInformation() != null) {
            if (aggregate.getSynchronizationInformation() == null) {
                aggregate.setSynchronizationInformation(new SynchronizationInformationType());
            }
            SynchronizationInformation.addTo(aggregate.getSynchronizationInformation(), increment.getSynchronizationInformation());
        }
        if (increment.getActionsExecutedInformation() != null) {
            if (aggregate.getActionsExecutedInformation() == null) {
                aggregate.setActionsExecutedInformation(new ActionsExecutedInformationType());
            }
            ActionsExecutedInformation.addTo(aggregate.getActionsExecutedInformation(), increment.getActionsExecutedInformation());
        }
        if (increment.getRepositoryPerformanceInformation() != null) {
            if (aggregate.getRepositoryPerformanceInformation() == null) {
                aggregate.setRepositoryPerformanceInformation(new RepositoryPerformanceInformationType());
            }
            RepositoryPerformanceInformationUtil.addTo(aggregate.getRepositoryPerformanceInformation(), increment.getRepositoryPerformanceInformation());
        }
        if (increment.getCachesPerformanceInformation() != null) {
            if (aggregate.getCachesPerformanceInformation() == null) {
                aggregate.setCachesPerformanceInformation(new CachesPerformanceInformationType());
            }
            CachePerformanceInformationUtil.addTo(aggregate.getCachesPerformanceInformation(), increment.getCachesPerformanceInformation());
        }
    }

    public static String format(OperationStatsType statistics) {
        if (statistics == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        if (statistics.getIterativeTaskInformation() != null) {
            sb.append("Iterative task information\n\n")
                    .append(IterativeTaskInformation.format(statistics.getIterativeTaskInformation()))
                    .append("\n");
        }
        if (statistics.getActionsExecutedInformation() != null) {
            sb.append("Actions executed\n\n")
                    .append(ActionsExecutedInformation.format(statistics.getActionsExecutedInformation()))
                    .append("\n");
        }
//        if (statistics.getSynchronizationInformation() != null) {
//            sb.append("Synchronization information:\n")
//                    .append(SynchronizationInformation.format(statistics.getSynchronizationInformation()))
//                    .append("\n");
//        }
        if (statistics.getEnvironmentalPerformanceInformation() != null) {
            sb.append("Environmental performance information\n\n")
                    .append(EnvironmentalPerformanceInformation.format(statistics.getEnvironmentalPerformanceInformation()))
                    .append("\n");
        }
        if (statistics.getRepositoryPerformanceInformation() != null) {
            sb.append("Repository performance information\n\n")
                    .append(RepositoryPerformanceInformationUtil.format(statistics.getRepositoryPerformanceInformation()))
                    .append("\n");
        }
        if (statistics.getCachesPerformanceInformation() != null) {
            sb.append("Cache performance information\n\n")
                    .append(CachePerformanceInformationUtil.format(statistics.getCachesPerformanceInformation()))
                    .append("\n");
        }
        if (statistics.getOperationsPerformanceInformation() != null) {
            sb.append("Methods performance information\n\n")
                    .append(OperationsPerformanceInformationUtil.format(statistics.getOperationsPerformanceInformation()))
                    .append("\n");
        }
        return sb.toString();
    }

    public static ItemProcessingOutcomeType getOutcome(ProcessedItemSetType set) {
        if (set != null && set.getOutcome() != null) {
            return set.getOutcome().getOutcome();
        } else {
            return null;
        }
    }
}
