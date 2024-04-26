/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.helpers;

import static com.evolveum.midpoint.schema.result.OperationResultStatus.FATAL_ERROR;

import java.util.Collection;
import java.util.Iterator;

import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.SequenceUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.sql.SerializationRelatedException;
import com.evolveum.midpoint.repo.sql.SqlRepositoryServiceImpl;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.PrismIdentifierGenerator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SequenceType;

@Component
public class SequenceHelper {

    @Autowired
    private ObjectRetriever objectRetriever;

    @Autowired
    private ObjectUpdater objectUpdater;

    @Autowired
    private BaseHelper baseHelper;

    private static final Trace LOGGER = TraceManager.getTrace(SqlRepositoryServiceImpl.class);
    private static final Trace LOGGER_PERFORMANCE = TraceManager.getTrace(SqlRepositoryServiceImpl.PERFORMANCE_LOG_NAME);

    public long advanceSequenceAttempt(String oid, OperationResult result) throws ObjectNotFoundException,
            SchemaException, SerializationRelatedException {

        long returnValue;

        LOGGER.debug("Advancing sequence with oid '{}'.", oid);
        LOGGER_PERFORMANCE.debug("> advance sequence, oid={}", oid);

        EntityManager em = null;
        try {
            em = baseHelper.beginTransaction();

            PrismObject<SequenceType> prismObject = objectRetriever.getObjectInternal(em, SequenceType.class, oid, null, true);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("OBJECT before:\n{}", prismObject.debugDump());
            }

            returnValue = SequenceUtil.advanceSequence(prismObject.asObjectable());

            LOGGER.trace("Return value = {}, OBJECT after:\n{}", returnValue, prismObject.debugDumpLazily());

            // merge and update object
            LOGGER.trace("Translating JAXB to data type.");
            PrismIdentifierGenerator idGenerator =
                    new PrismIdentifierGenerator(PrismIdentifierGenerator.Operation.MODIFY);
            RObject rObject = objectUpdater.createDataObjectFromJAXB(prismObject, idGenerator);
            rObject.setVersion(rObject.getVersion() + 1);

            objectUpdater.updateFullObject(rObject, prismObject);
            em.merge(rObject);

            LOGGER.trace("Before commit...");
            em.getTransaction().commit();
            LOGGER.trace("Committed!");

            return returnValue;
        } catch (ObjectNotFoundException | SchemaException ex) {
            baseHelper.rollbackTransaction(em, ex, result, FATAL_ERROR);
            throw ex;
        } catch (DtoTranslationException | RuntimeException ex) {
            baseHelper.handleGeneralException(ex, em, result);                                            // should always throw an exception
            throw new SystemException("Exception " + ex + " was not handled correctly", ex);        // ...so this shouldn't occur at all
        } finally {
            baseHelper.cleanupManagerAndResult(em, result);
            LOGGER.trace("EntityManager cleaned up.");
        }
    }

    public void returnUnusedValuesToSequenceAttempt(String oid, Collection<Long> unusedValues, OperationResult result) throws ObjectNotFoundException,
            SchemaException, SerializationRelatedException {

        LOGGER.debug("Returning unused values of {} to a sequence with oid '{}'.", unusedValues, oid);
        LOGGER_PERFORMANCE.debug("> return unused values, oid={}, values={}", oid, unusedValues);

        EntityManager em = null;
        try {
            em = baseHelper.beginTransaction();

            PrismObject<SequenceType> prismObject = objectRetriever.getObjectInternal(em, SequenceType.class, oid, null, true);
            LOGGER.trace("OBJECT before:\n{}", prismObject.debugDumpLazily());
            SequenceType sequence = prismObject.asObjectable();
            int maxUnusedValues = sequence.getMaxUnusedValues() != null ? sequence.getMaxUnusedValues() : 0;
            Iterator<Long> valuesToReturnIterator = unusedValues.iterator();
            while (valuesToReturnIterator.hasNext() && sequence.getUnusedValues().size() < maxUnusedValues) {
                Long valueToReturn = valuesToReturnIterator.next();
                if (valueToReturn == null) {        // sanity check
                    continue;
                }
                if (!sequence.getUnusedValues().contains(valueToReturn)) {
                    sequence.getUnusedValues().add(valueToReturn);
                } else {
                    LOGGER.warn("UnusedValues in sequence {} already contains value of {} - ignoring the return request", oid, valueToReturn);
                }
            }

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("OBJECT after:\n{}", prismObject.debugDump());
            }

            // merge and update object
            LOGGER.trace("Translating JAXB to data type.");
            PrismIdentifierGenerator idGenerator = new PrismIdentifierGenerator(PrismIdentifierGenerator.Operation.MODIFY);
            RObject rObject = objectUpdater.createDataObjectFromJAXB(prismObject, idGenerator);
            rObject.setVersion(rObject.getVersion() + 1);

            objectUpdater.updateFullObject(rObject, prismObject);
            em.merge(rObject);

            LOGGER.trace("Before commit...");
            em.getTransaction().commit();
            LOGGER.trace("Committed!");
        } catch (ObjectNotFoundException | SchemaException ex) {
            baseHelper.rollbackTransaction(em, ex, result, FATAL_ERROR);
            throw ex;
        } catch (DtoTranslationException | RuntimeException ex) {
            baseHelper.handleGeneralException(ex, em, result);                                            // should always throw an exception
            throw new SystemException("Exception " + ex + " was not handled correctly", ex);        // ...so this shouldn't occur at all
        } finally {
            baseHelper.cleanupManagerAndResult(em, result);
            LOGGER.trace("EntityManager cleaned up.");
        }
    }
}
