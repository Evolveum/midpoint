package com.evolveum.midpoint.provisioning.ucf.api;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.schema.AbstractOptions;
import com.evolveum.midpoint.schema.result.OperationResult;

import com.evolveum.midpoint.util.ShortDumpable;

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
     * The list of the object classes which should be generated in schema (null or empty means "all").
     *
     * The list is immutable. Ensured by the setter method.
     */
    private final List<QName> generateObjectClasses;

    /**
     * If set to `true`, the connector configuration is not ready to be shared with other clients,
     * i.e. such connector shouldn't be cached/pooled.
     *
     * Default is `false` i.e. the connector instance should be cached.
     *
     * TODO better name
     */
    private final Boolean doNotCache;

    public static final ConnectorConfigurationOptions DEFAULT = new ConnectorConfigurationOptions();

    public ConnectorConfigurationOptions() {
        generateObjectClasses = null;
        doNotCache = null;
    }

    // Assuming generateObjectClasses is immutable. Not wrapping it here, to avoid re-wraps.
    private ConnectorConfigurationOptions(List<QName> generateObjectClasses, Boolean doNotCache) {
        this.generateObjectClasses = generateObjectClasses;
        this.doNotCache = doNotCache;
    }

    public List<QName> getGenerateObjectClasses() {
        return generateObjectClasses;
    }

    public ConnectorConfigurationOptions generateObjectClasses(List<QName> value) {
        return new ConnectorConfigurationOptions(
                value != null ? Collections.unmodifiableList(value) : null,
                doNotCache);
    }

    public Boolean getDoNotCache() {
        return doNotCache;
    }

    public boolean isDoNotCache() {
        return Boolean.TRUE.equals(doNotCache);
    }

    public ConnectorConfigurationOptions doNotCache(boolean value) {
        return new ConnectorConfigurationOptions(generateObjectClasses, value);
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
}
