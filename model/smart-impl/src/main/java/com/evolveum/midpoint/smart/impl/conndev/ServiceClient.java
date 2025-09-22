package com.evolveum.midpoint.smart.impl.conndev;

import com.evolveum.axiom.concepts.CheckedFunction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
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

public class ServiceClient {

    private static final JsonNodeFactory JSON_FACTORY = JsonNodeFactory.instance;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String apiBase;

    private static SSLContext trustAllContext;
    private CloseableHttpClient client;

    public ServiceClient(String apiBase, CloseableHttpClient client) {
        this.client = client;
        this.apiBase = apiBase.endsWith("/") ? apiBase : apiBase + "/";
    }

    public Job postJob(String endpoint, ObjectNode body) throws IOException {
        var job = new Job(apiBase+endpoint);
        var request = job.postBuilder();
        request.setEntity(new StringEntity(body.toPrettyString(), ContentType.APPLICATION_JSON));
        job.startJob(request);
        return job;
    }

    public Job postDocumentationJob(String endpoint, InputStream documentation, ObjectNode body) throws IOException {
        final MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.EXTENDED);
        builder.addBinaryBody("documentation", documentation, ContentType.create("application/yaml", StandardCharsets.UTF_8), "spec.yml");

        var job = new Job(apiBase+endpoint);
        var request = job.postBuilder();
        request.setEntity(builder.build());
        job.startJob(request);
        return job;
    }

    public Job postDocumentationObjectClassJob(String endpoint, String objectClass, InputStream documentation, ObjectNode body) throws IOException {
        final MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.EXTENDED);
        builder.addBinaryBody("documentation", documentation, ContentType.create("application/yaml", StandardCharsets.UTF_8), "spec.yml");

        var job = new Job(apiBase+endpoint);
        var request = new HttpPost(apiBase+endpoint + "?objectClass=" + objectClass);
        request.setEntity(builder.build());
        job.startJob(request);
        return job;
    }

    public Job postEntityJob(String endpoint, HttpEntity entity) throws IOException {
        var job = new Job(apiBase+endpoint);
        var request = new HttpPost(apiBase+endpoint);
        request.setEntity(entity);
        job.startJob(request);
        return job;
    }

    public Job postEntityJob(String endpoint, String objectClass, HttpEntity entity) throws IOException {
        var job = new Job(apiBase+endpoint);
        var request = new HttpPost(apiBase+endpoint + "?objectClass=" + objectClass);
        request.setEntity(entity);
        job.startJob(request);
        return job;
    }

    enum JobStatus {
        NEW,
        SUBMITTED,
        FAILED,
        COMPLETED
    }

    public class Job implements AutoCloseable {

        private final String uri;
        private String jobId = null;

        private JobStatus status = JobStatus.NEW;
        private ObjectNode latestResult;

        public Job(String uri) {
            this.uri = uri;
        }

        @Override
        public void close(){
            //client.close();
        }

        public HttpPost postBuilder() {
            return new HttpPost(uri);
        }

        public void startJob(HttpPost request) throws IOException {
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
            return status == JobStatus.COMPLETED;
        }

        public <T,E extends Exception> T waitAndProcess(long sleepTime, CheckedFunction<ObjectNode, T, E> transform) throws E {
            while(process()) {
                // FIXME: Here we can provide status message update to task
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            if (isFinished()) {
                return transform.apply(getResult());
            }
            throw new IllegalStateException("Job has been finished");
        }
    }

    private ObjectNode parseJson(InputStream content) throws IOException {
        try {
            return MAPPER.readValue(content, ObjectNode.class);
        } finally {
            content.close();
        }
    }
}
