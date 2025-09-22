package com.evolveum.midpoint.smart.impl.conndev.activity;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunResult;
import com.evolveum.midpoint.repo.common.activity.run.LocalActivityRun;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.impl.conndev.ConnectorDevelopmentBackend;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;

@Component
public class DownloadConnectorActivityHandler
        extends AbstractConnDevActivityHandler<DownloadConnectorActivityHandler.WorkDefinition, DownloadConnectorActivityHandler> {

    private static final Trace LOGGER = TraceManager.getTrace(DownloadConnectorActivityHandler.class);

    private static final String ARCHETYPE_OID = SystemObjectsType.ARCHETYPE_UTILITY_TASK.value();

    public DownloadConnectorActivityHandler() {
        super(
                ConnDevDownloadConnectorWorkDefinitionType.COMPLEX_TYPE,
                WorkDefinitionsType.F_INSTALL_CONNECTOR,
                ConnDevDownloadConnectorWorkStateType.COMPLEX_TYPE,
                DownloadConnectorActivityHandler.WorkDefinition.class,
                DownloadConnectorActivityHandler.WorkDefinition::new
                );
    }

    @Override
    public AbstractActivityRun<DownloadConnectorActivityHandler.WorkDefinition, DownloadConnectorActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<DownloadConnectorActivityHandler.WorkDefinition, DownloadConnectorActivityHandler> context,
            @NotNull OperationResult result) {
        return new MyActivityRun(context);
    }

    public static class WorkDefinition extends AbstractWorkDefinition<ConnDevDownloadConnectorWorkDefinitionType> {

        final String connectorUrl;

        public WorkDefinition(WorkDefinitionFactory.@NotNull WorkDefinitionInfo info) throws ConfigurationException {
            super(info, false);
            connectorUrl = MiscUtil.configNonNull(typedDefinition.getConnectorUrl(), "No Base template URL specified");
        }
    }

    public static class MyActivityRun
            extends LocalActivityRun<
            DownloadConnectorActivityHandler.WorkDefinition,
            DownloadConnectorActivityHandler,
            FocusTypeSuggestionWorkStateType> {

        MyActivityRun(
                ActivityRunInstantiationContext<DownloadConnectorActivityHandler.WorkDefinition, DownloadConnectorActivityHandler> context) {
            super(context);
            setInstanceReady();
        }

        @Override
        protected @NotNull ActivityRunResult runLocally(OperationResult result) throws CommonException {

            var task = getRunningTask();
            var beans = ConnDevBeans.get();
            var connectorUrl = getWorkDefinition().connectorUrl;


            // Download template
            var targetName = connectorUrl.substring(connectorUrl.lastIndexOf('/') + 1);

            var template = beans.connectorService.writeConnector(targetName, stream -> {
                try {
                    beans.downloadFile(new URL(getWorkDefinition().connectorUrl), stream);
                } catch (IOException e) {
                    throw new SystemException("Couldn't download connector", e);
                }
            });

            // Install template
            var lookups = template.install(result);

            var lookup = lookups.get(0);

            var query = PrismContext.get().queryFor(ConnectorType.class)
                    .item(ConnectorType.F_CONNECTOR_BUNDLE).eq(lookup.getConnectorBundle())
                    .and().item(ConnectorType.F_CONNECTOR_TYPE).eq(lookup.getConnectorType())
                    .and().item(ConnectorType.F_CONNECTOR_VERSION).eq(lookup.getConnectorVersion())
                    .build();


            var connectors = beans.modelService.searchObjects(ConnectorType.class, query, null, task, result);
            var connector = connectors.get(0);

            var state = getActivityState();



            state.setWorkStateItemRealValues(FocusTypeSuggestionWorkStateType.F_RESULT,new ConnDevDownloadConnectorResultType()
                    .connectorRef(connector.getOid(), ConnectorType.COMPLEX_TYPE));
            state.flushPendingTaskModifications(result);
            return ActivityRunResult.success();
        }
    }
}
