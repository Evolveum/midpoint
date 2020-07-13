/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql;

import java.util.List;

import com.google.common.base.Strings;
import org.apache.commons.configuration2.BaseHierarchicalConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;

import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.audit.api.AuditServiceFactory;
import com.evolveum.midpoint.audit.api.AuditServiceFactoryException;
import com.evolveum.midpoint.repo.api.RepositoryServiceFactoryException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author lazyman
 */
public class SqlAuditServiceFactory implements AuditServiceFactory {

    private static final Trace LOGGER = TraceManager.getTrace(SqlAuditServiceFactory.class);

    private static final String CONF_AUDIT_SERVICE_COLUMNS = "customColumn";
    private static final String CONF_AUDIT_SERVICE_COLUMN_NAME = "columnName";
    private static final String CONF_AUDIT_SERVICE_EVENT_RECORD_PROPERTY_NAME = "eventRecordPropertyName";

    private SqlRepositoryFactory repositoryFactory;
    private SqlAuditServiceImpl auditService;

    public void setRepositoryFactory(SqlRepositoryFactory repositoryFactory) {
        this.repositoryFactory = repositoryFactory;
    }

    @Override
    public synchronized void destroy() throws AuditServiceFactoryException {
        LOGGER.info("Destroying Sql audit service factory.");
        try {
            repositoryFactory.destroy();
        } catch (RepositoryServiceFactoryException ex) {
            throw new AuditServiceFactoryException(ex.getMessage(), ex);
        }
        LOGGER.info("Sql audit service factory destroy complete.");
    }

    @Override
    public synchronized void init(Configuration config) throws AuditServiceFactoryException {
        LOGGER.info("Initializing Sql audit service factory.");
        try {
            repositoryFactory.init(config);
            auditService = new SqlAuditServiceImpl(repositoryFactory);
            List<HierarchicalConfiguration<ImmutableNode>> subConfigColumns = ((BaseHierarchicalConfiguration) config).configurationsAt(CONF_AUDIT_SERVICE_COLUMNS);
            for (Configuration subConfigColumn : subConfigColumns) {
                String columnName = getStringFromConfig(subConfigColumn, CONF_AUDIT_SERVICE_COLUMN_NAME);
                String eventRecordPropertyName = getStringFromConfig(subConfigColumn, CONF_AUDIT_SERVICE_EVENT_RECORD_PROPERTY_NAME);
                auditService.getCustomColumn().put(eventRecordPropertyName, columnName);
            }
        } catch (RepositoryServiceFactoryException ex) {
            throw new AuditServiceFactoryException(ex.getMessage(), ex);
        }
        LOGGER.info("Sql audit service factory initialization complete.");
    }

    private String getStringFromConfig(Configuration config, String key) {
        String value = config.getString(key);
        if (Strings.isNullOrEmpty(value)) {
            LOGGER.error("Property with key ({}) not found in configuration. " +
                    "Provided configuration:\n{}", new Object[] { key, config });
            throw new SystemException("Property with key (" + key
                    + ") not found in configuration. Provided configuration:\n"
                    + config);
        }

        return value;
    }

    @Override
    public void destroyService(AuditService service) {
        //we don't need destroying service objects, they will be GC correctly
    }

    @Override
    public AuditService getAuditService() {
        return auditService;
    }
}
