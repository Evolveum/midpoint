/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task.work;

/**
 * Generally useful constants related to bucketing.
 *
 * They are placed here because they are needed e.g. in tests with no access to `repo-common` module.
 */
public class BucketingConstants {

    public static final String GET_WORK_BUCKET_FOUND_SELF_ALLOCATED = "getWorkBucket.foundSelfAllocated";
    public static final String GET_WORK_BUCKET_CREATED_NEW = "getWorkBucket.createdNew";
    public static final String GET_WORK_BUCKET_DELEGATED = "getWorkBucket.delegated";
    public static final String GET_WORK_BUCKET_NO_MORE_BUCKETS_DEFINITE = "getWorkBucket.noMoreBucketsDefinite";
    public static final String GET_WORK_BUCKET_NO_MORE_BUCKETS_NOT_SCAVENGER = "getWorkBucket.noMoreBucketsNotScavenger";
    public static final String GET_WORK_BUCKET_NO_MORE_BUCKETS_WAIT_TIME_ELAPSED = "getWorkBucket.NoMoreBucketsWaitTimeElapsed";
    public static final String COMPLETE_WORK_BUCKET = "completeWorkBucket";
    public static final String RELEASE_WORK_BUCKET = "releaseWorkBucket";
    public static final String RELEASE_ALL_WORK_BUCKETS = "releaseAllWorkBuckets";
}
