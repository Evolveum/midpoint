/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.processors;

import static com.evolveum.midpoint.prism.PrismObject.asPrismObject;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;

import com.evolveum.midpoint.model.impl.lens.LensFocusContext;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.util.MiscHelper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

/**
 * Helper class intended to facilitate processing of model invocation.
 */
@Component
public class ModelHelper {

    private static final Trace LOGGER = TraceManager.getTrace(ModelHelper.class);

    @Autowired private LocalizationService localizationService;
    @Autowired private PrismContext prismContext;
    @Autowired private MiscHelper miscHelper;
    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;

    private static final String APPROVING_AND_EXECUTING_KEY = "ApprovingAndExecuting.";
    private static final String CREATION_OF_KEY = "CreationOf";
    private static final String DELETION_OF_KEY = "DeletionOf";
    private static final String CHANGE_OF_KEY = "ChangeOf";

    /**
     * Creates a root job creation instruction.
     *
     * @param changeProcessor reference to the change processor responsible for the whole operation
     * @param contextForRootCase model context that should be put into the root task (might be different from the modelContext)
     * @return the job creation instruction
     */
    public StartInstruction createInstructionForRoot(
            ChangeProcessor changeProcessor,
            @NotNull ModelInvocationContext<?> ctx,
            ModelContext<?> contextForRootCase,
            OperationResult result) throws SchemaException {
        StartInstruction instruction =
                StartInstruction.create(changeProcessor, SystemObjectsType.ARCHETYPE_OPERATION_REQUEST.value());
        instruction.setModelContext(contextForRootCase);

        LocalizableMessage rootCaseName = determineRootCaseName(ctx);
        String rootCaseNameInDefaultLocale = localizationService.translate(rootCaseName, Locale.getDefault());
        instruction.setName(rootCaseNameInDefaultLocale, rootCaseName);
        instruction.setObjectRef(ctx);
        instruction.setRequesterRef(ctx.getRequestor(result));
        return instruction;
    }

    /**
     * Determines the root task name (e.g. "Workflow for adding XYZ (started 1.2.2014 10:34)")
     * TODO allow change processor to influence this name
     */
    private LocalizableMessage determineRootCaseName(ModelInvocationContext<?> ctx) {
        String operationKey;
        LensFocusContext<?> focusContext = ctx.modelContext.getFocusContext();
        if (focusContext != null && focusContext.getPrimaryDelta() != null
                && focusContext.getPrimaryDelta().getChangeType() != null) {
            switch (focusContext.getPrimaryDelta().getChangeType()) {
                case ADD:
                    operationKey = CREATION_OF_KEY;
                    break;
                case DELETE:
                    operationKey = DELETION_OF_KEY;
                    break;
                case MODIFY:
                    operationKey = CHANGE_OF_KEY;
                    break;
                default:
                    throw new IllegalStateException();
            }
        } else {
            operationKey = CHANGE_OF_KEY;
        }
        ObjectType focus = ctx.getFocusObjectNewOrOld();
        String time = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                .withLocale(Locale.getDefault())
                .format(ZonedDateTime.now());

        return new LocalizableMessageBuilder()
                .key(APPROVING_AND_EXECUTING_KEY + operationKey)
                .arg(ObjectTypeUtil.createDisplayInformation(asPrismObject(focus), false))
                .arg(time)
                .build();
    }

    /**
     * Creates a root job, based on provided job start instruction.
     * Puts a reference to the workflow root task to the model task.
     *
     * @param rootInstruction instruction to use
     * @return reference to a newly created job
     */
    public CaseType addRoot(StartInstruction rootInstruction, OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException {
        CaseType rootCase = addCase(rootInstruction, result);
        result.setCaseOid(rootCase.getOid());
        return rootCase;
    }

    public void logJobsBeforeStart(CaseType rootCase, OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
        if (!LOGGER.isTraceEnabled()) {
            return;
        }

        StringBuilder sb = new StringBuilder();

        sb.append("===[ Situation just after case tree creation ]===\n");
        sb.append("Root case:\n").append(dumpCase(rootCase)).append("\n");
        List<CaseType> children = miscHelper.getSubcases(rootCase, result);
        for (int i = 0; i < children.size(); i++) {
            CaseType child = children.get(i);
            sb.append("Child job #").append(i).append(":\n").append(dumpCase(child));
        }
        LOGGER.trace("\n{}", sb);
    }

    private static final boolean USE_DEBUG_DUMP = false;

    private String dumpCase(CaseType aCase) {
        if (USE_DEBUG_DUMP) {
            return aCase.asPrismObject().debugDump(1);
        } else {
            try {
                return prismContext.xmlSerializer().serialize(aCase.asPrismObject());
            } catch (SchemaException e) {
                return "schema exception: " + e;
            }
        }
    }

    /**
     * Creates a case in repository.
     *
     * @param instruction the wf task creation instruction
     */
    public CaseType addCase(StartInstruction instruction, OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException {
        LOGGER.trace("Processing start instruction:\n{}", instruction.debugDumpLazily());
        CaseType aCase = instruction.getCase();
        repositoryService.addObject(aCase.asPrismObject(), null, result);
        return aCase;
    }
}
