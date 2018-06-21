/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author lazyman
 */
public class SqlBaseService {

    private static final Trace LOGGER = TraceManager.getTrace(SqlBaseService.class);
    // how many times we want to repeat operation after lock acquisition,
    // pessimistic, optimistic exception
	public static final int LOCKING_MAX_RETRIES = 40;

    // timeout will be a random number between 0 and LOCKING_DELAY_INTERVAL_BASE * 2^exp where exp is either real attempt # minus 1, or LOCKING_EXP_THRESHOLD (whatever is lesser)
    public static final long LOCKING_DELAY_INTERVAL_BASE = 50;
	public static final int LOCKING_EXP_THRESHOLD = 7;       // i.e. up to 6400 msec wait time

    @Autowired
    private PrismContext prismContext;
    @Autowired
    private MatchingRuleRegistry matchingRuleRegistry;

    private SqlRepositoryFactory repositoryFactory;

    public SqlBaseService(SqlRepositoryFactory repositoryFactory) {
        this.repositoryFactory = repositoryFactory;
    }

    public SqlRepositoryConfiguration getConfiguration() {
        return repositoryFactory.getSqlConfiguration();
    }

    public SqlPerformanceMonitor getPerformanceMonitor() {
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
