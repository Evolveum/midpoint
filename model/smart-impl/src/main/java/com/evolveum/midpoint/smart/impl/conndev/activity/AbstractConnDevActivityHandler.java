package com.evolveum.midpoint.smart.impl.conndev.activity;

import com.evolveum.midpoint.model.impl.tasks.ModelActivityHandler;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.repo.common.activity.definition.AffectedObjectsInformation;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityStateDefinition;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

public abstract class AbstractConnDevActivityHandler<T extends AbstractConnDevActivityHandler.AbstractWorkDefinition, S extends AbstractConnDevActivityHandler<T,S>>
        extends ModelActivityHandler<T, S> {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractConnDevActivityHandler.class);

    private static final String ARCHETYPE_OID = SystemObjectsType.ARCHETYPE_UTILITY_TASK.value();

    private final QName definitionType;
    private final ItemName definitionName;
    private final QName stateType;
    private final Class<? extends AbstractWorkDefinition> definitionClass;
    private final WorkDefinitionFactory.WorkDefinitionSupplier definitionFactory;

    public AbstractConnDevActivityHandler(QName definitionType, ItemName definitionName, QName stateType, Class<? extends AbstractWorkDefinition> definitionClass, WorkDefinitionFactory.WorkDefinitionSupplier definitionFactory) {
        this.definitionType = definitionType;
        this.definitionName = definitionName;
        this.stateType = stateType;
        this.definitionClass = definitionClass;
        this.definitionFactory = definitionFactory;
    }

    @PostConstruct
    public void register() {
        handlerRegistry.register(definitionType,definitionName,definitionClass,definitionFactory,this);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(definitionType, definitionClass);
    }

    @Override
    public @NotNull ActivityStateDefinition<?> getRootActivityStateDefinition() {
        return ActivityStateDefinition.normal(stateType);
    }

    @Override
    public String getIdentifierPrefix() {
        return definitionName.getLocalPart();
    }

    @Override
    public String getDefaultArchetypeOid() {
        return ARCHETYPE_OID;
    }


    public static abstract class AbstractWorkDefinition<T extends ConnDevBaseWorkDefinitionType> extends com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition {

        final String connectorDevelopmentOid;
        final T typedDefinition;

        public AbstractWorkDefinition(WorkDefinitionFactory.@NotNull WorkDefinitionInfo info) throws ConfigurationException {
            super(info);
            this.typedDefinition = (T) info.getBean();
            connectorDevelopmentOid = MiscUtil.configNonNull(Referencable.getOid(typedDefinition.getConnectorDevelopmentRef()), "No resource OID specified");
        }

        @Override
        public @NotNull AffectedObjectsInformation.ObjectSet getAffectedObjectSetInformation(@Nullable AbstractActivityWorkStateType state) throws SchemaException, ConfigurationException {
            return AffectedObjectsInformation.ObjectSet.repository(new BasicObjectSetType()
                    .objectRef(connectorDevelopmentOid, ConnectorDevelopmentType.COMPLEX_TYPE)
            );
        }

        @Override
        protected void debugDumpContent(StringBuilder sb, int indent) {

        }
    }
}
