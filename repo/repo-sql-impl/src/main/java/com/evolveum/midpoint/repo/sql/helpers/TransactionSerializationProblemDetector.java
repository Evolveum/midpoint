/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.helpers;

import java.sql.SQLException;
import java.util.regex.Pattern;

import org.hibernate.PessimisticLockException;
import org.hibernate.StaleObjectStateException;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.orm.hibernate5.HibernateOptimisticLockingFailureException;

import com.evolveum.midpoint.repo.sql.SerializationRelatedException;
import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration;
import com.evolveum.midpoint.schema.util.ExceptionUtil;
import com.evolveum.midpoint.util.logging.Trace;

/**
 * Methods detecting transaction serialization problems.
 * Uses our database/SQL configuration and takes logger from owner class to log under better name.
 */
public class TransactionSerializationProblemDetector {

    private final Trace log;
    private final SqlRepositoryConfiguration configuration;

    public TransactionSerializationProblemDetector(
            SqlRepositoryConfiguration configuration, Trace log) {
        this.log = log;
        this.configuration = configuration;
    }

    public boolean isExceptionRelatedToSerialization(Throwable ex) {
        boolean rv = isExceptionRelatedToSerializationInternal(ex);
        log.trace("Considering if exception {} is related to serialization: returning {}", ex, rv, ex);
        return rv;
    }

    private boolean isExceptionRelatedToSerializationInternal(Throwable ex) {
        if (ExceptionUtil.findCause(ex, SerializationRelatedException.class) != null
                || ExceptionUtil.findCause(ex, PessimisticLockException.class) != null
                || ExceptionUtil.findCause(ex, LockAcquisitionException.class) != null
                || ExceptionUtil.findCause(ex, HibernateOptimisticLockingFailureException.class) != null
                || ExceptionUtil.findCause(ex, StaleObjectStateException.class) != null) {  // todo the last one is questionable
            return true;
        }

        // it's not locking exception (optimistic, pessimistic lock or simple lock acquisition) understood by hibernate
        // however, it still could be such exception... wrapped in e.g. TransactionException
        // so we have a look inside - we try to find SQLException there

        SQLException sqlException = findSqlException(ex);
        if (sqlException == null) {
            return false;
        }

        if (hasSerializationRelatedConstraintViolationException(ex)) {
            return true;
        }

        // error messages / error codes / SQL states listed below we consider related to locking
        // (sql states should be somewhat standardized; sql error codes are vendor-specific)

        boolean mySqlCompatible = configuration.isUsingMySqlCompatible();
        boolean h2 = configuration.isUsingH2();
        boolean oracle = configuration.isUsingOracle();
        boolean sqlServer = configuration.isUsingSQLServer();

        return "40001".equals(sqlException.getSQLState()) // serialization failure in PostgreSQL - http://www.postgresql.org/docs/9.1/static/transaction-iso.html - and probably also in other systems
                || "40P01".equals(sqlException.getSQLState()) // deadlock in PostgreSQL
                || mySqlCompatible && messageContains(sqlException.getMessage(), MY_SQL_SERIALIZATION_ERRORS)
                || h2 && messageContains(sqlException.getMessage(), H_2_SERIALIZATION_ERRORS)
                || h2 && sqlException.getErrorCode() == 50200 // table timeout lock in H2, 50200 is LOCK_TIMEOUT_1 error code
                || h2 && sqlException.getErrorCode() == 40001 // DEADLOCK_1 in H2
                || oracle && sqlException.getErrorCode() == 8177 // ORA-08177: can't serialize access for this transaction in Oracle
                || oracle && sqlException.getErrorCode() == 1466 // ORA-01466 ["unable to read data - table definition has changed"] in Oracle
                || oracle && sqlException.getErrorCode() == 1555 // ORA-01555: snapshot too old: rollback segment number  with name "" too small
                || oracle && sqlException.getErrorCode() == 22924 // ORA-22924: snapshot too old
                || sqlServer && sqlException.getErrorCode() == 3960; // Snapshot isolation transaction aborted due to update conflict.
    }

