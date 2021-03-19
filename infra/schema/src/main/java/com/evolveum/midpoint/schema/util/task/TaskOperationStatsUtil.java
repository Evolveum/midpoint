/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.statistics.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.evolveum.midpoint.util.MiscUtil.or0;

import static java.util.Collections.singleton;

/**
 * Utility methods related to task operation statistics.
 */
public class TaskOperationStatsUtil {

    /**
     * Returns the number of item processing failures from this task and its subtasks.
     * Subtasks must be resolved into to full objects.
     *
     * TODO Avoid useless statistics aggregation (avoid "first aggregating, then selecting failures")
     */
    public static int getItemsProcessedWithFailureFromTree(TaskType task, PrismContext prismContext) {
        OperationStatsType stats = getOperationStatsFromTree(task, prismContext);
        return getItemsProcessedWithFailure(stats);
    }

    public static int getItemsProcessedWithFailure(TaskType task) {
        return getItemsProcessedWithFailure(task.getOperationStats());
    }

    public static int getItemsProcessedWithFailure(OperationStatsType stats) {
        return stats != null ? getItemsProcessedWithFailure(stats.getIterativeTaskInformation()) : 0;
    }

    public static int getItemsProcessedWithFailure(IterativeTaskInformationType info) {
        if (info != null) {
            return getCounts(info.getPart(), OutcomeKeyedCounterTypeUtil::isFailure);
        } else {
            return 0;
        }
    }

    public static int getItemsProcessedWithSuccess(TaskType task) {
        return getItemsProcessedWithSuccess(task.getOperationStats());
    }

    public static int getItemsProcessedWithSuccess(OperationStatsType stats) {
        return stats != null ? getItemsProcessedWithSuccess(stats.getIterativeTaskInformation()) : 0;
    }

    public static int getItemsProcessedWithSuccess(IterativeTaskInformationType info) {
        if (info != null) {
            return getCounts(info.getPart(), OutcomeKeyedCounterTypeUtil::isSuccess);
        } else {
            return 0;
        }
    }

    public static int getItemsProcessedWithSkip(IterativeTaskInformationType info) {
        if (info != null) {
            return getCounts(info.getPart(), OutcomeKeyedCounterTypeUtil::isSkip);
        } else {
            return 0;
        }
    }

    /**
     * Provides aggregated operation statistics from this task and all its subtasks.
     * Works with stored operation stats, obviously. (We have no task instances here.)
     *
     * Assumes that the task has all subtasks filled-in.
     *
     * Currently does NOT support low-level performance statistics, namely:
     *
     * 1. repositoryPerformanceInformation,
     * 2. cachesPerformanceInformation,
     * 3. operationsPerformanceInformation,
     * 4. workBucketManagementPerformanceInformation,
     * 5. cachingConfiguration.
     */
    public static OperationStatsType getOperationStatsFromTree(TaskType task, PrismContext prismContext) {
        if (!TaskWorkStateUtil.isPartitionedMaster(task) && !TaskWorkStateUtil.isWorkStateHolder(task)) {
            return task.getOperationStats();
        }

        OperationStatsType aggregate = new OperationStatsType(prismContext)
                .iterativeTaskInformation(new IterativeTaskInformationType(prismContext))
                .synchronizationInformation(new SynchronizationInformationType(prismContext))
                .actionsExecutedInformation(new ActionsExecutedInformationType())
                .environmentalPerformanceInformation(new EnvironmentalPerformanceInformationType());

        Stream<TaskType> subTasks = TaskTreeUtil.getAllTasksStream(task);
        subTasks.forEach(subTask -> {
            OperationStatsType operationStatsBean = subTask.getOperationStats();
            if (operationStatsBean != null) {
                IterativeTaskInformation.addTo(aggregate.getIterativeTaskInformation(), operationStatsBean.getIterativeTaskInformation());
                SynchronizationInformation.addTo(aggregate.getSynchronizationInformation(), operationStatsBean.getSynchronizationInformation());
                ActionsExecutedInformation.addTo(aggregate.getActionsExecutedInformation(), operationStatsBean.getActionsExecutedInformation());
                EnvironmentalPerformanceInformation.addTo(aggregate.getEnvironmentalPerformanceInformation(), operationStatsBean.getEnvironmentalPerformanceInformation());
            }
        });
        return aggregate;
    }

    /**
     * Returns the number of "iterations" i.e. how many times an item was processed by this task.
     * It is useful e.g. to provide average values for performance indicators.
     */
    public static Integer getItemsProcessed(TaskType task) {
        return getItemsProcessed(task.getOperationStats());
    }

