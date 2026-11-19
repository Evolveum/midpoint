/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.testing.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import com.evolveum.midpoint.authentication.impl.module.authentication.NodeAuthenticationTokenImpl;
import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.gui.test.TestMidPointSpringApplication;
import com.evolveum.midpoint.rest.impl.ClusterRestController;
import com.evolveum.midpoint.test.AbstractIntegrationTest;

/**
 * Tests cluster report-file operations, including streaming,
 * path validation, and file deletion.
 */
@ActiveProfiles("test")
@SpringBootTest(classes = TestMidPointSpringApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestClusterRestController extends AbstractIntegrationTest {

    @Autowired private ClusterRestController clusterRestController;
    @Autowired private MidpointConfiguration midpointConfiguration;

    @AfterMethod(alwaysRun = true)
    public void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void test260ReportFileResponseIsStreamed() throws Exception {
        byte[] expectedContent = "MID-13003 streamed report".getBytes(StandardCharsets.UTF_8);
        Path exportDirectory = reportDirectory("export");
        Files.createDirectories(exportDirectory);
        Path reportFile = Files.createTempFile(exportDirectory, "mid-13003-", ".txt");
        Files.write(reportFile, expectedContent);

        try {
            var response = retrieveReportFile(reportFile.getFileName().toString());

            assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.OK);
            assertThat(response.getHeaders().getContentType())
                    .isEqualTo(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM);
            assertThat(readResource(response)).containsExactly(expectedContent);
            Files.delete(reportFile);
            assertThat(reportFile).doesNotExist();
        } finally {
            Files.deleteIfExists(reportFile);
        }
    }

    @Test
    public void test270ReportFilesInSupportedDirectoriesAreAccessible() throws Exception {
        byte[] expectedContent = "MID-13003 contained report".getBytes(StandardCharsets.UTF_8);

        for (String selector : List.of("export", "import", "trace")) {
            Path directory = reportDirectory(selector);
            Files.createDirectories(directory);
            List<String> prefixes = "export".equals(selector) ? List.of("", "export/") : List.of(selector + "/");
            for (String prefix : prefixes) {
                Path reportFile = Files.createTempFile(directory, "mid-13003-", ".txt");
                Files.write(reportFile, expectedContent);
                String requestFileName = prefix + reportFile.getFileName();

                try {
                    var response = retrieveReportFile(requestFileName);

                    assertThat(response.getStatusCode()).as("GET status for %s", requestFileName)
                            .isEqualTo(org.springframework.http.HttpStatus.OK);
                    assertThat(readResource(response)).containsExactly(expectedContent);
                    assertThat(deleteReportFile(requestFileName).getStatusCode()).as("DELETE status for %s", requestFileName)
                            .isEqualTo(org.springframework.http.HttpStatus.OK);
                    assertThat(reportFile).doesNotExist();
                } finally {
                    Files.deleteIfExists(reportFile);
                }
            }
        }
    }

    @Test
    public void test275UnknownDirectorySelectorFallsBackToExport() throws Exception {
        byte[] expectedContent = "MID-13003 export fallback".getBytes(StandardCharsets.UTF_8);
        Path exportDirectory = reportDirectory("export");
        Files.createDirectories(exportDirectory);
        Path reportFile = Files.createTempFile(exportDirectory, "mid-13003-", ".txt");
        Files.write(reportFile, expectedContent);
        String requestFileName = "unsupported/" + reportFile.getFileName();

        try {
            var response = retrieveReportFile(requestFileName);

            assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.OK);
            assertThat(readResource(response)).containsExactly(expectedContent);
            assertThat(deleteReportFile(requestFileName).getStatusCode())
                    .isEqualTo(org.springframework.http.HttpStatus.OK);
            assertThat(reportFile).doesNotExist();
        } finally {
            Files.deleteIfExists(reportFile);
        }
    }

    @Test
    public void test280ReportFilePathTraversalIsForbidden() throws Exception {
        Path exportDirectory = reportDirectory("export");
        Files.createDirectories(exportDirectory);
        Path outsideFile = Files.createTempFile(exportDirectory.getParent(), "mid-13003-outside-", ".txt");
        byte[] outsideContent = "outside report directory".getBytes(StandardCharsets.UTF_8);
        Files.write(outsideFile, outsideContent);

        try {
            List<String> forbiddenFileNames = List.of(
                    "..\\" + outsideFile.getFileName(),
                    outsideFile.toAbsolutePath().toString(),
                    "C:\\outside.txt",
                    "export/../" + outsideFile.getFileName(),
                    "export\\..\\outside.txt",
                    "export/..",
                    "trace/nested/report.txt",
                    "");

            for (String fileName : forbiddenFileNames) {
                assertReportFileRequestForbidden(fileName);
            }

            String fallbackFileName = "../" + outsideFile.getFileName();
            assertThat(retrieveReportFile(fallbackFileName).getStatusCode())
                    .isEqualTo(org.springframework.http.HttpStatus.NOT_FOUND);
            assertThat(deleteReportFile(fallbackFileName).getStatusCode())
                    .isEqualTo(org.springframework.http.HttpStatus.NOT_FOUND);
            assertThat(exportDirectory.resolve("outside.txt")).doesNotExist();
            assertThat(retrieveReportFile("/outside.txt").getStatusCode())
                    .isEqualTo(org.springframework.http.HttpStatus.NOT_FOUND);
            assertThat(deleteReportFile("/outside.txt").getStatusCode())
                    .isEqualTo(org.springframework.http.HttpStatus.NOT_FOUND);
            assertThat(Files.readAllBytes(outsideFile)).containsExactly(outsideContent);
        } finally {
            Files.deleteIfExists(outsideFile);
        }
    }

    @Test
    public void test290SymbolicLinkOutsideReportDirectoryIsForbidden() throws Exception {
        Path exportDirectory = reportDirectory("export");
        Files.createDirectories(exportDirectory);
        Path outsideFile = Files.createTempFile(exportDirectory.getParent(), "mid-13003-symlink-target-", ".txt");
        Path link = exportDirectory.resolve("mid-13003-link-" + UUID.randomUUID() + ".txt");
        byte[] outsideContent = "outside symlink target".getBytes(StandardCharsets.UTF_8);
        Files.write(outsideFile, outsideContent);

        try {
            try {
                Files.createSymbolicLink(link, outsideFile);
            } catch (UnsupportedOperationException | SecurityException | FileSystemException e) {
                throw new SkipException("Symbolic links are not available for this test: " + e.getMessage());
            }

            assertReportFileRequestForbidden(link.getFileName().toString());
            assertThat(Files.readAllBytes(outsideFile)).containsExactly(outsideContent);
            assertThat(Files.isSymbolicLink(link)).isTrue();
        } finally {
            Files.deleteIfExists(link);
            Files.deleteIfExists(outsideFile);
        }
    }

    @Test
    public void test300MissingReportFileRemainsNotFound() throws Exception {
        Path exportDirectory = reportDirectory("export");
        Files.createDirectories(exportDirectory);
        Path missingFile = Files.createTempFile(exportDirectory, "mid-13003-missing-", ".txt");
        Files.delete(missingFile);

        assertThat(retrieveReportFile(missingFile.getFileName().toString()).getStatusCode())
                .isEqualTo(org.springframework.http.HttpStatus.NOT_FOUND);
        assertThat(deleteReportFile(missingFile.getFileName().toString()).getStatusCode())
                .isEqualTo(org.springframework.http.HttpStatus.NOT_FOUND);
    }

    private ResponseEntity<?> retrieveReportFile(String fileName) {
        setTestNodeAuthentication();
        return clusterRestController.getReportFile(fileName);
    }

    private ResponseEntity<?> deleteReportFile(String fileName) {
        setTestNodeAuthentication();
        return clusterRestController.deleteReportFile(fileName);
    }

    private void assertReportFileRequestForbidden(String fileName) {
        var getStatus = retrieveReportFile(fileName).getStatusCode();
        var deleteStatus = deleteReportFile(fileName).getStatusCode();

        assertThat(getStatus).as("GET status for %s", fileName)
                .isEqualTo(org.springframework.http.HttpStatus.FORBIDDEN);
        assertThat(deleteStatus).as("DELETE status for %s", fileName)
                .isEqualTo(org.springframework.http.HttpStatus.FORBIDDEN);
    }

    private byte[] readResource(ResponseEntity<?> response) throws Exception {
        assertThat(response.getBody()).isInstanceOf(Resource.class);
        Resource resource = (Resource) response.getBody();
        var output = new MockHttpOutputMessage();
        new ResourceHttpMessageConverter().write(resource, org.springframework.http.MediaType.APPLICATION_OCTET_STREAM, output);
        return output.getBodyAsBytes();
    }

    private void setTestNodeAuthentication() {
        var authentication = new NodeAuthenticationTokenImpl(
                taskManager.getLocalNode().asPrismObject(),
                "127.0.0.1",
                List.of());
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private Path reportDirectory(String selector) {
        if ("trace".equals(selector)) {
            return Path.of(midpointConfiguration.getMidpointHome(), "trace");
        }
        String configKey = "import".equals(selector) ? "importFolder" : "exportFolder";
        String configuredPath = midpointConfiguration
                .getConfiguration(MidpointConfiguration.WEB_APP_CONFIGURATION)
                .getString(configKey);
        if (configuredPath == null || configuredPath.isEmpty()) {
            configuredPath = "export";
        }
        return configuredPath.startsWith("/")
                ? Path.of(configuredPath)
                : Path.of(midpointConfiguration.getMidpointHome(), configuredPath);
    }
}
