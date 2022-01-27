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

import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.model.api.correlator.CorrelatorInstantiationContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Represents the whole correlation context: a set of options, including "new owner" one.
 */
public class CorrelationContextDto implements Serializable {

    private static final Trace LOGGER = TraceManager.getTrace(CorrelationContextDto.class);

    static final String F_OPTION_HEADERS = "optionHeaders";
    static final String F_CORRELATION_OPTIONS = "correlationOptions";
    static final String F_CORRELATION_PROPERTIES = "correlationProperties";

    private static final String HEADER_NEW_OWNER = "New owner";
    private static final String HEADER_SUGGESTION = "Suggestion %d";

    /**
     * Headers for individual options: "New owner", "Suggestion 1", "Suggestion 2", ...
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
    private final List<CorrelationPropertyDefinition> correlationProperties = new ArrayList<>();

    CorrelationContextDto(CaseType aCase, PageBase pageBase, Task task, OperationResult result) throws CommonException {
        load(aCase, pageBase, task, result);
    }

    private void load(CaseType aCase, PageBase pageBase, Task task, OperationResult result) throws CommonException {
        resolveObjectsInCorrelationContext(aCase.getCorrelationContext(), pageBase, task, result);
        createCorrelationOptions(aCase.getCorrelationContext());
        createCorrelationPropertiesDefinitions(aCase, pageBase, task, result);
    }

    private void resolveObjectsInCorrelationContext(CorrelationContextType correlationContext, PageBase pageBase, Task task, OperationResult result) {
        for (PotentialOwnerType potentialOwner : correlationContext.getPotentialOwners().getPotentialOwner()) {
            resolve(potentialOwner.getCandidateOwnerRef(), "candidate " + potentialOwner.getUri(), pageBase, task, result);
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

    private void createCorrelationOptions(CorrelationContextType context) {
        int suggestionNumber = 1;
        for (PotentialOwnerType potentialOwner : context.getPotentialOwners().getPotentialOwner()) {
            if (SchemaConstants.CORRELATION_NONE_URI.equals(potentialOwner.getUri())) {
                optionHeaders.add(0, HEADER_NEW_OWNER);
                correlationOptions.add(0,
                        new CorrelationOptionDto(context.getPreFocusRef()));
            } else {
                optionHeaders.add(String.format(HEADER_SUGGESTION, suggestionNumber));
                correlationOptions.add(
                        new CorrelationOptionDto(potentialOwner));
                suggestionNumber++;
            }
        }
    }

    private void createCorrelationPropertiesDefinitions(CaseType aCase, PageBase pageBase, Task task, OperationResult result)
            throws CommonException {
        CorrelatorInstantiationContext instantiationContext =
                pageBase.getCorrelationService().getInstantiationContext(aCase.asPrismObject(), task, result);
        CorrelationPropertiesDefinitionType propertiesBean = instantiationContext.synchronizationBean.getCorrelationProperties();
        CorrelationOptionDto newOwnerOption = getNewOwnerOption();
        if (propertiesBean != null) {
            PrismObject<?> preFocus = newOwnerOption != null ? newOwnerOption.getObject() : null;
            CorrelationPropertyDefinition.fillFromConfiguration(correlationProperties, propertiesBean, preFocus);
        } else {
            if (newOwnerOption == null) {
                LOGGER.warn("Couldn't create property definitions from 'new owner' focus object because there's none");
            } else {
                CorrelationPropertyDefinition.fillFromObject(correlationProperties, newOwnerOption.getObject());
            }
        }
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

    public List<CorrelationPropertyDefinition> getCorrelationProperties() {
        return correlationProperties;
    }
}
