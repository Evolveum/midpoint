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

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.List;

import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.xml.resolver.Catalog;
import org.apache.xml.resolver.CatalogManager;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.components.io.fileselectors.IncludeExcludeFileSelector;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.util.DOMUtil;

/**
 * @goal schemadist
 * @requiresDependencyResolution compile
 * @phase package
 */
public class SchemaDistMojo extends AbstractMojo {

    /**
	 * @parameter
	 */
	private String includes;

    /**
     * @parameter
     */
	private String excludes;

	/**
	 * @parameter default-value="${project.build.directory}/schemadist" required=true
	 */
    private File outputDirectory;

	/**
	 * @parameter default-value="${project.build.directory}/schemadist-work" required=true
	 */
    private File workDirectory;

    /**
     * @parameter
     */
    private List<ArtifactItem> artifactItems;

    /**
	 * @parameter default-value="true" required=true
	 */
    private boolean translateSchemaLocation;

    /**
     * @parameter default-value="src/main/schemadoc/resources"
     */
    private File resourcesDir;

    /** @parameter default-value="${project}" */
    private org.apache.maven.project.MavenProject project;

    /** @parameter default-value="${localRepository}" */
    private ArtifactRepository local;

    /** @parameter default-value="${project.remoteArtifactRepositories}" */
    protected List<ArtifactRepository> remoteRepos;

    /**
     * @component
     */
    private ArtifactFactory factory;

    /**
     * @component
     */
    private ArtifactResolver resolver;

    /**
     * @component
     */
    private ArchiverManager archiverManager;

    /**
     * @component
     */
    private MavenProjectHelper projectHelper;

    private void processArtifactItems() throws MojoExecutionException, InvalidVersionSpecificationException {
    	for (ArtifactItem artifactItem : artifactItems) {
    		if (StringUtils.isEmpty(artifactItem.getVersion())) {
                fillMissingArtifactVersion(artifactItem);
            }
    		artifactItem.setArtifact(getArtifact(artifactItem));
    	}
    }

    private void fillMissingArtifactVersion(ArtifactItem artifactItem) throws MojoExecutionException {
    	List<Dependency> deps = project.getDependencies();
        List<Dependency> depMngt = project.getDependencyManagement() == null
            ? Collections.<Dependency>emptyList() : project.getDependencyManagement().getDependencies();

        if ( !findDependencyVersion( artifactItem, deps, false )
            && ( project.getDependencyManagement() == null || !findDependencyVersion( artifactItem, depMngt, false ) )
            && !findDependencyVersion( artifactItem, deps, true )
            && ( project.getDependencyManagement() == null || !findDependencyVersion( artifactItem, depMngt, true ) ) )
        {
            throw new MojoExecutionException(
                "Unable to find artifact version of " + artifactItem.getGroupId() + ":" + artifactItem.getArtifactId()
                    + " in either dependency list or in project's dependency management." );
        }
	}

    private boolean findDependencyVersion(ArtifactItem artifact, List<Dependency> dependencies, boolean looseMatch) {
        for ( Dependency dependency : dependencies ) {
            if ( StringUtils.equals( dependency.getArtifactId(), artifact.getArtifactId() )
                && StringUtils.equals( dependency.getGroupId(), artifact.getGroupId() )
                && ( looseMatch || StringUtils.equals( dependency.getClassifier(), artifact.getClassifier() ) )
                && ( looseMatch || StringUtils.equals( dependency.getType(), artifact.getType() ) ) ) {
                artifact.setVersion( dependency.getVersion() );
                return true;
            }
        }

        return false;
    }

