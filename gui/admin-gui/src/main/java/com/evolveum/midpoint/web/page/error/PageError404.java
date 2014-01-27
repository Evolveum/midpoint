package com.evolveum.midpoint.web.page.error;

import com.evolveum.midpoint.web.application.PageDescriptor;

/**
 * @author lazyman
 */
@PageDescriptor(url = "/error/404")
public class PageError404 extends PageError {

    public PageError404() {
        super(404);
    }
}
