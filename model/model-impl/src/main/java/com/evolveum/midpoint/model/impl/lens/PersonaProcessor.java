/*
 * Copyright (c) 2017-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.construction.EvaluatedConstructionPack;
import com.evolveum.midpoint.model.impl.lens.construction.EvaluatedPersonaConstructionImpl;
import com.evolveum.midpoint.model.impl.lens.construction.PersonaConstruction;
import com.evolveum.midpoint.model.impl.lens.assignments.EvaluatedAssignmentImpl;
import com.evolveum.midpoint.model.impl.lens.projector.focus.TemplateMappingsEvaluation;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.impl.lens.projector.ComplexConstructionConsumer;
import com.evolveum.midpoint.model.impl.lens.projector.ConstructionProcessor;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Runs persona-related changes after the primary operation is all done. Executed directly from clockwork.
 *
 * Not entirely clean solution. Ideally, this should be somehow integrated in the clockwork process (nested contexts).
 * But should be good enough for now.
 *
 * @author semancik
 */
@Component
public class PersonaProcessor {

    private static final Trace LOGGER = TraceManager.getTrace(PersonaProcessor.class);

    private static final String OP_EXECUTE_PERSONA_DELTA = PersonaProcessor.class.getName() + ".executePersonaDelta";

    @Autowired private ConstructionProcessor constructionProcessor;
    @Autowired @Qualifier("modelObjectResolver")
    private ObjectResolver objectResolver;

