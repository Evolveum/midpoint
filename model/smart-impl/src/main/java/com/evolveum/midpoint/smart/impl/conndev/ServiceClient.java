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
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.impl.EnglishReasonPhraseCatalog;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.net.URIBuilder;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.function.BooleanSupplier;

public class ServiceClient {

    private static final String SESSION_PATTERN = "{sessionId}";
    private static final String RELATIVE_SESSION_ENDPOINT = "session/{sessionId}";
    private static final String API_KEY_HEADER = "X-Gravitee-Api-Key";

    private static final JsonNodeFactory JSON_FACTORY = JsonNodeFactory.instance;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Default maximum duration for which transient gateway errors (HTTP 502/504) are tolerated
     * while monitoring remote jobs, before the job is declared failed.
     */
    public static final long DEFAULT_TRANSIENT_ERROR_TIMEOUT_MS = 15_000L;

    private final String apiBase;
    private final long transientErrorTimeoutMs;

    private static SSLContext trustAllContext;

    /**
     * Reentrancy guard for the session restoration/synchronization callbacks: they may themselves
     * trigger {@link #ensureSessionExists()} (e.g. a documentation upload), which must not run the
     * callback again on the same thread.
     */
    private static final ThreadLocal<Boolean> SESSION_CALLBACK_IN_PROGRESS = ThreadLocal.withInitial(() -> false);

    private final String apiKey;
    private final String sessionId;
    private final CloseableHttpClient client;
    private final SessionRestoration restoration;
    private final SessionRestoration synchronization;
    private final String sessionEndpoint;

    public ServiceClient(String apiBase, String apiKey, String sessionId, SessionRestoration restoration, SessionRestoration synchronization, CloseableHttpClient client) {
        this(apiBase, apiKey, sessionId, restoration, synchronization, client, DEFAULT_TRANSIENT_ERROR_TIMEOUT_MS);
    }

    public ServiceClient(String apiBase, String apiKey, String sessionId, SessionRestoration restoration, SessionRestoration synchronization, CloseableHttpClient client, long transientErrorTimeoutMs) {
        this.client = client;
        this.apiKey = apiKey;
        this.sessionId = sessionId;
        this.restoration = restoration;
        this.synchronization = synchronization;
        this.apiBase = (apiBase.endsWith("/") ? apiBase : apiBase + "/" );
        this.transientErrorTimeoutMs = transientErrorTimeoutMs;
        this.sessionEndpoint = appendSession(this.apiBase + RELATIVE_SESSION_ENDPOINT);
    }

    private CloseableHttpResponse execute(ClassicHttpRequest request) throws IOException {
        if (apiKey != null) {
            request.setHeader(API_KEY_HEADER, apiKey);
        }
        return client.execute(request);
    }

    public Job postJob(String endpoint, String apiType, boolean skipCache) throws IOException {
        var job = new Job(apiBase+endpoint, apiType, skipCache);
        var request = job.postBuilder();
        job.startJob(request);
        return job;
    }

    public Job postJob(String endpoint, String apiType, String intent, boolean skipCache) throws IOException {
        var job = new Job(apiBase+endpoint, apiType, intent, skipCache);
        var request = job.postBuilder();
        job.startJob(request);
        return job;
    }

