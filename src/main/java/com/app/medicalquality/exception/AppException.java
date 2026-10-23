package com.app.medicalquality.exception;

import java.util.Arrays;
import lombok.Getter;

@Getter
public class AppException extends RuntimeException {

    private final String messageKey;
    private final Object[] args;
    private final Object data;

    public AppException(String a_messageKey) {
        this(a_messageKey, null, null);
    }

    /** Second argument is a data payload (not a MessageFormat argument). */
    public AppException(String a_messageKey, Object a_data) {
        this(a_messageKey, null, a_data);
    }

    public AppException(String a_messageKey, Object[] a_args) {
        this(a_messageKey, a_args, null);
    }

    public AppException(String a_messageKey, Object[] a_args, Object a_data) {
        super(formatDebugMessage(a_messageKey, a_args));
        this.messageKey = a_messageKey;
        this.args = a_args == null ? new Object[0] : Arrays.copyOf(a_args, a_args.length);
        this.data = a_data;
    }

    private static String formatDebugMessage(String a_messageKey, Object[] a_args) {
        if (a_args == null || a_args.length == 0) {
            return a_messageKey;
        }
        return a_messageKey + " " + Arrays.toString(a_args);
    }
}
