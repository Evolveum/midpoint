package com.evolveum.midpoint.task.quartzimpl;

import com.evolveum.midpoint.repo.api.SystemConfigurationChangeListener;
import com.evolveum.midpoint.util.logging.Trace;

import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;

import com.evolveum.midpoint.repo.api.SystemConfigurationChangeDispatcher;
import com.evolveum.midpoint.schema.result.OperationResult;

import org.springframework.stereotype.Component;

@DependsOn({"taskManager", "upAndDown"})
@Component
public class TaskManagerInitializer implements SystemConfigurationChangeListener {

    private static final Trace LOGGER = TraceManager.getTrace(TaskManagerInitializer.class);

    private static final String DOT_IMPL_CLASS = TaskManagerInitializer.class.getName() + ".";

    @Autowired private UpAndDown upAndDown;
    @Autowired private SystemConfigurationChangeDispatcher systemConfigurationChangeDispatcher;
    @Autowired private TaskManagerQuartzImpl taskManager;

    //region Initialization and shutdown
    @PostConstruct
    public void init() {
        OperationResult result = new OperationResult(DOT_IMPL_CLASS + "init");
        systemConfigurationChangeDispatcher.registerListener(this);
        upAndDown.init(result);
    }

    @PreDestroy
    public void destroy() {
        OperationResult result = new OperationResult(DOT_IMPL_CLASS + "shutdown");
        systemConfigurationChangeDispatcher.unregisterListener(this);
        upAndDown.shutdown(result);
    }

    /**
     * Called when the whole application is initialized.
     *
     * Here we make this node a real cluster member: We set the operational state to UP, enabling receiving cache invalidation
     * events (among other effects). We also invalidate local caches - to begin with a clean slate - and start the scheduler.
     *
     * The postInit mechanism cannot be used for this purpose. The reason is that it is invoked shortly before the application
     * is completely up. REST endpoints are not yet functional at that time. This means that some cache invalidation
     * messages could be lost, and the other nodes could get error messages in the meanwhile.
     *
     * Unfortunately, REST endpoints are not initialized even when this event is emitted. There's a few seconds before
     * they are really available. So the real action can be delayed by setting "nodeStartupDelay" configuration parameter.
     * (This is a temporary solution until something better is found.)
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onSystemStarted() {
        OperationResult result = new OperationResult(DOT_IMPL_CLASS + "onSystemStarted");
        upAndDown.onSystemStarted(result);
    }

    /**
     * Stops the local tasks as soon as we know we are going down - without waiting for {@link PreDestroy} method on Spring
     * beans in this module is called. The latter is too late for us. We need all background tasks to stop before midPoint
     * is torn down to pieces.
     *
     * Otherwise, incorrect processing is experienced, like live sync events being emitted to nowhere - see e.g. MID-7648.
     */
    @EventListener(ContextClosedEvent.class)
    public void onSystemShutdown() {
        OperationResult result = new OperationResult(DOT_IMPL_CLASS + "onSystemShutdown");
        upAndDown.stopLocalSchedulerAndTasks(result);
    }
    //endregion

    @Override
    public void update(@Nullable SystemConfigurationType value) {
        taskManager.update(value);
    }
}
