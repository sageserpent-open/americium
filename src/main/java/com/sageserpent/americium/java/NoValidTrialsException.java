package com.sageserpent.americium.java;

public class NoValidTrialsException extends RuntimeException {
    public NoValidTrialsException() {
        super("No valid trials were performed.");
    }
}
