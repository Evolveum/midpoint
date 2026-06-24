package com.evolveum.midpoint.smart.impl.conndev;

import com.evolveum.axiom.concepts.CheckedFunction;
import com.evolveum.midpoint.util.exception.SystemException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import org.apache.hc.client5.http.ConnectTimeoutException;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpHead;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.entity.mime.HttpMultipartMode;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.StringEntity;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.BooleanSupplier;

public class ServiceClient {

    private static final String SESSION_PATTERN = "{sessionId}";
    private static final String RELATIVE_SESSION_ENDPOINT = "session/{sessionId}";



    private static final JsonNodeFactory JSON_FACTORY = JsonNodeFactory.instance;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String apiBase;

    private static SSLContext trustAllContext;
    private final String sessionId;
    private final CloseableHttpClient client;
    private final SessionRestoration restoration;
    private final SessionRestoration synchronization;
    private final String sessionEndpoint;

    public ServiceClient(String apiBase, String sessionId, SessionRestoration restoration, SessionRestoration synchronization, CloseableHttpClient client) {
        this.client = client;
        this.sessionId = sessionId;
        this.restoration = restoration;
        this.synchronization = synchronization;
        this.apiBase = (apiBase.endsWith("/") ? apiBase : apiBase + "/" );
        this.sessionEndpoint = appendSession(this.apiBase + RELATIVE_SESSION_ENDPOINT);
    }

    public Job postJob(String endpoint, boolean skipCache) throws IOException {
        var job = new Job(apiBase+endpoint, skipCache);
        var request = job.postBuilder();
        job.startJob(request);
        return job;
    }

    public Job postJob(String endpoint, ObjectNode body, boolean skipCache) throws IOException {
        var job = new Job(apiBase+endpoint, skipCache);
        var request = job.postBuilder();
        request.setEntity(new StringEntity(body.toPrettyString(), ContentType.APPLICATION_JSON));
        job.startJob(request);
        return job;
    }

    private String appendSession(String base) {
        return base.replace(SESSION_PATTERN, sessionId);
    }

    public Job postDocumentationJob(String endpoint, InputStream documentation, ObjectNode body, boolean skipCache) throws IOException {
        final MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.EXTENDED);
        builder.addBinaryBody("documentation", documentation, ContentType.create("application/yaml", StandardCharsets.UTF_8), "spec.yml");

