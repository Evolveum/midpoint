package com.evolveum.midpoint.schrodinger;

/**
 * Created by Viliam Repan (lazyman).
 */
public class SchrodingerException extends RuntimeException {

    public SchrodingerException(String message) {
        super(message);
    }

    public SchrodingerException(String message, Throwable cause) {
        super(message, cause);
    }

    public SchrodingerException(Throwable cause) {
        super(cause);
    }
}