    private boolean messageContains(String message, String[] substrings) {
        if (message != null) {
            for (String substring : substrings) {
                if (message.contains(substring)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static final String[] MY_SQL_SERIALIZATION_ERRORS = {
            // strange exception occurring in MySQL when doing multithreaded org closure maintenance
            // alternatively we might check for error code = 1030, sql state = HY000
            // but that would cover all cases of "Got error XYZ from storage engine"
            "Got error -1 from storage engine",

            // Another error that cannot be detected using standard mechanisms (MID-5561)
            "Lock wait timeout exceeded"
    };

    private static final String[] H_2_SERIALIZATION_ERRORS = {
            // this is some recent H2 weirdness (MID-3969)
            "Referential integrity constraint violation: \"FK_AUDIT_ITEM: PUBLIC.M_AUDIT_ITEM FOREIGN KEY(RECORD_ID) REFERENCES PUBLIC.M_AUDIT_EVENT(ID)"
    };

    private static final Pattern[] OK_PATTERNS = new Pattern[] {
            Pattern.compile(".*Duplicate entry '.*' for key 'iExtItemDefinition'.*"), // MySQL
            Pattern.compile(".*ORA-00001:.*\\.IEXTITEMDEFINITION\\).*") // Oracle
    };

    private static final String[] OK_STRINGS = new String[] {
            "Unique index or primary key violation: \"IEXTITEMDEFINITION", // H2
            "Violation of UNIQUE KEY constraint 'iExtItemDefinition'", // SQL Server
            "duplicate key value violates unique constraint \"iextitemdefinition\"", // PostgreSQL

            // SQL Server
            "Violation of PRIMARY KEY constraint 'PK__m_org_cl__",
            "Violation of PRIMARY KEY constraint 'PK__m_refere__",
            "Violation of PRIMARY KEY constraint 'PK__m_assign__",
            "Violation of PRIMARY KEY constraint 'PK__m_operat__",
            "Violation of PRIMARY KEY constraint 'PK__m_audit___", // MID-4815

            // ???
            "is not present in table \"m_ext_item\"",

            // PostgreSQL
            "duplicate key value violates unique constraint \"m_audit_item_pkey\"", // MID-4815
            "duplicate key value violates unique constraint \"m_audit_event_pkey\"", // MID-4815
            "duplicate key value violates unique constraint \"m_org_closure_pkey\"",
            "duplicate key value violates unique constraint \"m_reference_pkey\"",
            "duplicate key value violates unique constraint \"m_assignment_pkey\"",
            "duplicate key value violates unique constraint \"m_operation_execution_pkey\"" // TODO resolve more intelligently (and completely!)
    };

    private boolean hasSerializationRelatedConstraintViolationException(Throwable ex) {
        ConstraintViolationException cve = ExceptionUtil.findException(ex, ConstraintViolationException.class);
        return cve != null && isSerializationRelatedConstraintViolationException(cve);
    }

    public boolean isSerializationRelatedConstraintViolationException(ConstraintViolationException cve) {
        // BRUTAL HACK - serialization-related issues sometimes manifest themselves as ConstraintViolationException.
        //
        // For PostgreSQL and Microsoft SQL Server this happens mainly when automatically generated identifiers are to be
        // persisted (presumably because of their optimistic approach to serialization isolation).
        //
        // The "solution" provided here is not complete, as it does not recognize all places where IDs are generated.
        //
        // Moreover:
        //
        // For all databases the serialization issues can occur when m_ext_item records are created in parallel: see
        // ExtDictionaryConcurrencyTest but with "synchronized" keyword removed from state-changing ExtItemDictionary methods.
        // The solution is to check for ConstraintViolationExceptions related to iExtItemDefinition and treat these as
        // serialization-related issues.
        //
        // TODO: somewhat generalize this approach
        //
        // see MID-4698

        SQLException sqlException = findSqlException(cve);
        if (sqlException != null) {
            SQLException nextException = sqlException.getNextException();
            log.debug("ConstraintViolationException = {}; SQL exception = {}; embedded SQL exception = {}",
                    cve, sqlException, nextException);
            String msg1;
            if (sqlException.getMessage() != null) {
                msg1 = sqlException.getMessage().trim();
            } else {
                msg1 = "";
            }
            String msg2;
            if (nextException != null && nextException.getMessage() != null) {
                msg2 = nextException.getMessage().trim();
            } else {
                msg2 = "";
            }
            for (String okString : OK_STRINGS) {
                if (msg1.contains(okString) || msg2.contains(okString)) {
                    return true;
                }
            }
            for (Pattern okPattern : OK_PATTERNS) {
                if (okPattern.matcher(msg1).matches() || okPattern.matcher(msg2).matches()) {
                    return true;
                }
            }
        }
        return false;
    }

    private SQLException findSqlException(Throwable ex) {
        return ExceptionUtil.findException(ex, SQLException.class);
    }
}
