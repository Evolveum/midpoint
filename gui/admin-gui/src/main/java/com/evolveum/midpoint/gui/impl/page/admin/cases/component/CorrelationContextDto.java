/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.cases.component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.model.api.correlation.CorrelationCaseDescription;

import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.cases.CorrelationCaseUtil;
import com.evolveum.midpoint.schema.util.cases.OwnerOptionIdentifier;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Represents the whole correlation context: a set of options, including "new owner" one.
 */
class CorrelationContextDto implements Serializable {

    private static final Trace LOGGER = TraceManager.getTrace(CorrelationContextDto.class);

    static final String F_OPTION_HEADERS = "optionHeaders";
    static final String F_CORRELATION_OPTIONS = "correlationOptions";
    static final String F_CORRELATION_PROPERTIES = "correlationProperties";

    // TODO move into properties
    private static final String TEXT_BEING_CORRELATED = "Object being correlated";
    private static final String TEXT_CANDIDATE = "Correlation candidate %d";

    /**
     * Headers for individual options.
     */
    private final List<String> optionHeaders = new ArrayList<>();

    /**
     * All correlation options. Correspond to columns in the correlation options table.
     *
     * The first one is the "new owner" option.
     */
    private final List<CorrelationOptionDto> correlationOptions = new ArrayList<>();

    /**
     * Properties that will be presented by the GUI. Obtained either from the correlation configuration
     * or determined dynamically.
     *
     * Correspond to rows in the correlation options table.
     */
    private final List<CorrelationCaseDescription.CorrelationProperty> correlationProperties = new ArrayList<>();

    CorrelationContextDto(CaseType aCase, PageBase pageBase, Task task, OperationResult result) throws CommonException {
        load(aCase, pageBase, task, result);
    }

    private void load(CaseType aCase, PageBase pageBase, Task task, OperationResult result) throws CommonException {
        ResourceObjectOwnerOptionsType ownerOptions = CorrelationCaseUtil.getOwnerOptions(aCase);
        if (ownerOptions != null) {
            resolvePotentialOwners(ownerOptions, pageBase, task, result);
        }
        createCorrelationOptions(aCase);
        createCorrelationPropertiesDefinitions(aCase, pageBase, task, result);
    }

    private void resolvePotentialOwners(ResourceObjectOwnerOptionsType potentialOwners, PageBase pageBase, Task task, OperationResult result) {
        for (ResourceObjectOwnerOptionType potentialOwner : potentialOwners.getOption()) {
            resolve(potentialOwner.getCandidateOwnerRef(), "candidate " + potentialOwner.getIdentifier(), pageBase, task, result);
        }
    }

    private void resolve(ObjectReferenceType ref, String desc, PageBase pageBase, Task task, OperationResult result) {
        LOGGER.trace("Resolving {}: {}", desc, ref);
        if (ref == null || ref.getOid() == null) {
            LOGGER.trace("Null ref or no OID");
            return;
        }
        if (ref.getObject() != null) {
            LOGGER.trace("Already resolved");
            return;
        }
        ref.asReferenceValue().setObject(
                loadObject(ref.getOid(), pageBase, task, result));
    }

    private PrismObject<?> loadObject(String oid, PageBase pageBase, Task task, OperationResult result) {
        try {
            return pageBase.getModelService()
                    .getObject(FocusType.class, oid, null, task, result);
        } catch (Exception e) {
            result.recordFatalError(e);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't resolve focus {}", e, oid);
            return null;
        }
    }

    private void createCorrelationOptions(CaseType aCase) throws SchemaException {
        CaseCorrelationContextType context = aCase.getCorrelationContext();
        int suggestionNumber = 1;
        for (ResourceObjectOwnerOptionType potentialOwner : CorrelationCaseUtil.getOwnerOptionsList(aCase)) {
            OwnerOptionIdentifier identifier = OwnerOptionIdentifier.of(potentialOwner);
            if (identifier.isNewOwner()) {
                optionHeaders.add(0, TEXT_BEING_CORRELATED);
                correlationOptions.add(0,
                        new CorrelationOptionDto(potentialOwner, context.getPreFocusRef()));
            } else {
                optionHeaders.add(String.format(TEXT_CANDIDATE, suggestionNumber));
                correlationOptions.add(
                        new CorrelationOptionDto(potentialOwner));
                suggestionNumber++;
            }
        }
    }

    private void createCorrelationPropertiesDefinitions(CaseType aCase, PageBase pageBase, Task task, OperationResult result)
            throws CommonException {
        correlationProperties.clear();
        correlationProperties.addAll(
                pageBase.getCorrelationService()
                        .describeCorrelationCase(aCase, null, task, result)
                        .getCorrelationProperties()
                        .values());
    }

    @Nullable CorrelationOptionDto getNewOwnerOption() {
        if (correlationOptions.isEmpty()) {
            return null;
        }
        CorrelationOptionDto first = correlationOptions.get(0);
        return first.isNewOwner() ? first : null;
    }

    public List<CorrelationOptionDto> getCorrelationOptions() {
        return correlationOptions;
    }

    public List<String> getOptionHeaders() {
        return optionHeaders;
    }

    public List<CorrelationCaseDescription.CorrelationProperty> getCorrelationProperties() {
        return correlationProperties;
    }
}
