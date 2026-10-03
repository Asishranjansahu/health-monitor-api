package com.healthmonitor.api.model;

/** Roles supported by the API. Every request must carry a JWT or a device key. */
public enum Role {
    DOCTOR,
    NURSE
}
