/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.util.task.work;

/**
 * Generally useful constants related to bucketing.
 *
 * They are placed here because they are needed e.g. in tests with no access to `repo-common` module.
 */
public class BucketingConstants {

    public static final String GET_WORK_BUCKET_FOUND_DELEGATED = "getWorkBucket.foundDelegated";
    public static final String GET_WORK_BUCKET_FOUND_READY = "getWorkBucket.foundReady";
    public static final String GET_WORK_BUCKET_CREATED_NEW = "getWorkBucket.createdNew";
    public static final String GET_WORK_BUCKET_NO_MORE_BUCKETS_DEFINITE = "getWorkBucket.noMoreBucketsDefinite";
    public static final String GET_WORK_BUCKET_NO_MORE_BUCKETS_NOT_SCAVENGER = "getWorkBucket.noMoreBucketsNotScavenger";
    public static final String GET_WORK_BUCKET_NO_MORE_BUCKETS_WAIT_TIME_ELAPSED = "getWorkBucket.NoMoreBucketsWaitTimeElapsed";
    public static final String COMPLETE_WORK_BUCKET = "completeWorkBucket";
    public static final String RELEASE_WORK_BUCKET = "releaseWorkBucket";
}
