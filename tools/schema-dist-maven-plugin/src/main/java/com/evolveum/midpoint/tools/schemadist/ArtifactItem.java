/*
 * Copyright (c) 2014 Evolveum
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
 *
 * This file is inspired and uses minor parts of the maven-dependency-plugin by Brian Fox.
 */

package com.evolveum.midpoint.tools.schemadist;

import java.io.File;

import org.apache.maven.artifact.Artifact;
import org.apache.xml.resolver.Catalog;

public class ArtifactItem {

    /**
     * @parameter
     * @required
     */
    private String groupId;

    /**
     * @parameter
     * @required
     */
    private String artifactId;

    /**
     * @parameter
     */
    private String version = null;

    /**
     * @parameter
     * @required
     */
    private String type = "jar";

    /**
     * @parameter
     */
    private String classifier;

    /**
     * @parameter
     */
    private String catalog;				// intentionally no default

    private Artifact artifact;

    private File workDir;

    private Catalog resolveCatalog;

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public void setArtifactId(String artifactId) {
		this.artifactId = artifactId;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getClassifier() {
		return classifier;
	}

	public void setClassifier(String classifier) {
		this.classifier = classifier;
	}

	public String getCatalog() {
		return catalog;
	}

	public void setCatalog(String catalog) {
		this.catalog = catalog;
	}

	public Artifact getArtifact() {
		return artifact;
	}

	public void setArtifact(Artifact artifact) {
		this.artifact = artifact;
	}

	public File getWorkDir() {
		return workDir;
	}

	public void setWorkDir(File workDir) {
		this.workDir = workDir;
	}

	public Catalog getResolveCatalog() {
		return resolveCatalog;
	}

	public void setResolveCatalog(Catalog resolveCatalog) {
		this.resolveCatalog = resolveCatalog;
	}


}