	protected Artifact getArtifact(ArtifactItem artifactItem) throws MojoExecutionException, InvalidVersionSpecificationException {
	    Artifact artifact;

	    VersionRange vr = VersionRange.createFromVersionSpec(artifactItem.getVersion());

	    if (StringUtils.isEmpty(artifactItem.getClassifier())) {
	        artifact = factory.createDependencyArtifact( artifactItem.getGroupId(), artifactItem.getArtifactId(), vr,
	                                                     artifactItem.getType(), null, Artifact.SCOPE_COMPILE );
	    } else {
	        artifact = factory.createDependencyArtifact( artifactItem.getGroupId(), artifactItem.getArtifactId(), vr,
	                                                     artifactItem.getType(), artifactItem.getClassifier(),
	                                                     Artifact.SCOPE_COMPILE );
	    }

	    try {
			resolver.resolve(artifact, remoteRepos, local);
		} catch (ArtifactResolutionException | ArtifactNotFoundException e) {
			throw new MojoExecutionException("Error resolving artifact "+artifact, e);
		}

	    return artifact;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info( "SchemaDist plugin started" );

        try {
			processArtifactItems();
		} catch (InvalidVersionSpecificationException e) {
			handleFailure(e);
		}
        final File outDir = initializeOutDir(outputDirectory);

        CatalogManager catalogManager = new CatalogManager();
        catalogManager.setVerbosity(999);

        for (ArtifactItem artifactItem: artifactItems) {
        	Artifact artifact = artifactItem.getArtifact();
        	getLog().info( "SchemaDist unpacking artifact " + artifact);
        	File workDir = new File(workDirectory, artifact.getArtifactId());
        	initializeOutDir(workDir);
        	artifactItem.setWorkDir(workDir);
        	unpack(artifactItem, workDir);

        	if (translateSchemaLocation) {
				String catalogPath = artifactItem.getCatalog();
				if (catalogPath != null) {
					File catalogFile = new File(workDir, catalogPath);
					if (!catalogFile.exists()) {
						throw new MojoExecutionException(
								"No catalog file " + catalogPath + " in artifact " + artifact);
					}
					Catalog catalog = new Catalog(catalogManager);
					catalog.setupReaders();
					try {
						// UGLY HACK. On Windows, file names like d:\abc\def\catalog.xml eventually get treated very strangely
						// (resulting in names like "file:<current-working-dir>d:\abc\def\catalog.xml" that are obviously wrong)
						// Prefixing such names with "file:/" helps.
						String prefix;
						if (catalogFile.isAbsolute() && !catalogFile.getPath().startsWith("/")) {
							prefix = "/";
						} else {
							prefix = "";
						}
						String fileName = "file:" + prefix + catalogFile.getPath();
						getLog().debug("Calling parseCatalog with: " + fileName);
						catalog.parseCatalog(fileName);
					} catch (MalformedURLException e) {
						throw new MojoExecutionException(
								"Error parsing catalog file " + catalogPath + " in artifact " + artifact, e);
					} catch (IOException e) {
						throw new MojoExecutionException(
								"Error parsing catalog file " + catalogPath + " in artifact " + artifact, e);
					}
					artifactItem.setResolveCatalog(catalog);
				}
			} else {
				getLog().debug("Catalog search disabled for " + artifact);
			}
        }

        for (ArtifactItem artifactItem: artifactItems) {
        	Artifact artifact = artifactItem.getArtifact();
        	getLog().info( "SchemaDist processing artifact " + artifact);
        	final File workDir = artifactItem.getWorkDir();
        	FileVisitor<Path> fileVisitor = new FileVisitor<Path>() {
				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
						throws IOException {
					// nothing to do
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) throws IOException {
					String fileName = filePath.getFileName().toString();
					if (fileName.endsWith(".xsd")) {
						getLog().debug("=======================> Processing file "+filePath);
						try {
							processXsd(filePath, workDir, outDir);
						} catch (MojoExecutionException | MojoFailureException e) {
							throw new RuntimeException(e.getMessage(),e);
						}
					} else if (fileName.endsWith(".wsdl")) {
						getLog().debug("=======================> Processing file "+filePath);
						try {
							processWsdl(filePath, workDir, outDir);
						} catch (MojoExecutionException | MojoFailureException e) {
							throw new RuntimeException(e.getMessage(),e);
						}
					} else {
						getLog().debug("=======================> Skipping file "+filePath);
					}
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
					return FileVisitResult.TERMINATE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					// nothing to do
					return FileVisitResult.CONTINUE;
				}
			};
			try {
				Files.walkFileTree(workDir.toPath(), fileVisitor);
			} catch (IOException e) {
				throw new MojoExecutionException("Error processing files of artifact "+artifact, e);
			}

        }
        getLog().info( "SchemaDist plugin finished" );
    }

