/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.upgrade;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

/**
 * URL format: https://download.evolveum.com/midpoint/<VERSION>/midpoint-<VERSION_NUMBER>-dist.zip
 * VERSION can be: 3.4.1, ..., 4.7, latest
 * VERSION_NUMBER can be: 3.4.1, ..., 4.7, SNAPSHOT
 */
public class DistributionManager {

    private static final String DOWNLOAD_URL = "https://download.evolveum.com/midpoint/";

    public static final String LATEST_VERSION = "latest";

    private final File tempDirectory;

    private final boolean ignoreSslErrors;

    public DistributionManager(@NotNull File tempDirectory, boolean ignoreSslErrors) {
        this.tempDirectory = tempDirectory;
        this.ignoreSslErrors = ignoreSslErrors;
    }

    public File downloadDistribution(@NotNull String version, ProgressListener listener) throws IOException, NoSuchAlgorithmException, KeyManagementException {
        String distributionFile = createFileName(version);

        File file = new File(tempDirectory, System.currentTimeMillis() + "-" + distributionFile);
        FileUtils.forceMkdirParent(file);
        file.createNewFile();

        String url = DOWNLOAD_URL + version + "/" + distributionFile;
        Request request = new Request.Builder()
                .url(url)
                .build();

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .addNetworkInterceptor(chain -> {
                    Response originalResponse = chain.proceed(chain.request());
                    return originalResponse.newBuilder()
                            .body(new ProgressResponseBody(originalResponse.body(), listener))
                            .build();
                });

        if (ignoreSslErrors) {
            X509TrustManager tm = new EmptyTrustManager();
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[] { tm }, null);
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            builder.sslSocketFactory(sslSocketFactory, tm);
            builder.hostnameVerifier((hostname, session) -> true);
        }

        OkHttpClient client = builder.build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Couldn't download distribution with version " + version
                        + ". Unexpected code " + response.code() + ", message: " + response.message() + " for url " + url);
            }

            try (InputStream is = response.body().byteStream()) {
                FileUtils.copyInputStreamToFile(is, file);
            }
        }

        return file;
    }

    private String createFileName(String versionNumber) {
        return "midpoint-" + versionNumber + "-dist.zip";
    }

    private static final class EmptyTrustManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {

        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
