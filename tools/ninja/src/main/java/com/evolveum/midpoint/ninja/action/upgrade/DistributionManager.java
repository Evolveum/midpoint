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

    private File tempDirectory;

    public DistributionManager(@NotNull File tempDirectory) {
        this.tempDirectory = tempDirectory;
    }

    public File downloadDistribution(@NotNull String version, ProgressListener listener) throws IOException {
        String distributionFile = createFileName(version);

        File file = new File(tempDirectory, System.currentTimeMillis() + "-" + distributionFile);
        FileUtils.forceMkdirParent(file);
        file.createNewFile();

        String url = DOWNLOAD_URL + version + "/" + distributionFile;
        Request request = new Request.Builder()
                .url(url)
                .build();

        OkHttpClient client = new OkHttpClient.Builder()
                .addNetworkInterceptor(chain -> {
                    Response originalResponse = chain.proceed(chain.request());
                    return originalResponse.newBuilder()
                            .body(new ProgressResponseBody(originalResponse.body(), listener))
                            .build();
                })
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response.code() + ", message: " + response.message());
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
}