	private void processXsd(Path filePath, File workDir, File outDir) throws MojoExecutionException, MojoFailureException {
		Document dom = DOMUtil.parseFile(filePath.toFile());
		Element rootElement = DOMUtil.getFirstChildElement(dom);
		if (translateSchemaLocation) {
			processXsdElement(rootElement, filePath, workDir, outDir);
		}
		serializeXml(dom, filePath, workDir, outDir);
	}

	private void serializeXml(Document dom, Path filePath, File workDir, File outDir) throws MojoFailureException, MojoExecutionException {
		Path fileRelPath = workDir.toPath().relativize(filePath);
		File outFile = new File(outDir, fileRelPath.toString());
		initializeOutDir(outFile.getParentFile());
		try {
			DOMUtil.serializeDOMToFile(dom, outFile);
		} catch (TransformerFactoryConfigurationError | TransformerException e) {
			throw new MojoExecutionException("Error serializing modified file "+fileRelPath+" to XML: "+ e.getMessage(), e);
		}
	}

	private void processXsdElement(Element rootElement, Path filePath, File workDir, File outDir) throws MojoExecutionException, MojoFailureException {
		List<Element> importElements = DOMUtil.getChildElements(rootElement, DOMUtil.XSD_IMPORT_ELEMENT);
		for (Element importElement: importElements) {
			String namespace = DOMUtil.getAttribute(importElement, DOMUtil.XSD_ATTR_NAMESPACE);
			if (DOMUtil.getAttribute(importElement, DOMUtil.XSD_ATTR_SCHEMA_LOCATION) == null) {
				// target-less imports are skipped
				continue;
			}
			getLog().debug("Processing import of '" + namespace + "'...");
			String schemaLocation;
			try {
				schemaLocation = resolveSchemaLocation(namespace, filePath, workDir);
			} catch (IOException e) {
				throw new MojoExecutionException(
						"Error resolving namespace " + namespace + " in file " + filePath + ": " + e.getMessage(), e);
			}
			importElement.setAttribute(DOMUtil.XSD_ATTR_SCHEMA_LOCATION.getLocalPart(), schemaLocation);
		}

		List<Element> includeElements = DOMUtil.getChildElements(rootElement, DOMUtil.XSD_INCLUDE_ELEMENT);
		for (Element includeElement: includeElements) {
			String schemaLocationOriginal = DOMUtil.getAttribute(includeElement, DOMUtil.XSD_ATTR_SCHEMA_LOCATION);
			getLog().debug("Processing include of '" + schemaLocationOriginal + "'...");
			String schemaLocationTranslated;
			try {
				schemaLocationTranslated = resolveSchemaLocation(schemaLocationOriginal, filePath, workDir);
			} catch (IOException e) {
				throw new MojoExecutionException("Error resolving schemaLocation "+schemaLocationOriginal+" in file "+filePath+": "+ e.getMessage(), e);
			}
			includeElement.setAttribute(DOMUtil.XSD_ATTR_SCHEMA_LOCATION.getLocalPart(),
					schemaLocationTranslated);
		}
	}

	private void processWsdl(Path filePath, File workDir, File outDir) throws MojoExecutionException, MojoFailureException {
		Document dom = DOMUtil.parseFile(filePath.toFile());

		if (translateSchemaLocation) {
			Element rootElement = DOMUtil.getFirstChildElement(dom);
			List<Element> importElements = DOMUtil.getChildElements(rootElement, DOMUtil.WSDL_IMPORT_ELEMENT);
			for(Element importElement: importElements) {
				String namespace = DOMUtil.getAttribute(importElement, DOMUtil.WSDL_ATTR_NAMESPACE);
				String schemaLocation;
				try {
					schemaLocation = resolveSchemaLocation(namespace, filePath, workDir);
				} catch (IOException e) {
					throw new MojoExecutionException("Error resolving namespace "+namespace+" in file "+filePath+": "+ e.getMessage(), e);
				}
				importElement.setAttribute(DOMUtil.WSDL_ATTR_LOCATION.getLocalPart(),
						schemaLocation);
			}

			List<Element> typesElements = DOMUtil.getChildElements(rootElement, DOMUtil.WSDL_TYPES_ELEMENT);
			for(Element typesElement: typesElements) {
				processXsdElement(DOMUtil.getFirstChildElement(typesElement),filePath,workDir,outDir);
			}
		}

		serializeXml(dom, filePath, workDir, outDir);
	}

