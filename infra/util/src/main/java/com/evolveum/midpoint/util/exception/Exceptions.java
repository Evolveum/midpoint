package com.evolveum.midpoint.util.exception;

public class Exceptions {

    ExceptionMessageHelper messageFrom(Exception e) {
        return ExceptionMessageHelper.from(e);
    }
}
