package com.healthmonitor.api.model;

/** One problem found in a reading: its category plus a human readable message. */
public record AlertIssue(AlertType type, String message) {
}