    private String resolveSchemaLocation(String namespaceOrLocation, Path filePath, File workDir) throws MojoExecutionException, IOException {
    	for (ArtifactItem artifactItem: artifactItems) {
    		Catalog catalog = artifactItem.getResolveCatalog();
			if (catalog == null) {
				continue;
			}
            String publicId = namespaceOrLocation;
            if (publicId.endsWith("#")) {
                publicId = publicId.substring(0, publicId.length()-1);
            }
    		String resolvedString = catalog.resolveEntity(filePath.toString(), publicId, publicId);
    		if (resolvedString != null) {
    			getLog().debug("-------------------");
    			getLog().debug("Resolved namespace/schemaLocation "+namespaceOrLocation+" to "+resolvedString+" using catalog "+catalog);
    			URL resolvedUrl = new URL(resolvedString);
    			String resolvedPathString = resolvedUrl.getPath();
    			Path resolvedPath = new File(resolvedPathString).toPath();
    			Path workDirPath = workDir.toPath();

    			Path resolvedRelativeToCatalogWorkdir = artifactItem.getWorkDir().toPath().relativize(resolvedPath);
    			Path fileRelativeToWorkdir = workDirPath.relativize(filePath);

    			getLog().debug("workDirPath: "+workDirPath);
    			getLog().debug("resolvedRelativeToCatalogWorkdir: "+resolvedRelativeToCatalogWorkdir+",  fileRelativeToWorkdir: "+fileRelativeToWorkdir);

    			Path relativePath = fileRelativeToWorkdir.getParent().relativize(resolvedRelativeToCatalogWorkdir);
    			getLog().debug("Rel: "+relativePath);
				String unixSeparators = FilenameUtils.separatorsToUnix(relativePath.toString());
				getLog().debug("Normalized to use UNIX separators: " + unixSeparators);
    			return unixSeparators;
    		}
    	}
		throw new MojoExecutionException("Cannot resolve namespace "+namespaceOrLocation+" in file "+filePath+" using any of the catalogs");
	}

	private File initializeOutDir(File dir) throws MojoFailureException {
        getLog().info("Output dir: "+dir);
        if ( dir.exists() && !dir.isDirectory() ) {
            throw new MojoFailureException("Output directory is not a directory: "+dir);
        }
        if (dir.exists() && !dir.canWrite()) {
            throw new MojoFailureException("Output directory is not writable: "+dir);
        }
        dir.mkdirs();
        return dir;
    }

    private void unpack(ArtifactItem artifactItem, File destDir) throws MojoExecutionException {
    	Artifact artifact = artifactItem.getArtifact();
    	File file = artifact.getFile();
    	if (file == null) {
    		throw new MojoExecutionException("No file for artifact "+artifact);
    	}
    	if (file.isDirectory()) {
    		try {
				FileUtils.copyDirectory(file, destDir);
			} catch (IOException e) {
				throw new MojoExecutionException("Error copying directory "+file+" to "+destDir+": "+e.getMessage(), e);
			}
        } else {
	    	try {
	            UnArchiver unArchiver = archiverManager.getUnArchiver( artifact.getType() );
	            unArchiver.setSourceFile(file);
	            unArchiver.setDestDirectory(destDir);

	            if (StringUtils.isNotEmpty(excludes) || StringUtils.isNotEmpty(includes)) {
	                // Create the selectors that will filter
	                // based on include/exclude parameters
	                // MDEP-47
	                IncludeExcludeFileSelector[] selectors =
	                    new IncludeExcludeFileSelector[]{ new IncludeExcludeFileSelector() };

	                if ( StringUtils.isNotEmpty( excludes ) ) {
	                    selectors[0].setExcludes( excludes.split( "," ) );
	                }

	                if ( StringUtils.isNotEmpty( includes ) ) {
	                    selectors[0].setIncludes( includes.split( "," ) );
	                }

	                unArchiver.setFileSelectors( selectors );
	            }

	            unArchiver.extract();
	    	} catch (ArchiverException | NoSuchArchiverException e) {
	            throw new MojoExecutionException(
	                    "Error unpacking file: " + file + " to: " + destDir + "\r\n" + e.toString(), e );
			}
        }
    }

    private void handleFailure(Exception e) throws MojoFailureException {
    	e.printStackTrace();
    	throw new MojoFailureException(e.getMessage());
	}

}