    @Autowired @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    @Autowired private ContextFactory contextFactory;
    @Autowired private Clockwork clockwork;
    @Autowired private Clock clock;
    @Autowired private PrismContext prismContext;
    @Autowired private ModelBeans beans;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    <O extends ObjectType> HookOperationMode processPersonaChanges(LensContext<O> context, Task task, OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException, PolicyViolationException {

        LensFocusContext<O> focusContext = context.getFocusContext();
        if (focusContext == null) {
            return HookOperationMode.FOREGROUND;
        }
        if (!FocusType.class.isAssignableFrom(focusContext.getObjectTypeClass())) {
            // We can do this only for FocusType.
            return HookOperationMode.FOREGROUND;
        }

        if (focusContext.isDelete()) {
            // Special case. Simply delete all the existing personas.
            // TODO: maybe we need to do this before actual focus delete?
            LOGGER.trace("Focus delete -> delete all personas");
            // TODO
            return HookOperationMode.FOREGROUND;
        }

        return processPersonaChangesFocus((LensContext) context, task, result);
    }

    private <F extends FocusType> HookOperationMode processPersonaChangesFocus(LensContext<F> context, Task task, OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException, PolicyViolationException {
        //noinspection unchecked,rawtypes
        DeltaSetTriple<EvaluatedAssignmentImpl<F>> evaluatedAssignmentTriple = (DeltaSetTriple)context.getEvaluatedAssignmentTriple();
        if (evaluatedAssignmentTriple == null || evaluatedAssignmentTriple.isEmpty()) {
            return HookOperationMode.FOREGROUND;
        }

        DeltaSetTriple<PersonaKey> activePersonaKeyTriple = PrismContext.get().deltaFactory().createDeltaSetTriple();

        ComplexConstructionConsumer<PersonaKey, EvaluatedPersonaConstructionImpl<F>> consumer =
                new ComplexConstructionConsumer<>() {

                    @Override
                    public boolean before(PersonaKey key) {
                        return true;
                    }

                    @Override
                    public void onAssigned(PersonaKey key, String desc, Task task, OperationResult lResult) {
                        activePersonaKeyTriple.addToPlusSet(key);
                    }

                    @Override
                    public void onUnchangedValid(PersonaKey key, String desc, Task task, OperationResult lResult) {
                        activePersonaKeyTriple.addToZeroSet(key);
                    }

                    @Override
                    public void onUnchangedInvalid(PersonaKey key, String desc) {
                    }

                    @Override
                    public void onUnassigned(PersonaKey key, String desc) {
                        activePersonaKeyTriple.addToMinusSet(key);
                    }

                    @Override
                    public void after(PersonaKey key, String desc,
                            DeltaMapTriple<PersonaKey, EvaluatedConstructionPack<EvaluatedPersonaConstructionImpl<F>>> constructionMapTriple) {
                    }
                };

        DeltaMapTriple<PersonaKey, EvaluatedConstructionPack<EvaluatedPersonaConstructionImpl<F>>> constructionMapTriple =
            constructionProcessor.distributeConstructions(evaluatedAssignmentTriple,
                    EvaluatedAssignmentImpl::getPersonaConstructionTriple,
                    evaluatedConstruction ->
                            new PersonaKey(this, evaluatedConstruction.getConstruction().getConstructionBean()),
                    consumer, task, result);

        LOGGER.trace("activePersonaKeyTriple:\n{}", activePersonaKeyTriple.debugDumpLazily(1));

        List<FocusType> existingPersonas = readExistingPersonas(context, task, result);
        LOGGER.trace("existingPersonas:\n{}", existingPersonas);

        for (PersonaKey key: activePersonaKeyTriple.getNonNegativeValues()) {
            FocusType existingPersona = findPersona(existingPersonas, key);
            LOGGER.trace("existingPersona: {}", existingPersona);
            // TODO: add ability to merge several constructions
            EvaluatedConstructionPack<EvaluatedPersonaConstructionImpl<F>> pack = constructionMapTriple.getPlusMap().get(key);
            if (pack == null) {
                pack = constructionMapTriple.getZeroMap().get(key);
            }
            Collection<EvaluatedPersonaConstructionImpl<F>> evaluatedConstructions = pack.getEvaluatedConstructions();
            if (evaluatedConstructions.isEmpty()) {
                continue;
            }
            if (evaluatedConstructions.size() > 1) {
                throw new UnsupportedOperationException("Merging of multiple persona constructions is not supported yet");
            }
            PersonaConstruction<F> construction = evaluatedConstructions.iterator().next().getConstruction();
            LOGGER.trace("construction:\n{}", construction.debugDumpLazily());
            if (existingPersona == null) {
                personaAdd(context, key, construction, task, result);
            } else {
                personaModify(context, key, construction, existingPersona.asPrismObject(), task, result);
            }
        }

        for (PersonaKey key: activePersonaKeyTriple.getMinusSet()) {
            FocusType existingPersona = findPersona(existingPersonas, key);
            if (existingPersona != null) {
                personaDelete(context, key, existingPersona, task, result);
            }
        }

        return HookOperationMode.FOREGROUND;
    }

    private <F extends FocusType> List<FocusType> readExistingPersonas(LensContext<F> context, Task task, OperationResult result) throws CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        LensFocusContext<F> focusContext = context.getFocusContext();
        PrismObject<F> focus = focusContext.getObjectNew();

        List<FocusType> personas = new ArrayList<>();
        String desc = "personaRef in "+focus;
        for (ObjectReferenceType personaRef: focus.asObjectable().getPersonaRef()) {
            try {
                FocusType persona = objectResolver.resolve(personaRef, FocusType.class, null, desc, task, result);
                personas.add(persona);
            } catch (ObjectNotFoundException | SchemaException e) {
                LOGGER.warn("Cannot find persona {} referenced from {}", personaRef.getOid(), focus);
                // But go on...
            }
        }

        return personas;
    }

    private FocusType findPersona(List<FocusType> personas, PersonaKey key) {
        for (FocusType persona: personas) {
            if (personaMatches(persona, key)) {
                return persona;
            }
        }
        return null;
    }

    /**
     * Archetype checks are fragile, see {@link ObjectTypeUtil#hasArchetypeRef(AssignmentHolderType, String)} but should
     * be adequate, as this processor runs after main {@link Clockwork} work is done.
     */
    private boolean personaMatches(FocusType persona, PersonaKey key) {
        PrismObject<? extends FocusType> personaObj = persona.asPrismObject();
        QName personaType = personaObj.getDefinition().getTypeName();
        if (!QNameUtil.match(personaType, key.getType())) {
            return false;
        }

        for (String keyArchetypeOid: key.getArchetypeOids()) {
            if (!ObjectTypeUtil.hasArchetypeRef(persona, keyArchetypeOid)) {
                return false;
            }
        }
        return true;
    }

    private <F extends FocusType, T extends FocusType> void personaAdd(
            LensContext<F> context, PersonaKey key, PersonaConstruction<F> construction,
            Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, PolicyViolationException,
            ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException {
        PrismObject<F> focus = context.getFocusContext().getObjectNew();
        LOGGER.debug("Adding persona {} for {} using construction {}", key, focus, construction);
        PersonaConstructionType constructionBean = construction.getConstructionBean();
        ObjectReferenceType templateRef = constructionBean.getObjectMappingRef();
        ObjectTemplateType template = objectResolver.resolve(templateRef, ObjectTemplateType.class, null, "object template in persona construction in "+focus, task, result);

        QName targetType = constructionBean.getTargetType();
        PrismObjectDefinition<T> objectDef = prismContext.getSchemaRegistry().findObjectDefinitionByType(targetType);
        PrismObject<T> target = objectDef.instantiate();

        if (!constructionBean.getArchetypeRef().isEmpty()) {
            FocusTypeUtil.addArchetypeAssignments(target, constructionBean.getArchetypeRef());
        }

        // pretend ADD focusOdo. We need to push all the items through the object template
        ObjectDeltaObject<F> focusOdoAbsolute = new ObjectDeltaObject<>(null, focus.createAddDelta(), focus, context.getFocusContext().getObjectDefinition());
        ObjectDelta<T> targetDelta = target.createAddDelta();

        String contextDesc = "persona construction for "+focus;
        XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();

        Collection<ItemDelta<?, ?>> itemDeltas = processPersonaTemplate(context, template,
                focusOdoAbsolute, target, targetDelta, contextDesc, now, task, result);

        LOGGER.trace("itemDeltas:\n{}", DebugUtil.debugDumpLazily(itemDeltas));

        for (ItemDelta<?, ?> itemDelta: itemDeltas) {
            itemDelta.applyTo(target);
        }

        LOGGER.trace("Creating persona:\n{}", target.debugDumpLazily(1));

        String targetOid = executePersonaDelta(targetDelta, focus.getOid(), task, result);
        target.setOid(targetOid);

        link(context, target.asObjectable(), task, result);
    }

    private <F extends FocusType, T extends FocusType> void personaModify(LensContext<F> context, PersonaKey key, PersonaConstruction<F> construction,
            PrismObject<T> existingPersona, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, PolicyViolationException,
            ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException {
        PrismObject<F> focus = context.getFocusContext().getObjectNew();
        LOGGER.debug("Modifying persona {} for {} using construction {}", key, focus, construction);
        PersonaConstructionType constructionType = construction.getConstructionBean();
        ObjectReferenceType templateRef = constructionType.getObjectMappingRef();
        ObjectTemplateType template = objectResolver.resolve(templateRef, ObjectTemplateType.class, null, "object template in persona construction in "+focus, task, result);

        ObjectDeltaObject<F> focusOdoAbsolute = context.getFocusContext().getObjectDeltaObjectAbsolute();
        String contextDesc = "persona construction for "+focus;
        XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();

        Collection<ItemDelta<?, ?>> itemDeltas = processPersonaTemplate(context, template,
                focusOdoAbsolute, existingPersona, null, contextDesc, now, task, result);

        LOGGER.trace("itemDeltas:\n{}", DebugUtil.debugDumpLazily(itemDeltas));

        ObjectDelta<T> targetDelta = existingPersona.createModifyDelta();
        for (ItemDelta<?, ?> itemDelta: itemDeltas) {
            targetDelta.addModification(itemDelta);
        }

        executePersonaDelta(targetDelta, focus.getOid(), task, result);
    }

    private <F extends FocusType> void personaDelete(LensContext<F> context, PersonaKey key, FocusType existingPersona,
            Task task, OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
        PrismObject<F> focus = context.getFocusContext().getObjectOld();
        LOGGER.debug("Deleting persona {} for {}: {}", key, focus, existingPersona);
        ObjectDelta<? extends FocusType> targetDelta = existingPersona.asPrismObject().createDeleteDelta();

        executePersonaDelta(targetDelta, focus.getOid(), task, result);

        unlink(context, existingPersona, task, result);
    }

    /**
     * Processing object mapping: application of object template where focus is the source
     * and another object is the target. Used to map focus to personas.
     */
    private <F extends FocusType, T extends FocusType> Collection<ItemDelta<?,?>> processPersonaTemplate(
            LensContext<F> context, ObjectTemplateType template, ObjectDeltaObject<F> focusOdoAbsolute,
            PrismObject<T> target, ObjectDelta<T> targetAPrioriDelta, String contextDesc, XMLGregorianCalendar now,
            Task task, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException,
            SecurityViolationException, ConfigurationException, CommunicationException {

        TemplateMappingsEvaluation<F, T> evaluation = TemplateMappingsEvaluation.createForPersonaTemplate(
                beans, context, focusOdoAbsolute, template, target, targetAPrioriDelta, contextDesc, now, task, result);
        evaluation.computeItemDeltas();
        return evaluation.getItemDeltas();
    }

    private <F extends FocusType> void link(LensContext<F> context, FocusType persona, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        ObjectDelta<F> delta = context.getFocusContext().getObjectNew().createModifyDelta();
        PrismReferenceValue refValue = prismContext.itemFactory().createReferenceValue();
        refValue.setOid(persona.getOid());
        refValue.setTargetType(persona.asPrismObject().getDefinition().getTypeName());
        delta.addModificationAddReference(FocusType.F_PERSONA_REF, refValue);

        executeOrSimulate(delta, context, task, result);
    }

    private <F extends FocusType> void unlink(LensContext<F> context, FocusType persona, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        ObjectDelta<F> delta = context.getFocusContext().getObjectNew().createModifyDelta();
        PrismReferenceValue refValue = prismContext.itemFactory().createReferenceValue();
        refValue.setOid(persona.getOid());
        refValue.setTargetType(persona.asPrismObject().getDefinition().getTypeName());
        delta.addModificationDeleteReference(FocusType.F_PERSONA_REF, refValue);

        executeOrSimulate(delta, context, task, result);
    }

    private <F extends FocusType> void executeOrSimulate(
            ObjectDelta<F> delta, LensContext<F> context, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        // TODO we should audit the delta execution (if persistent mode) or put into simulation result (if simulation mode)
        //  see MID-10100
        if (task.isExecutionFullyPersistent()) {
            repositoryService.modifyObject(delta.getObjectTypeClass(), delta.getOid(), delta.getModifications(), result);
        } else {
            context.getFocusContextRequired().simulateDeltaExecution(delta);
        }
    }

    private <O extends ObjectType> String executePersonaDelta(ObjectDelta<O> delta, String ownerOid, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            PolicyViolationException, ExpressionEvaluationException, ObjectAlreadyExistsException, SecurityViolationException {
        OperationResult result = parentResult.subresult(OP_EXECUTE_PERSONA_DELTA)
                .build();
        try {
            Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(delta);
            LensContext<? extends ObjectType> context = contextFactory.createContext(deltas, null, task, result);
            // Persona changes are all "secondary" changes, triggered by roles and policies. We do not want to authorize
            // them as REQUEST. Assignment of the persona role was REQUEST. Changes in persona itself is all EXECUTION.
            context.setExecutionPhaseOnly(true);
            context.setOwnerOid(ownerOid);
            clockwork.run(context, task, result);
            String personaOid = ObjectDeltaOperation.findFocusDeltaOidInCollection(context.getExecutedDeltas());
            if (delta.isAdd() && personaOid == null) {
                LOGGER.warn("Persona delta execution resulted in no OID:\n{}", DebugUtil.debugDump(context.getExecutedDeltas()));
            }
            return personaOid;
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

}
