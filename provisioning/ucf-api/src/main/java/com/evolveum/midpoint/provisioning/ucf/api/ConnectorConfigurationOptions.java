package com.evolveum.midpoint.provisioning.ucf.api;

import java.io.Serializable;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.AbstractOptions;
import com.evolveum.midpoint.schema.processor.CompleteResourceSchema;
import com.evolveum.midpoint.schema.processor.NativeResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchemaFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * Options driving the {@link ConnectorInstance#configure(ConnectorConfiguration, ConnectorConfigurationOptions, OperationResult)}
 * operation.
 *
 * TODO consider removing, as currently only "do not cache" flag remanined here (but others may appear in the future)
 */
public class ConnectorConfigurationOptions extends AbstractOptions implements Cloneable, Serializable, ShortDumpable {

    /**
     * If set to `true`, the connector configuration is not ready to be shared with other clients,
     * i.e. such connector shouldn't be cached/pooled.
     *
     * Default is `false` i.e. the connector instance should be cached.
     *
     * TODO better name
     */
    private final Boolean doNotCache;

    public ConnectorConfigurationOptions() {
        doNotCache = null;
    }

    private ConnectorConfigurationOptions(Boolean doNotCache) {
        this.doNotCache = doNotCache;
    }

    public boolean isDoNotCache() {
        return Boolean.TRUE.equals(doNotCache);
    }

    public ConnectorConfigurationOptions doNotCache(boolean value) {
        return new ConnectorConfigurationOptions(value);
    }

    @Override
    public void shortDump(StringBuilder sb) {
        appendFlag(sb, "doNotCache", doNotCache);
        removeLastComma(sb);
    }

    @Override
    public ConnectorConfigurationOptions clone() {
        try {
            return (ConnectorConfigurationOptions) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    /** TODO we will probably remove this */
    public interface CompleteSchemaProvider extends Serializable {

        CompleteResourceSchema completeSchema(NativeResourceSchema nativeSchema)
                throws SchemaException, ConfigurationException;

        static @NotNull CompleteSchemaProvider forResource(@NotNull ResourceType resource) {
            return (rawSchema) -> ResourceSchemaFactory.parseCompleteSchema(resource, rawSchema);
        }

        static @NotNull CompleteSchemaProvider none() {
            return (rawSchema) -> {
                throw new UnsupportedOperationException();
            };
        }
    }
}
