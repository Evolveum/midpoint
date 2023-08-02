/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType;

import java.io.Serializable;

public class ShadowStatisticsDto implements Serializable {

   private SynchronizationSituationType situation;
   private int count;
   private final String color;

    public ShadowStatisticsDto(SynchronizationSituationType situation, int count) {
        this.situation = situation;
        this.count = count;
        this.color = prepareColor();
    }

    public void setSituation(SynchronizationSituationType situation) {
        this.situation = situation;
    }

    public SynchronizationSituationType getSituation() {
        return situation;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getCount() {
        return count;
    }

    public String getColor() {
        return color;
    }

    private String prepareColor() {
        if (situation == null) {
            return "rgba(145, 145, 145)";
        }
        return switch (situation) {
            case LINKED -> "rgba(73, 171, 101)";
            case UNLINKED -> "rgba(50, 171, 131)";
            case DISPUTED -> "rgba(100, 44, 44)";
            case DELETED -> "rgba(168, 44, 44))";
            default -> "rgba(145, 145, 165)";
        };

    }
}
