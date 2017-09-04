/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.schema;

import java.io.Serializable;
import java.util.List;

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
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((additionalDetails == null) ? 0 : additionalDetails.hashCode());
		result = prime * result + ((driverShortName == null) ? 0 : driverShortName.hashCode());
		result = prime * result + ((driverVersion == null) ? 0 : driverVersion.hashCode());
		result = prime * result + ((implementationDescription == null) ? 0 : implementationDescription.hashCode());
		result = prime * result + ((implementationShortName == null) ? 0 : implementationShortName.hashCode());
		result = prime * result + (isEmbedded ? 1231 : 1237);
		result = prime * result + ((repositoryUrl == null) ? 0 : repositoryUrl.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RepositoryDiag other = (RepositoryDiag) obj;
		if (additionalDetails == null) {
			if (other.additionalDetails != null)
				return false;
		} else if (!additionalDetails.equals(other.additionalDetails))
			return false;
		if (driverShortName == null) {
			if (other.driverShortName != null)
				return false;
		} else if (!driverShortName.equals(other.driverShortName))
			return false;
		if (driverVersion == null) {
			if (other.driverVersion != null)
				return false;
		} else if (!driverVersion.equals(other.driverVersion))
			return false;
		if (implementationDescription == null) {
			if (other.implementationDescription != null)
				return false;
		} else if (!implementationDescription.equals(other.implementationDescription))
			return false;
		if (implementationShortName == null) {
			if (other.implementationShortName != null)
				return false;
		} else if (!implementationShortName.equals(other.implementationShortName))
			return false;
		if (isEmbedded != other.isEmbedded)
			return false;
		if (repositoryUrl == null) {
			if (other.repositoryUrl != null)
				return false;
		} else if (!repositoryUrl.equals(other.repositoryUrl))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "RepositoryDiag(implementationShortName=" + implementationShortName + ", isEmbedded=" + isEmbedded
				+ ", driverShortName=" + driverShortName + ", driverVersion=" + driverVersion + ", repositoryUrl="
				+ repositoryUrl + ", additionalDetails=" + additionalDetails + ")";
	}


}
