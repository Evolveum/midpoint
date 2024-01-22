/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * DTO that contains repository run-time configuration and diagnostic information.
 *
 * All information contained in this class are meant for information purposes only.
 * They are not meant to be used by a machine or algorithm, they are meant to be displayed
 * to a human user.
 *
 * @author Radovan Semancik
 *
 */
public class RepositoryDiag implements Serializable {

    /**
     * Short description of the implementation type, e.g. "SQL", "BaseX", "LDAP"
     */
    private String implementationShortName;

    /**
     * Longer (possible multi-line) description of the implementation;
     */
    private String implementationDescription;

    /** True if the repository is H2-based (always `false` for the native repository). */
    private boolean isH2;

    private boolean isEmbedded;

    /**
     * Short description of a driver or a library used to access the repository.
     * It is usually a named of the JDBC driver (e.g. "org.postgresql.Driver") or
     * something equivalent for other implementations (e.g. "Mozilla SDK" for LDAP).
     */
    private String driverShortName;

    /**
     * Version of the driver (if known)
     */
    private String driverVersion;

    /**
     * URL-formatted location of the repository, usually JDBC connection string,
     * LDAP server URL or a similar construction.
     */
    private String repositoryUrl;

    /**
     * Additional repository information that do not fit the structured data above.
     * May be anything that the implementations thinks is important.
     * E.g. a SQL dialect, transaction mode, etc.
     */
    private List<LabeledString> additionalDetails;

    public String getImplementationShortName() {
        return implementationShortName;
    }

    public void setImplementationShortName(String implementationShortName) {
        this.implementationShortName = implementationShortName;
    }

    public String getImplementationDescription() {
        return implementationDescription;
    }

    public void setImplementationDescription(String implementationDescription) {
        this.implementationDescription = implementationDescription;
    }

    public boolean isH2() {
        return isH2;
    }

    public void setH2(boolean h2) {
        isH2 = h2;
    }

    public boolean isEmbedded() {
        return isEmbedded;
    }

    public void setEmbedded(boolean isEmbedded) {
        this.isEmbedded = isEmbedded;
    }

    public String getDriverShortName() {
        return driverShortName;
    }

    public void setDriverShortName(String driverShortName) {
        this.driverShortName = driverShortName;
    }

    public String getDriverVersion() {
        return driverVersion;
    }

    public void setDriverVersion(String driverVersion) {
        this.driverVersion = driverVersion;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public List<LabeledString> getAdditionalDetails() {
        return additionalDetails;
    }

    public void setAdditionalDetails(List<LabeledString> additionalDetails) {
        this.additionalDetails = additionalDetails;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RepositoryDiag that = (RepositoryDiag) o;
        return isEmbedded == that.isEmbedded && Objects.equals(implementationShortName, that.implementationShortName) && Objects.equals(implementationDescription, that.implementationDescription) && Objects.equals(driverShortName, that.driverShortName) && Objects.equals(driverVersion, that.driverVersion) && Objects.equals(repositoryUrl, that.repositoryUrl) && Objects.equals(additionalDetails, that.additionalDetails);
    }

    @Override
    public int hashCode() {
        return Objects.hash(implementationShortName, implementationDescription, isEmbedded, driverShortName, driverVersion, repositoryUrl, additionalDetails);
    }

    @Override
    public String toString() {
        return "RepositoryDiag(implementationShortName=" + implementationShortName + ", isEmbedded=" + isEmbedded
                + ", driverShortName=" + driverShortName + ", driverVersion=" + driverVersion + ", repositoryUrl="
                + repositoryUrl + ", additionalDetails=" + additionalDetails + ")";
    }


}
