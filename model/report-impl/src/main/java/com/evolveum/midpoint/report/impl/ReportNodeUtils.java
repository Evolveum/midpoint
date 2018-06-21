/*
 * Copyright (c) 2010-2018 Evolveum
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

package com.evolveum.midpoint.report.impl;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;

import com.evolveum.midpoint.schema.util.ReportTypeUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;

public class ReportNodeUtils {
	
    private static final Trace LOGGER = TraceManager.getTrace(ReportNodeUtils.class);
    private static final String SPACE = "%20";
    private static final String ENDPOINTURIPATH = "/report";
    
    public static InputStream executeOperation(String host, String fileName, String intraClusterHttpUrlPattern, String operation) throws CommunicationException, SecurityViolationException, ObjectNotFoundException, ConfigurationException, IOException {
        fileName = fileName.replaceAll("\\s", SPACE);
        InputStream inputStream = null;
        InputStream entityContent = null;
        LOGGER.trace("About to initiate connection with {}", host);
        try {
            if (StringUtils.isNotEmpty(intraClusterHttpUrlPattern)) {
                LOGGER.trace("The cluster uri pattern: {} ", intraClusterHttpUrlPattern);
                URI requestUri = buildURI(intraClusterHttpUrlPattern, host, fileName);
                fileName = URLDecoder.decode(fileName, ReportTypeUtil.URLENCODING);
                
                LOGGER.debug("Sending request to the following uri: {} ", requestUri);
                HttpRequestBase httpRequest = buildHttpRequest(operation);
                httpRequest.setURI(requestUri);
                httpRequest.setHeader("User-Agent", ReportTypeUtil.HEADER_USERAGENT);
                HttpClient client = HttpClientBuilder.create().build();
                try (CloseableHttpResponse response = (CloseableHttpResponse) client.execute(httpRequest)) {
                    HttpEntity entity = response.getEntity();
                    Integer statusCode = response.getStatusLine().getStatusCode();

                    if (statusCode == HttpStatus.SC_OK) {
                        LOGGER.debug("Response OK, the file successfully returned by the cluster peer. ");
                        if (entity != null) {
                            entityContent = entity.getContent();
                            ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
                            byte[] buffer = new byte[1024];
                            int len;
                            while ((len = entityContent.read(buffer)) > -1) {
                                arrayOutputStream.write(buffer, 0, len);
                            }
                            arrayOutputStream.flush();
                            inputStream = new ByteArrayInputStream(arrayOutputStream.toByteArray());
                        }
                    } else if (statusCode == HttpStatus.SC_NO_CONTENT) {
                        if (HttpDelete.METHOD_NAME.equals(operation)) {
                            LOGGER.info("Deletion of the file {} was successful.", fileName);
                        }
                    } else if (statusCode == HttpStatus.SC_FORBIDDEN) {
                        LOGGER.error("The access to the report with the name {} is forbidden.", fileName);
                        String error = "The access to the report " + fileName + " is forbidden.";
                        throw new SecurityViolationException(error);
                    } else if (statusCode == HttpStatus.SC_NOT_FOUND) {
                        String error = "The report file " + fileName + " was not found on the originating nodes filesystem.";
                        throw new ObjectNotFoundException(error);
                    }
                } catch (ClientProtocolException e) {
                    String error = "An exception with the communication protocol has occurred during a query to the cluster peer. " +
                                    e.getLocalizedMessage();
                    throw new CommunicationException(error);
                }
            } else {
                LOGGER.error("Cluster pattern parameters is empty, please refer to the documentation and set up the parameter value accordingly");
                throw new ConfigurationException("Cluster pattern parameters is empty, please refer to the documentation and set up the parameter value accordingly");
            }
        } catch (URISyntaxException e1) {
            throw new CommunicationException("Invalid uri syntax: " + e1.getLocalizedMessage());
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("Unhandled exception when listing nodes");
            LoggingUtils.logUnexpectedException(LOGGER, "Unhandled exception when listing nodes", e);
        } finally {
            IOUtils.closeQuietly(entityContent);
        }

        return inputStream;
    }
    
    private static URI buildURI(String intraClusterHttpUrlPattern, String host, String fileName) throws URISyntaxException {
    		String path = intraClusterHttpUrlPattern.replace("$host", host) + ENDPOINTURIPATH;
        URIBuilder uriBuilder = new URIBuilder(path);
        uriBuilder.setParameter(ReportTypeUtil.FILENAMEPARAMETER, fileName);
        return uriBuilder.build();
    }

    private static HttpRequestBase buildHttpRequest(String typeOfRequest) {
        HttpRequestBase httpRequest;

        if (HttpDelete.METHOD_NAME.equals(typeOfRequest)) {
            httpRequest = new HttpDelete();
        } else {
            httpRequest = new HttpGet();
        }
        return httpRequest;
    }

}
