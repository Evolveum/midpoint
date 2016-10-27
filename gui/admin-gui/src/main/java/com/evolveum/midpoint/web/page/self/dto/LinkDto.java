package com.evolveum.midpoint.web.page.self.dto;

import java.io.Serializable;

/**
 * Created by Kate on 23.09.2015.
 */
public class LinkDto implements Serializable {

    private String linkUrl;
    private String linkName;

    public String getLinkUrl() {
        return linkUrl;
    }
}