    /**
     * Returns the number of "iterations" i.e. how many times an item was processed by this task.
     * It is useful e.g. to provide average values for performance indicators.
     */
    public static Integer getItemsProcessed(OperationStatsType statistics) {
        if (statistics == null || statistics.getIterativeTaskInformation() == null) {
            return null;
        } else {
            return getCounts(statistics.getIterativeTaskInformation().getPart(), set -> true);
        }
    }

    public static IterativeTaskPartItemsProcessingInformationType getIterativeInfoForCurrentPart(OperationStatsType statistics,
            StructuredTaskProgressType structuredProgress) {
        return getIterativeInfoForPart(statistics,
                TaskProgressUtil.getCurrentPartUri(structuredProgress));
    }

    private static IterativeTaskPartItemsProcessingInformationType getIterativeInfoForPart(OperationStatsType statistics,
            String partUri) {
        if (statistics == null || statistics.getIterativeTaskInformation() == null) {
            return null;
        } else {
            return statistics.getIterativeTaskInformation().getPart().stream()
                    .filter(part -> Objects.equals(part.getPartUri(), partUri))
                    .findFirst().orElse(null);
        }
    }

    public static int getItemsProcessedForCurrentPart(OperationStatsType statistics,
            StructuredTaskProgressType structuredTaskProgress) {
        return getItemsProcessed(
                getIterativeInfoForCurrentPart(statistics, structuredTaskProgress));
    }

    public static int getItemsProcessed(IterativeTaskPartItemsProcessingInformationType info) {
        if (info == null) {
            return 0;
        } else {
            return getCounts(singleton(info), set -> true);
        }
    }

    public static int getErrors(IterativeTaskPartItemsProcessingInformationType info) {
        if (info == null) {
            return 0;
        } else {
            return getCounts(singleton(info), OutcomeKeyedCounterTypeUtil::isFailure);
        }
    }

    public static Long getProcessingTime(IterativeTaskPartItemsProcessingInformationType info) {
        if (info == null) {
            return null;
        } else {
            return getProcessingTime(singleton(info), set -> true);
        }
    }

    /**
     * Returns sum of `count` values from processing information conforming to given predicate.
     */
    private static int getCounts(Collection<IterativeTaskPartItemsProcessingInformationType> parts,
            Predicate<ProcessedItemSetType> itemSetFilter) {
        return parts.stream()
                .flatMap(component -> component.getProcessed().stream())
                .filter(Objects::nonNull)
                .filter(itemSetFilter)
                .mapToInt(p -> or0(p.getCount()))
                .sum();
    }

    private static long getProcessingTime(Collection<IterativeTaskPartItemsProcessingInformationType> parts,
            Predicate<ProcessedItemSetType> itemSetFilter) {
        return parts.stream()
                .flatMap(component -> component.getProcessed().stream())
                .filter(Objects::nonNull)
                .filter(itemSetFilter)
                .mapToLong(p -> or0(p.getDuration()))
                .sum();
    }

    /**
     * Returns object that was last successfully processed by given task.
     */
    public static String getLastSuccessObjectName(TaskType task) {
        OperationStatsType stats = task.getOperationStats();
        if (stats == null || stats.getIterativeTaskInformation() == null) {
            return null;
        } else {
            return getLastProcessedObjectName(stats.getIterativeTaskInformation(), OutcomeKeyedCounterTypeUtil::isSuccess);
        }
    }

    /**
     * Returns object that was last processed by given task in item set defined by the filter.
     */
    public static String getLastProcessedObjectName(IterativeTaskInformationType info,
            Predicate<ProcessedItemSetType> itemSetFilter) {
        if (info == null) {
            return null;
        }
        ProcessedItemType lastSuccess = info.getPart().stream()
                .flatMap(component -> component.getProcessed().stream())
                .filter(itemSetFilter)
                .map(ProcessedItemSetType::getLastItem)
                .filter(Objects::nonNull)
                .max(Comparator.nullsFirst(Comparator.comparing(item -> XmlTypeConverter.toMillis(item.getEndTimestamp()))))
                .orElse(null);
        return lastSuccess != null ? lastSuccess.getName() : null;
    }

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

    public static long getWallClockTime(IterativeTaskPartItemsProcessingInformationType info) {
        return new WallClockTimeComputer(info.getExecution())
                .getSummaryTime();
    }
}
