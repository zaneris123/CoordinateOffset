package com.jtprince.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Utility to print condensed stacktraces for when the lower frames of the stack are irrelevant.
 */
public class PartialStacktraceLogger {
    /**
     * Log a stacktrace based on an exception, including frames between the exception itself and the caller of this
     * log function.
     * @param logger Logger object to log to.
     * @param message Message to include above the stacktrace
     * @param e Exception containing the stacktrace to log
     */
    public static void logStacktrace(Logger logger, String message, Exception e) {
        List<StackTraceElement> stack = new ArrayList<>(Arrays.asList(e.getStackTrace()));
        List<StackTraceElement> current = new ArrayList<>(Arrays.asList(Thread.currentThread().getStackTrace()));

        // Pop off common elements
        final int popBuffer = 2;
        int popped = 0;
        while (stack.size() > popBuffer+1 && current.size() > popBuffer+1 &&
                stack.get(stack.size()-popBuffer).equals(current.get(current.size()-popBuffer))) {
            stack.remove(stack.size()-1);
            current.remove(current.size()-1);
            popped++;
        }

        logger.severe(message +
                "\nCaused by " + e.getClass().getName() + ": " + e.getMessage() + "\n  " +
                stack.stream().map(StackTraceElement::toString).collect(Collectors.joining("\n  ")) +
                "\n (+" + popped + " hidden frames)"
        );
    }

//    public static void recursiveException(int frameCount) {
//        if (frameCount == 0) {
//            throw new ArrayIndexOutOfBoundsException("Test exception only!");
//        } else {
//            recursiveException(frameCount-1);
//        }
//    }
}
