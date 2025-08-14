/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.smart;

import java.io.Serial;
import java.util.*;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.statistics.AbstractStatisticsPrinter;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.schema.util.ShadowObjectClassStatisticsTypeUtil;
import com.evolveum.midpoint.smart.api.SmartIntegrationService;
import com.evolveum.midpoint.smart.api.info.ObjectClassInfo;
import com.evolveum.midpoint.smart.api.info.ObjectClassInfoPrinter;
import com.evolveum.midpoint.smart.api.info.ObjectTypesSuggestionStatusInfoPrinter;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTypesSuggestionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

public class RealResourceStatus implements ResourceStatus {

    @Serial private static final long serialVersionUID = 1L;

    private final PrismObject<ResourceType> resource;
    private final Map<QName, ObjectClassInfo> objectClassInfoMap = new HashMap<>();
    private final List<StatusInfo<ObjectTypesSuggestionType>> statuses = new ArrayList<>();

    RealResourceStatus(PrismObject<ResourceType> resource) {
        this.resource = resource;
    }

    public void initialize(SmartIntegrationService sis, Task task, OperationResult result)
            throws CommonException {

        var schema = Resource.of(resource).getCompleteSchemaRequired();
        var resourceOid = resource.getOid();

        for (var objectClassDefinition : schema.getObjectClassDefinitions()) {
            var ocName = objectClassDefinition.getObjectClassName();
            var sizeEstimation = sis.estimateObjectClassSize(resourceOid, ocName, 100, task, result);
            var statsObject = sis.getLatestStatistics(resourceOid, ocName, task, result);
            var stats = statsObject != null ? ShadowObjectClassStatisticsTypeUtil.getStatisticsRequired(statsObject) : null;
            var typeDefs = schema.getObjectTypeDefinitions(t -> t.getObjectClassName().equals(ocName));
            objectClassInfoMap.put(ocName, new ObjectClassInfo(
                    objectClassDefinition, typeDefs, sizeEstimation, stats));
        }

        statuses.addAll(
                sis.listSuggestObjectTypesOperationStatuses(resourceOid, task, result));
    }

    private List<ObjectClassInfo> getClassesSortedByRelevance() {
        var classes = new ArrayList<>(objectClassInfoMap.values());
        classes.sort(
                Comparator
                        .comparing((ObjectClassInfo oci) -> oci.objectTypes().size(), Comparator.reverseOrder())
                        .thenComparing((ObjectClassInfo oci) -> oci.getStatisticsSize(), Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing((ObjectClassInfo oci) -> oci.sizeEstimation().getValue(), Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing((ObjectClassInfo oci) -> oci.getObjectClassName().getLocalPart()));
        return classes;
    }

    @Override
    public String getObjectClassesText() {
        return new ObjectClassInfoPrinter(getClassesSortedByRelevance(), new AbstractStatisticsPrinter.Options())
                .print();
    }

    @Override
    public String getObjectTypesSuggestionsText() {
        return new ObjectTypesSuggestionStatusInfoPrinter(statuses, new AbstractStatisticsPrinter.Options())
                .print();
    }

    @Override
    public ObjectClassInfo getSuggestedObjectClassInfo() {
        return getClassesSortedByRelevance().stream()
                .filter(i -> i.objectTypes().isEmpty()) // we want to suggest a class that has no types defined yet
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<ObjectClassInfo> getObjectClassInfos() {
        return getClassesSortedByRelevance();
    }

    @Override
    public void checkObjectClassName(QName objectClassName) throws SchemaException, ConfigurationException {
        Resource.of(resource).getCompleteSchemaRequired()
                .findObjectClassDefinitionRequired(
                        objectClassName,
                        () -> "Object class name '%s' is not defined in the resource schema".formatted(objectClassName));
    }

    @Override
    public StatusInfo<ObjectTypesSuggestionType> getObjectTypesSuggestionToExplore() {
        for (var status : getSuccessfulStatuses()) {
            var objectClass = status.getObjectClassName();
            if (objectClass == null || objectClassInfoMap.get(objectClass).objectTypes().isEmpty()) {
                return status;
            } else {
                // not suggesting those that are already defined
            }
        }
        return null;
    }

    private List<StatusInfo<ObjectTypesSuggestionType>> getSuccessfulStatuses() {
        return statuses.stream()
                .filter(s -> s.getStatus() == OperationResultStatusType.SUCCESS)
                .toList();
    }

    @Override
    public List<StatusInfo<ObjectTypesSuggestionType>> getObjectTypesSuggestions() {
        return getSuccessfulStatuses();
    }

    @Override
    public boolean isReal() {
        return true;
    }

    public ResourceType getResource() {
        return resource.asObjectable();
    }

    ObjectTypesSuggestionType getObjectTypesSuggestion(String token) {
        return statuses.stream()
                .filter(s -> s.getToken().equals(token))
                .map(i -> i.getResult())
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No suggestion with token " + token));
    }
}