    public Job postJob(String endpoint, ObjectNode body, String apiType, boolean skipCache) throws IOException {
        var job = new Job(apiBase+endpoint, apiType, skipCache);
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

    public SynchronizationClient synchronizationClient() {
        return new SynchronizationClient();
    }


    enum JobStatus {
        NEW,
        SUBMITTED,
        FAILED,
        COMPLETED
    }

    public class Job implements AutoCloseable {

        private final String uri;
        private final String apiType;
        private final String intent;
        private final boolean skipCache;
        private String jobId = null;

        private JobStatus status = JobStatus.NEW;
        private ObjectNode latestResult;

        /**
         * Details of the last HTTP-level failure (status code + response body or exception
         * message), so that the error report shown to the user contains what the remote service
         * actually answered. {@code httpFailureCode} is {@code 0} when the failure did not come
         * from an HTTP response (e.g. a connection-level {@link IOException}).
         */
        private int httpFailureCode;
        private String httpFailureMessage;

        /**
         * Wall-clock time (epoch millis) of the first transient gateway error (502/504) in the
         * current uninterrupted sequence of polls; {@code 0} when no such error is in progress.
         */
        private long transientErrorStart;

        public Job(String uri, boolean skipCache) {
            this(uri, null, skipCache);
        }

        public Job(String uri, String apiType, boolean skipCache) {
            this(uri, apiType, null, skipCache);
        }

        public Job(String uri, String apiType, String intent, boolean skipCache) {
            this.uri = appendSession(uri);
            this.apiType = apiType;
            this.intent = intent;
            this.skipCache =skipCache;
        }

        @Override
        public void close(){
            //client.close();
        }

        public HttpPost postBuilder() {
            try {
                var builder = new URIBuilder(uri);
                if (skipCache) {
                    builder.addParameter("skipCache", "true");
                }
                if (apiType != null) {
                    builder.addParameter("apiType", apiType);
                }
                if (intent != null) {
                    builder.addParameter("intent", intent);
                }
                return new HttpPost(builder.build());
            } catch (URISyntaxException e) {
                throw new SystemException(e);
            }
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
            try(var response = execute(request)) {
                if (HttpStatus.SC_OK == response.getCode()) {
                    var result = parseJson(response.getEntity().getContent());
                    jobId = result.get("jobId").asText();
                } else {
                    // also we should proceed with something?
                    recordHttpFailure(response);
                    status = JobStatus.FAILED;
                }
            }
        }

        public void refresh() {
            var request = new HttpGet(uri + "?jobId=" + jobId);
            try(var response = execute(request)) {
                var code = response.getCode();
                if (HttpStatus.SC_OK == code) {
                    // The service answered successfully, so any transient gateway error sequence is over.
                    transientErrorStart = 0L;
                    latestResult = parseJson(response.getEntity().getContent());
                    updateState();
                } else if (code == HttpStatus.SC_BAD_GATEWAY || code == HttpStatus.SC_GATEWAY_TIMEOUT) {
                    // Transient gateway error (e.g. the service is restarting): tolerate it for
                    // transientErrorTimeoutMs, failing the job only when it keeps recurring.
                    var now = System.currentTimeMillis();
                    if (transientErrorStart == 0L) {
                        transientErrorStart = now;
                    }
                    recordHttpFailure(response);
                    if (transientErrorTimeoutMs <= 0 || now - transientErrorStart > transientErrorTimeoutMs) {
                        status = JobStatus.FAILED;
                    }
                } else {
                    recordHttpFailure(response);
                    status = JobStatus.FAILED;
                }
            } catch (IOException e) {
                httpFailureMessage = e.getMessage();
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
            if (isCompleted()) {
                // Terminal state (e.g. the submission itself failed): do not poll again, so the
                // recorded failure details are not clobbered by a follow-up status request.
                return false;
            }
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
                var httpFailure = httpFailureDetail();
                var message = new StringBuilder("The connector generation service reported a failure");
                if (errors != null) {
                    message.append(":\n").append(errors);
                }
                if (httpFailure != null) {
                    message.append(errors != null ? "\n" : ":").append(httpFailure);
                }
                throw new SystemException(message.toString());
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

        /**
         * Records the status code and (truncated) response body of a failed HTTP response as the
         * job's failure details, so that the error report surfaces what the remote service sent.
         */
        private void recordHttpFailure(CloseableHttpResponse response) {
            httpFailureCode = response.getCode();
            httpFailureMessage = failureDetail(response);
        }

        /**
         * Formats the recorded HTTP-level failure details (status code, reason phrase and response
         * body, or the connection-level exception message) for inclusion in the error report.
         */
        private String httpFailureDetail() {
            if (httpFailureCode != 0) {
                var detail = httpStatusDescription(httpFailureCode);
                return httpFailureMessage != null ? detail + ": " + httpFailureMessage : detail;
            }
            return httpFailureMessage;
        }
    }

    private void ensureSessionExists() throws IOException {
        int code;
        try (var response = execute(new HttpHead(sessionEndpoint))) {
            code = response.getCode();
        }
        if (HttpStatus.SC_NOT_FOUND == code) {
            createSession();
        } else if (HttpStatus.SC_OK == code || HttpStatus.SC_NO_CONTENT == code) {
            // The session exists, but its session-scoped state (documentation, ...) may be stale or
            // incomplete (e.g. files uploaded to the development after the session was created). The
            // synchronization callback is idempotent and (re)establishes whatever is missing.
            runSessionCallback(synchronization);
        } else {
            throw new IOException("Could not determine code-generation session at " + apiBase + ". Status code " + code);
        }
    }

    private void createSession() throws IOException {
        int code;
        try (var response = execute(new HttpPost(sessionEndpoint))) {
            code = response.getCode();
        }
        if (code == HttpStatus.SC_OK || code == HttpStatus.SC_CREATED) {
            runSessionCallback(restoration);
        } else {
            throw new IOException("Could not create code-generation session at " + sessionEndpoint + ". Status code " + code);
        }
    }

    private void runSessionCallback(SessionRestoration callback) throws IOException {
        if (callback == null || Boolean.TRUE.equals(SESSION_CALLBACK_IN_PROGRESS.get())) {
            return;
        }
        SESSION_CALLBACK_IN_PROGRESS.set(true);
        try {
            callback.restore(new RestorationClient());
        } finally {
            SESSION_CALLBACK_IN_PROGRESS.remove();
        }
    }

    private ObjectNode parseJson(InputStream content) throws IOException {
        try {
            return MAPPER.readValue(content, ObjectNode.class);
        } finally {
            content.close();
        }
    }

    /**
     * Reads (up to a few hundred bytes of) the response body of a failed response, for
     * diagnostics. Returns {@code null} when there is no readable body.
     */
    private String failureDetail(CloseableHttpResponse response) {
        if (response.getEntity() == null) {
            return null;
        }
        try (var content = response.getEntity().getContent()) {
            var bytes = content.readNBytes(1024);
            var text = new String(bytes, StandardCharsets.UTF_8).trim();
            return text.isEmpty() ? null : (text.length() > 512 ? text.substring(0, 512) + "…" : text);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Formats a status code with its English reason phrase (e.g. {@code "HTTP 502 Bad Gateway"})
     * for inclusion in error messages.
     */
    private static String httpStatusDescription(int statusCode) {
        var phrase = EnglishReasonPhraseCatalog.INSTANCE.getReason(statusCode, Locale.ENGLISH);
        return "HTTP " + statusCode + (phrase == null || phrase.isEmpty() ? "" : " " + phrase);
    }

    public interface SessionRestoration {

        void restore(RestorationClient client) throws IOException;

    }

    public class RestorationClient {

        public void put(String apiUri, Supplier<HttpEntity> entitySupplier) throws IOException {
            var uri = appendSession(apiBase + apiUri);
            var request = new HttpPut(uri);
            request.setEntity(entitySupplier.get());
            try (var uploadResponse = execute(request)) {
                if (uploadResponse.getCode() >= 200 && uploadResponse.getCode() < 300) {
                    return;
                }
                // Surface the service's response body (e.g. validation errors of a 422) so failures
                // are diagnosable instead of a bare status code.
                var detail = failureDetail(uploadResponse);
                throw new IOException("Problem uploading content to " + uri + ". Status code " + uploadResponse.getCode()
                        + (detail == null ? "" : ". Response: " + detail));
            }
        }

        public void putDocumentationIfMissing(String apiUri, Supplier<HttpEntity> entitySupplier) throws IOException {
            var uri = appendSession(apiBase + apiUri);
            int checkCode;
            try (var response = execute(new HttpHead(uri))) {
                checkCode = response.getCode();
            }
            if (checkCode == HttpStatus.SC_OK || checkCode == HttpStatus.SC_NO_CONTENT) {
                return;
            }
            put(apiUri, entitySupplier);
        }

    }

    /**
     * Client for the documentation synchronization direction: upload dev documentation to the generation
     * service, wait for it to be processed, and pull the processed result back. The service is the source
     * of truth for processed documentation, so callers push with {@link #postDocumentation}, then, once
     * {@link #awaitDocumentation} confirms processing, read the result with {@link #getDocumentation}.
     */
    public class SynchronizationClient {

        private String documentationUri(String docId) {
            return appendSession(apiBase + "session/{sessionId}/documentation/" + docId);
        }

        /**
         * Uploads a single documentation file (multipart {@code documentation} part). The endpoint does
         * not merely store it but hands it to the LLM, chunking it into {@code DocumentationItem}s (source
         * {@code upload}, {@code doc_id == docId}) as an asynchronous job. Returns immediately; use
         * {@link #awaitDocumentation} to wait for the job and {@link #getDocumentation} to pull the result.
         */
        public void postDocumentation(String docId, InputStream documentation, ContentType contentType, String filename) throws IOException {
            ensureSessionExists();

            final MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setMode(HttpMultipartMode.EXTENDED);
            builder.addBinaryBody("documentation", documentation, contentType, filename);

            var uri = documentationUri(docId);
            var request = new HttpPost(uri);
            request.setEntity(builder.build());
            try (var response = execute(request)) {
                if (response.getCode() < 200 || response.getCode() >= 300) {
                    var detail = failureDetail(response);
                    throw new IOException("Could not upload documentation " + docId + " to " + uri + ". Status code "
                            + response.getCode() + (detail == null ? "" : ". Response: " + detail));
                }
            }
        }

        /**
         * One-shot probe of a documentation item ({@code HEAD documentation/{docId}}). Returns
         * {@code 204} (or {@code 200}) once the chunks are persisted, {@code 202} while the upload job
         * is still running, and {@code 404} when neither items nor a pending job exist.
         */
        public int documentationStatus(String docId) throws IOException {
            try (var response = execute(new HttpHead(documentationUri(docId)))) {
                return response.getCode();
            }
        }

        /**
         * Waits until an uploaded documentation has finished processing, by polling
         * {@link #documentationStatus}. Unlike the module job endpoints, the documentation endpoint has no
         * {@code ?jobId=} status envelope; the HEAD status is the contract instead: {@code 204} once the
         * chunks are persisted, {@code 202} while the upload job is still running, and {@code 404} when
         * neither items nor a pending job exist (i.e. the upload failed). Transient gateway errors
         * ({@code 502}/{@code 504}) are tolerated for the configured transient-error timeout window,
         * exactly like in the job monitoring.
         */
        public void awaitDocumentation(String docId, long sleepTime, BooleanSupplier canRun) throws IOException {
            var transientErrorStart = 0L;
            while (true) {
                var code = documentationStatus(docId);
                if (code == HttpStatus.SC_NO_CONTENT || code == HttpStatus.SC_OK) {
                    return;
                }
                if (code == HttpStatus.SC_BAD_GATEWAY || code == HttpStatus.SC_GATEWAY_TIMEOUT) {
                    // Transient gateway error (e.g. the service is restarting): tolerate it for
                    // transientErrorTimeoutMs, same as the job monitoring above.
                    var now = System.currentTimeMillis();
                    if (transientErrorStart == 0L) {
                        transientErrorStart = now;
                    }
                    if (transientErrorTimeoutMs <= 0 || now - transientErrorStart > transientErrorTimeoutMs) {
                        throw new IOException("Documentation " + docId + " was not processed. "
                                + httpStatusDescription(code) + " kept occurring for too long");
                    }
                } else if (code != HttpStatus.SC_ACCEPTED) {
                    throw new IOException("Documentation " + docId + " was not processed. Status code " + code);
                } else {
                    // The upload job is still running, so the service is up: reset the window.
                    transientErrorStart = 0L;
                }
                if (!canRun.getAsBoolean()) {
                    throw new IOException("Cancelled while waiting for documentation " + docId + " to be processed");
                }
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while waiting for documentation " + docId, e);
                }
            }
        }

        /**
         * Fetches a single processed documentation bundle ({@code docId} + its chunks) as raw JSON. Only
         * call this once {@link #awaitDocumentation} has confirmed processing has finished.
         */
        public String getDocumentation(String docId) throws IOException {
            var uri = documentationUri(docId);
            try (var response = execute(new HttpGet(uri))) {
                if (HttpStatus.SC_OK == response.getCode()) {
                    return new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
                }
                throw new IOException("Could not fetch documentation " + docId + " from " + uri + ". Status code " + response.getCode());
            }
        }

    }
}
