/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql;

import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.repo.sql.perf.SqlPerformanceMonitorImpl;

/**
 * @author lazyman
 */
public class SqlBaseService {

    // how many times we want to repeat operation after lock acquisition,
    // pessimistic, optimistic exception
    public static final int LOCKING_MAX_RETRIES = 40;

    // timeout will be a random number between 0 and LOCKING_DELAY_INTERVAL_BASE * 2^exp
    // where exp is either real attempt # minus 1, or LOCKING_EXP_THRESHOLD (whatever is lesser)
    public static final long LOCKING_DELAY_INTERVAL_BASE = 50;
    public static final int LOCKING_EXP_THRESHOLD = 7; // i.e. up to 6400 msec wait time

    @Autowired private PrismContext prismContext;
    @Autowired private MatchingRuleRegistry matchingRuleRegistry;

    private final SqlRepositoryFactory repositoryFactory;

    public SqlBaseService(SqlRepositoryFactory repositoryFactory) {
        this.repositoryFactory = repositoryFactory;
    }

    public SqlRepositoryConfiguration getConfiguration() {
        return repositoryFactory.getSqlConfiguration();
    }

    public SqlPerformanceMonitorImpl getPerformanceMonitor() {
        return repositoryFactory.getPerformanceMonitor();
    }

    public PrismContext getPrismContext() {
        return prismContext;
    }

    public void setPrismContext(PrismContext prismContext) {
        this.prismContext = prismContext;
    }

    public MatchingRuleRegistry getMatchingRuleRegistry() {
        return matchingRuleRegistry;
    }

    public void setMatchingRuleRegistry(MatchingRuleRegistry matchingRuleRegistry) {
        this.matchingRuleRegistry = matchingRuleRegistry;
    }
}