        var job = new Job(apiBase+endpoint, skipCache);
        var request = job.postBuilder();
        request.setEntity(builder.build());
        job.startJob(request);
        return job;
    }

    public Job postDocumentationObjectClassJob(String endpoint, String objectClass, InputStream documentation, ObjectNode body, boolean skipCache) throws IOException {
        final MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.EXTENDED);
        builder.addBinaryBody("documentation", documentation, ContentType.create("application/yaml", StandardCharsets.UTF_8), "spec.yml");

        var job = new Job(apiBase+endpoint, skipCache);
        var request = new HttpPost(apiBase+endpoint + "?objectClass=" + objectClass);
        request.setEntity(builder.build());
        job.startJob(request);
        return job;
    }

    public Job postEntityJob(String endpoint, HttpEntity entity, boolean skipCache) throws IOException {
        var job = new Job(apiBase+endpoint, skipCache);
        var request = new HttpPost(apiBase+endpoint);
        request.setEntity(entity);
        job.startJob(request);
        return job;
    }

    public Job postEntityJob(String endpoint, String objectClass, HttpEntity entity, boolean skipCache) throws IOException {
        var job = new Job(apiBase+endpoint, skipCache);
        var request = new HttpPost(apiBase+endpoint + "?objectClass=" + objectClass);
        request.setEntity(entity);
        job.startJob(request);
        return job;
    }

    public RestorationClient synchronizationClient() {
        return new RestorationClient();
    }


    enum JobStatus {
        NEW,
        SUBMITTED,
        FAILED,
        COMPLETED
    }

    public class Job implements AutoCloseable {

        private final String uri;
        private final boolean skipCache;
        private String jobId = null;

        private JobStatus status = JobStatus.NEW;
        private ObjectNode latestResult;

        public Job(String uri, boolean skipCache) {
            this.uri = appendSession(uri);
            this.skipCache =skipCache;
        }

        private String appendSkipCache(String uri) {
            if (!skipCache) {
                return uri;
            }
            return uri + ( !uri.contains("?") ? "?" : "&" ) + "skipCache=true";
        }

        @Override
        public void close(){
            //client.close();
        }

        public HttpPost postBuilder() {
            return new HttpPost(appendSkipCache(uri));
        }

        public void startJob(HttpPost request) throws IOException {
            ensureSessionExists();
            try {
                startJob0(request);
            } catch (ConnectTimeoutException e) {
                startJob0(request);
            }
        }

        public void startJob0(HttpPost request) throws IOException {
            try(var response = client.execute(request)) {
                if (HttpStatus.SC_OK == response.getCode()) {
                    var result = parseJson(response.getEntity().getContent());
                    jobId = result.get("jobId").asText();
                } else {
                    // also we should proceed with something?
                    status = JobStatus.FAILED;
                }
            }
        }

        public void refresh() {
            var request = new HttpGet(uri + "?jobId=" + jobId);
            try(var response = client.execute(request)) {
                if (HttpStatus.SC_OK == response.getCode()) {
                    latestResult = parseJson(response.getEntity().getContent());
                    updateState();
                } else {
                    // FIXME Add unexpected exception
                    status = JobStatus.FAILED;
                }
            } catch (IOException e) {
                status = JobStatus.FAILED;
            }
        }

        private void updateState() {
            var result = latestResult;
            var remoteStatus = latestResult.get("status").asText();
            if (remoteStatus.equals("finished")) {
                status = JobStatus.COMPLETED;
            } else if (remoteStatus.equals("failed")) {
                status = JobStatus.FAILED;
            }
        }

        public boolean isCompleted() {
            return status == JobStatus.COMPLETED || status == JobStatus.FAILED;
        }

        public ObjectNode getResult() {
            Preconditions.checkState(status == JobStatus.COMPLETED);
            return (ObjectNode) latestResult.get("result");
        }

        public boolean process() {
            refresh(); // Gets latest status of job
            return !isCompleted();
        }

        public boolean isFinished() {
            return status == JobStatus.COMPLETED
                    || (status ==  JobStatus.FAILED && getResult() != null && !getResult().isEmpty());
        }

        public <T,E extends Exception> T waitAndProcess(long sleepTime, BooleanSupplier canRun, CheckedFunction<ObjectNode, T, E> transform) throws E {
            while(process()) {
                // FIXME: Here we can provide status message update to task
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new SystemException("Interrupted while waiting for the connector generation service", e);
                }
                if (!canRun.getAsBoolean()) {
                    throw new SystemException("Operation was cancelled while waiting for the connector generation service");
                }
            }
            if (status == JobStatus.FAILED) {
                var errors = extractErrors();
                throw new SystemException("The connector generation service reported a failure"
                        + (errors != null ? ": " + errors : ""));
            }
            if (status == JobStatus.COMPLETED) {
                return transform.apply(getResult());
            }
            throw new SystemException("The connector generation service job did not finish successfully");
        }

        /**
         * Extracts the {@code errors} array (if present) from the latest job response, so that
         * service-side failures are surfaced in the exception message shown in the GUI instead of
         * a generic, contextless error.
         */
        private String extractErrors() {
            if (latestResult == null) {
                return null;
            }
            var errorsNode = latestResult.get("errors");
            if (errorsNode == null || errorsNode.isNull()) {
                var resultNode = latestResult.get("result");
                errorsNode = resultNode != null ? resultNode.get("errors") : null;
            }
            if (errorsNode == null || !errorsNode.isArray() || errorsNode.isEmpty()) {
                return null;
            }
            var sb = new StringBuilder();
            for (var error : errorsNode) {
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append(error.asText());
            }
            return sb.length() == 0 ? null : sb.toString();
        }
    }

    private void ensureSessionExists() throws IOException {
        int code;
        try (var response = client.execute(new HttpHead(sessionEndpoint))) {
            code = response.getCode();
        }
        if (HttpStatus.SC_NOT_FOUND == code) {
            createSession();
        } else if (HttpStatus.SC_OK != code && HttpStatus.SC_NO_CONTENT != code) {
            throw new IOException("Could not determine code-generation session at " + apiBase + ". Status code " + code);
        }
    }

    private void createSession() throws IOException {
        int code;
        try (var response = client.execute(new HttpPost(sessionEndpoint))) {
            code = response.getCode();
        }
        if (code == HttpStatus.SC_OK || code == HttpStatus.SC_CREATED) {
            if (restoration != null) {
                restoration.restore(new RestorationClient());
            }
        } else {
            throw new IOException("Could not create code-generation session at " + sessionEndpoint + ". Status code " + code);
        }
    }

    private ObjectNode parseJson(InputStream content) throws IOException {
        try {
            return MAPPER.readValue(content, ObjectNode.class);
        } finally {
            content.close();
        }
    }

    public interface SessionRestoration {

        void restore(RestorationClient client) throws IOException;

    }

    public class RestorationClient {

        public void put(String apiUri, Supplier<HttpEntity> entitySupplier) throws IOException {
            var uri = appendSession(apiBase + apiUri);
            var request = new HttpPut(uri);
            request.setEntity(entitySupplier.get());
            try (var uploadResponse = client.execute(request)) {
                if (uploadResponse.getCode() >= 200 && uploadResponse.getCode() < 300) {
                    return;
                }
                throw new IOException("Problem uploading content to " + uri + ". Status code " + uploadResponse.getCode());
            }
        }

        public void putDocumentationIfMissing(String apiUri, Supplier<HttpEntity> entitySupplier) throws IOException {
            var uri = appendSession(apiBase + apiUri);
            int checkCode;
            try (var response = client.execute(new HttpHead(uri))) {
                checkCode = response.getCode();
            }
            if (checkCode == HttpStatus.SC_OK || checkCode == HttpStatus.SC_NO_CONTENT) {
                return;
            }
            put(apiUri, entitySupplier);
        }

    }
}
