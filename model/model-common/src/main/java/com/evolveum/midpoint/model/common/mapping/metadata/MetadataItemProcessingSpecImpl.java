/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.mapping.metadata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.schema.metadata.DefaultValueMetadataProcessing;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.MetadataItemProcessingSpec;
import com.evolveum.midpoint.model.common.util.ObjectTemplateIncludeProcessor;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Specification of processing of individual metadata items.
 *
 * This is somewhat orthogonal to {@link ItemValueMetadataProcessingSpec}: it describes support for
 * given metadata item (e.g. provenance) for individual data items (givenName, familyName, fullName, etc).
 */
public class MetadataItemProcessingSpecImpl implements MetadataItemProcessingSpec {

    private static final Trace LOGGER = TraceManager.getTrace(MetadataItemProcessingSpecImpl.class);

    @NotNull private final ItemPath metadataItemPath;

    private final List<ProcessingSpec> processingSpecs = new ArrayList<>();
    private final DefaultValueMetadataProcessing defaultProcessing;

    public MetadataItemProcessingSpecImpl(@NotNull ItemPath metadataItemPath) {
        this.metadataItemPath = metadataItemPath;
        this.defaultProcessing = DefaultValueMetadataProcessing.forMetadataItem(metadataItemPath.firstName());

    }

    @Override
    public boolean isFullProcessing(ItemPath dataItemPath) throws SchemaException {
        for (ProcessingSpec processingSpec : processingSpecs) {
            if (processingSpec.processing == ItemProcessingType.FULL && processingSpec.appliesTo(dataItemPath)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isFullProcessing(ItemPath dataItemPath, ItemDefinition<?> definition) throws SchemaException {
        // FIXME: Maybe first we should check if it is enabled by default or check if it is explicitly disabled?
        for (ProcessingSpec processingSpec : processingSpecs) {
            if (processingSpec.processing == ItemProcessingType.FULL && processingSpec.appliesTo(dataItemPath)) {
                return true;
            }
        }
        if (defaultProcessing.isEnabledFor(dataItemPath, definition)) {
            return true;
        }
        return false;
    }

    public void populateFromObjectTemplate(ObjectReferenceType templateRef, ObjectResolver objectResolver, String contextDesc,
            Task task, OperationResult result) throws CommunicationException, ObjectNotFoundException, SchemaException,
            SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        if (templateRef == null) {
            LOGGER.trace("No object template, no metadata handling");
        } else {
            ObjectTemplateType template = objectResolver.resolve(templateRef, ObjectTemplateType.class, null,
                    "metadata handling determination", task, result);
            if (template != null) {
                addFromObjectTemplate(template, objectResolver, contextDesc, task, result);
            }
        }
    }

    private void addFromObjectTemplate(ObjectTemplateType rootTemplate, ObjectResolver objectResolver, String contextDesc,
            Task task, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {

        LOGGER.trace("Obtaining metadata handling instructions from {}", rootTemplate);

        // TODO resolve conflicts in included templates
        try {
            new ObjectTemplateIncludeProcessor(objectResolver)
                    .processThisAndIncludedTemplates(rootTemplate, contextDesc, task, result, this::addFromObjectTemplate);
        } catch (TunnelException e) {
            if (e.getCause() instanceof SchemaException) {
                throw (SchemaException) e.getCause();
            } else {
                MiscUtil.unwrapTunnelledExceptionToRuntime(e);
            }
        }
    }

    private void addFromObjectTemplate(ObjectTemplateType template) {
        addMetadataHandling(template.getMeta(), null);
        template.getItem().forEach(
                itemDef -> addMetadataHandling(itemDef.getMeta(), itemDef.getRef()));
    }

    private void addMetadataHandling(MetadataHandlingType metadataHandling, ItemPathType dataItemPathBean) {
        try {
            if (metadataHandling != null) {
                for (MetadataItemDefinitionType metadataItemDef : metadataHandling.getItem()) {
                    if (metadataItemDef.getRef() != null && metadataItemDef.getRef().getItemPath().equivalent(metadataItemPath)) {
                        ItemProcessingType processing = ProcessingUtil.getProcessingOfItem(metadataItemDef);
                        if (processing != null) {
                            processingSpecs.add(new ProcessingSpec(processing, dataItemPathBean,
                                    metadataHandling.getApplicability(), metadataItemDef.getApplicability()));
                        }
                    }
                }
            }
        } catch (SchemaException e) {
            throw new TunnelException(e);
        }
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpWithLabelLn(sb, "metadataItemPath", String.valueOf(metadataItemPath), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "processingSpecs", processingSpecs, indent);
        return sb.toString();
    }

    private static class ProcessingSpec implements DebugDumpable {
        private final ItemProcessingType processing;
        private final ItemPath dataItemPath;
        private final List<MetadataProcessingApplicabilitySpecificationType> applicabilityList;

        private ProcessingSpec(ItemProcessingType processing, ItemPathType dataItemPathBean,
                MetadataProcessingApplicabilitySpecificationType... applicabilityList) {
            this.processing = processing;
            this.dataItemPath = dataItemPathBean != null ? dataItemPathBean.getItemPath() : null;
            this.applicabilityList = Arrays.asList(applicabilityList);
        }

        public boolean appliesTo(ItemPath dataItemPath) throws SchemaException {
            if (this.dataItemPath != null && !this.dataItemPath.equivalent(dataItemPath)) {
                return false;
            }
            for (MetadataProcessingApplicabilitySpecificationType applicability : applicabilityList) {
                if (!ProcessingUtil.doesApplicabilityMatch(applicability, dataItemPath)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String debugDump(int indent) {
            StringBuilder sb = new StringBuilder();
            DebugUtil.debugDumpWithLabelLn(sb, "processing", String.valueOf(processing), indent);
            DebugUtil.debugDumpWithLabelLn(sb, "dataItemPath", String.valueOf(dataItemPath), indent);
            DebugUtil.debugDumpWithLabelLn(sb, "applicabilityList", applicabilityList, indent);
            return sb.toString();
        }
    }
}
