package com.healthmonitor.api.controller;

import com.healthmonitor.api.model.AlertRecord;
import com.healthmonitor.api.repository.AlertRecordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AlertController.class)
class AlertControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AlertRecordRepository alertRecordRepository;

    @Test
    void getAlerts_returnsPage() throws Exception {
        AlertRecord record = new AlertRecord();
        record.setPatientId(1L);
        record.setMessage("Low SpO2: 88");

        when(alertRecordRepository.findByPatientIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(record), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/alerts/1?page=0&size=10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].patientId").value(1))
            .andExpect(jsonPath("$.content[0].message").value("Low SpO2: 88"));
    }
}
