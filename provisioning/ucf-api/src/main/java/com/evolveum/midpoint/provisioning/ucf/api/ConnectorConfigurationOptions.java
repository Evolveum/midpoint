package com.evolveum.midpoint.provisioning.ucf.api;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.schema.AbstractOptions;
import com.evolveum.midpoint.schema.processor.CompleteResourceSchema;
import com.evolveum.midpoint.schema.processor.NativeResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchemaFactory;
import com.evolveum.midpoint.schema.result.OperationResult;

import com.evolveum.midpoint.util.ShortDumpable;

import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Options driving the {@link ConnectorInstance#configure(PrismContainerValue, ConnectorConfigurationOptions, OperationResult)}
 * operation.
 *
 * TODO consider whether to include the configuration (a {@link PrismContainerValue}) itself here.
 */
public class ConnectorConfigurationOptions extends AbstractOptions implements Cloneable, Serializable, ShortDumpable {

    /**
     * The list of the object classes which should be generated in schema (empty means "all").
     *
     * The list is immutable. (Ensured by the setter method.)
     */
    @NotNull private final List<QName> generateObjectClasses;

    /** Transforms raw schema to the {@link CompleteResourceSchema}. The connector needs to have full definitions. */
    @NotNull private final CompleteSchemaProvider completeSchemaProvider;

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
        generateObjectClasses = List.of();
        completeSchemaProvider = CompleteSchemaProvider.none();
        doNotCache = null;
    }

    // Assuming generateObjectClasses is immutable. Not wrapping it here, to avoid re-wraps.
    private ConnectorConfigurationOptions(
            @NotNull List<QName> generateObjectClasses,
            @NotNull CompleteSchemaProvider completeSchemaProvider,
            Boolean doNotCache) {
        this.generateObjectClasses = generateObjectClasses;
        this.completeSchemaProvider = completeSchemaProvider;
        this.doNotCache = doNotCache;
    }

    public @NotNull List<QName> getGenerateObjectClasses() {
        return generateObjectClasses;
    }

    public ConnectorConfigurationOptions generateObjectClasses(List<QName> value) {
        return new ConnectorConfigurationOptions(
                value != null ? Collections.unmodifiableList(value) : List.of(),
                completeSchemaProvider,
                doNotCache);
    }

    public @NotNull CompleteSchemaProvider getCompleteSchemaProvider() {
        return completeSchemaProvider;
    }

    public ConnectorConfigurationOptions completeSchemaProvider(@NotNull CompleteSchemaProvider completeSchemaProvider) {
        return new ConnectorConfigurationOptions(generateObjectClasses, completeSchemaProvider, doNotCache);
    }

    public Boolean getDoNotCache() {
        return doNotCache;
    }

    public boolean isDoNotCache() {
        return Boolean.TRUE.equals(doNotCache);
    }

    public ConnectorConfigurationOptions doNotCache(boolean value) {
        return new ConnectorConfigurationOptions(generateObjectClasses, completeSchemaProvider, value);
    }

    @Override
    public void shortDump(StringBuilder sb) {
        appendVal(sb, "generateObjectClasses", generateObjectClasses);
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
