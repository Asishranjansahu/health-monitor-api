package com.healthmonitor.api.service;

import com.healthmonitor.api.model.AlertType;

/** Sink for triggered alerts; the Telegram implementation pushes to a chat. */
public interface AlertNotifier {

    void send(Long patientId, AlertType type, String message);
}
