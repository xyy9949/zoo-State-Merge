package edu.upenn.zktester.util;

public class AssertionFailureError extends Error {

    public AssertionFailureError(final String message) {
        super(message);
    }
}
