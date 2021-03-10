/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.api.*;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.task.api.Tracer;
import com.evolveum.midpoint.task.quartzimpl.cluster.ClusterManager;
import com.evolveum.midpoint.task.quartzimpl.cluster.ClusterStatusInformationRetriever;
import com.evolveum.midpoint.task.quartzimpl.cluster.NodeRegistrar;
import com.evolveum.midpoint.task.quartzimpl.execution.*;
import com.evolveum.midpoint.task.quartzimpl.nodes.NodeCleaner;
import com.evolveum.midpoint.task.quartzimpl.nodes.NodeRetriever;
import com.evolveum.midpoint.task.quartzimpl.quartz.LocalScheduler;
import com.evolveum.midpoint.task.quartzimpl.quartz.TaskSynchronizer;
import com.evolveum.midpoint.task.quartzimpl.run.HandlerExecutor;
import com.evolveum.midpoint.task.quartzimpl.tasks.TaskInstantiator;
import com.evolveum.midpoint.task.quartzimpl.tasks.TaskStateManager;
import com.evolveum.midpoint.task.quartzimpl.tasks.TaskPersister;
import com.evolveum.midpoint.task.quartzimpl.tasks.TaskRetriever;
import com.evolveum.midpoint.task.quartzimpl.work.WorkStateManager;
import com.evolveum.midpoint.task.quartzimpl.work.workers.WorkersManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Beans to be used by non-Spring component classes in task manager module.
 */
@Component
public class TaskBeans {

    //region Beans in task manager
    @Autowired public TaskManagerConfiguration configuration;
    @Autowired public Tracer tracer;
    @Autowired public Schedulers schedulers;
    @Autowired public TaskThreadsDumper taskThreadsDumper;
    @Autowired public TaskStopper taskStopper;
    @Autowired public LocalScheduler localScheduler;
    @Autowired public LocalExecutionManager localExecutionManager;
    @Autowired public ClusterManager clusterManager;
    @Autowired public TaskHandlerRegistry handlerRegistry;
    @Autowired public HandlerExecutor handlerExecutor;
    @Autowired public TaskListenerRegistry listenerRegistry;
    @Autowired public TaskStateManager taskStateManager;
    @Autowired public TaskRetriever taskRetriever;
    @Autowired public NodeRetriever nodeRetriever;
    @Autowired public TaskPersister taskPersister;
    @Autowired public TaskInstantiator taskInstantiator;
    @Autowired public LocalNodeState localNodeState;
    @Autowired public NodeCleaner nodeCleaner;
    @Autowired public NodeRegistrar nodeRegistrar;
    @Autowired public WorkStateManager workStateManager;
    @Autowired public WorkersManager workersManager;
    @Autowired public UpAndDown upAndDown;
    @Autowired public LightweightTaskManager lightweightTaskManager;
    @Autowired public TaskSynchronizer taskSynchronizer;
    @Autowired public ClusterStatusInformationRetriever clusterStatusInformationRetriever;
    //endregion

    //region Outside beans
    @Autowired public PrismContext prismContext;
    @Autowired public SchemaService schemaService;
    @Autowired public RepositoryService repositoryService;
    @Autowired(required = false) public SqlPerformanceMonitorsCollection sqlPerformanceMonitorsCollection;
    @Autowired public MidpointConfiguration midpointConfiguration;
    @Autowired public SystemConfigurationChangeDispatcher systemConfigurationChangeDispatcher;
    @Autowired public CacheConfigurationManager cacheConfigurationManager;
    @Autowired public CacheRegistry cacheRegistry;
    @Autowired public CounterManager counterManager;
    @Autowired
    @Qualifier("securityContextManager")
    public SecurityContextManager securityContextManager;
    //endregion
}
