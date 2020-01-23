/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api;

/**
 * Created by honchar.
 */
public enum SubscriptionType {

    ANNUAL_SUBSRIPTION("01"),
    PLATFORM_SUBSRIPTION("02"),
    DEPLOYMENT_SUBSRIPTION("03"),
    DEVELOPMENT_SUBSRIPTION("04"),
    DEMO_SUBSRIPTION("05");

    private String subscriptionType;

    private SubscriptionType(String subscriptionType){
        this.subscriptionType = subscriptionType;
    }

    public String getSubscriptionType() {
        return subscriptionType;
    }

    public void setSubscriptionType(String subscriptionType) {
        this.subscriptionType = subscriptionType;
    }
}
