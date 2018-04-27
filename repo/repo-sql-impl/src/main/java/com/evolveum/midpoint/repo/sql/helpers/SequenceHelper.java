/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.helpers;

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
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Iterator;

/**
 * @author mederly
 */
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

        Session session = null;
        try {
            session = baseHelper.beginTransaction();

            PrismObject<SequenceType> prismObject = objectRetriever.getObjectInternal(session, SequenceType.class, oid, null, true, result);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("OBJECT before:\n{}", prismObject.debugDump());
            }
            SequenceType sequence = prismObject.asObjectable();

            if (!sequence.getUnusedValues().isEmpty()) {
                returnValue = sequence.getUnusedValues().remove(0);
            } else {
                long counter = sequence.getCounter() != null ? sequence.getCounter() : 0L;
                long maxCounter = sequence.getMaxCounter() != null ? sequence.getMaxCounter() : Long.MAX_VALUE;
                boolean allowRewind = Boolean.TRUE.equals(sequence.isAllowRewind());

                if (counter < maxCounter) {
                    returnValue = counter;
                    sequence.setCounter(counter + 1);
                } else if (counter == maxCounter) {
                    returnValue = counter;
                    if (allowRewind) {
                        sequence.setCounter(0L);
                    } else {
                        sequence.setCounter(counter + 1);       // will produce exception during next run
                    }
                } else {        // i.e. counter > maxCounter
                    if (allowRewind) {          // shouldn't occur but...
                        LOGGER.warn("Sequence {} overflown with allowRewind set to true. Rewinding.", oid);
                        returnValue = 0;
                        sequence.setCounter(1L);
                    } else {
                        // TODO some better exception...
                        throw new SystemException("No (next) value available from sequence " + oid + ". Current counter = " + sequence.getCounter() + ", max value = " + sequence.getMaxCounter());
                    }
                }
            }

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Return value = {}, OBJECT after:\n{}", returnValue, prismObject.debugDump());
            }

            // merge and update object
            LOGGER.trace("Translating JAXB to data type.");
            PrismIdentifierGenerator<SequenceType> idGenerator = new PrismIdentifierGenerator<>(PrismIdentifierGenerator.Operation.MODIFY);
            RObject rObject = objectUpdater.createDataObjectFromJAXB(prismObject, idGenerator);
            rObject.setVersion(rObject.getVersion() + 1);

            objectUpdater.updateFullObject(rObject, prismObject);
            session.merge(rObject);

            LOGGER.trace("Before commit...");
            session.getTransaction().commit();
            LOGGER.trace("Committed!");

            return returnValue;
        } catch (ObjectNotFoundException ex) {
            baseHelper.rollbackTransaction(session, ex, result, true);
            throw ex;
        } catch (SchemaException ex) {
            baseHelper.rollbackTransaction(session, ex, result, true);
            throw ex;
        } catch (DtoTranslationException | RuntimeException ex) {
            baseHelper.handleGeneralException(ex, session, result);                                            // should always throw an exception
            throw new SystemException("Exception " + ex + " was not handled correctly", ex);        // ...so this shouldn't occur at all
        } finally {
            baseHelper.cleanupSessionAndResult(session, result);
            LOGGER.trace("Session cleaned up.");
        }
    }

    public void returnUnusedValuesToSequenceAttempt(String oid, Collection<Long> unusedValues, OperationResult result) throws ObjectNotFoundException,
            SchemaException, SerializationRelatedException {

        LOGGER.debug("Returning unused values of {} to a sequence with oid '{}'.", unusedValues, oid);
        LOGGER_PERFORMANCE.debug("> return unused values, oid={}, values={}", oid, unusedValues);

        Session session = null;
        try {
            session = baseHelper.beginTransaction();

            PrismObject<SequenceType> prismObject = objectRetriever.getObjectInternal(session, SequenceType.class, oid, null, true, result);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("OBJECT before:\n{}", prismObject.debugDump());
            }
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
            PrismIdentifierGenerator<SequenceType> idGenerator = new PrismIdentifierGenerator<>(PrismIdentifierGenerator.Operation.MODIFY);
            RObject rObject = objectUpdater.createDataObjectFromJAXB(prismObject, idGenerator);
            rObject.setVersion(rObject.getVersion() + 1);

            objectUpdater.updateFullObject(rObject, prismObject);
            session.merge(rObject);

            LOGGER.trace("Before commit...");
            session.getTransaction().commit();
            LOGGER.trace("Committed!");
        } catch (ObjectNotFoundException ex) {
            baseHelper.rollbackTransaction(session, ex, result, true);
            throw ex;
        } catch (SchemaException ex) {
            baseHelper.rollbackTransaction(session, ex, result, true);
            throw ex;
        } catch (DtoTranslationException | RuntimeException ex) {
            baseHelper.handleGeneralException(ex, session, result);                                            // should always throw an exception
            throw new SystemException("Exception " + ex + " was not handled correctly", ex);        // ...so this shouldn't occur at all
        } finally {
            baseHelper.cleanupSessionAndResult(session, result);
            LOGGER.trace("Session cleaned up.");
        }
    }
}
