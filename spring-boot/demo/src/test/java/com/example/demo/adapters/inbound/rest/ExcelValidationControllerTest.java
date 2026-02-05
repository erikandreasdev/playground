package com.example.demo.adapters.inbound.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.core.exceptions.ResourceNotFoundException;
import com.example.demo.core.internal.domain.enums.ValidationStatus;
import com.example.demo.core.internal.domain.result.FileMetadata;
import com.example.demo.core.internal.domain.result.ValidationMetrics;
import com.example.demo.core.internal.domain.result.ValidationReport;
import com.example.demo.core.internal.valueobjects.LoadedResource;
import com.example.demo.core.internal.valueobjects.SourceType;
import com.example.demo.core.ports.inbound.ExcelValidationPort;
import com.example.demo.core.ports.outbound.ResourceLoaderPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ExcelValidationController.class)
class ExcelValidationControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockBean private ResourceLoaderPort resourceLoaderPort;
  @MockBean private ExcelValidationPort excelValidationPort;

  @Test
  void validate_shouldReturnReport() throws Exception {
    LoadedResource mockResource =
        new LoadedResource(
            "test.xlsx",
            1024,
            SourceType.FILESYSTEM,
            MediaType.APPLICATION_OCTET_STREAM,
            new ByteArrayResource(new byte[0]));
    when(resourceLoaderPort.loadResource(any())).thenReturn(mockResource);

    ValidationReport report =
        new ValidationReport(
            ValidationStatus.SUCCESS,
            new FileMetadata("test.xlsx", 1024, "FILESYSTEM"),
            new FileMetadata("config.yml", 500, "FILESYSTEM"),
            new ValidationMetrics(10, 10, 0, 0),
            Collections.emptyList(),
            Collections.emptyList());
    when(excelValidationPort.validate(any(), any(), anyBoolean())).thenReturn(report);

    ValidationRequest request = new ValidationRequest("test.xlsx", "config.yml", false);

    mockMvc
        .perform(
            post("/api/excel/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());
  }

  @Test
  void validate_shouldDefaultPersistToFalse() throws Exception {
    LoadedResource mockResource =
        new LoadedResource(
            "test.xlsx",
            1024,
            SourceType.FILESYSTEM,
            MediaType.APPLICATION_OCTET_STREAM,
            new ByteArrayResource(new byte[0]));
    when(resourceLoaderPort.loadResource(any())).thenReturn(mockResource);

    ValidationReport report =
        new ValidationReport(
            ValidationStatus.SUCCESS,
            new FileMetadata("test.xlsx", 1024, "FILESYSTEM"),
            new FileMetadata("config.yml", 500, "FILESYSTEM"),
            new ValidationMetrics(10, 10, 0, 0),
            Collections.emptyList(),
            Collections.emptyList());
    when(excelValidationPort.validate(any(), any(), anyBoolean())).thenReturn(report);

    ValidationRequest request = new ValidationRequest("test.xlsx", "config.yml", null);

    mockMvc
        .perform(
            post("/api/excel/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());
  }

  @Test
  void validate_shouldReturnNotFound_whenResourceMissing() throws Exception {
    when(resourceLoaderPort.loadResource(any()))
        .thenThrow(new ResourceNotFoundException("File not found"));

    ValidationRequest request = new ValidationRequest("missing.xlsx", "config.yml", false);

    mockMvc
        .perform(
            post("/api/excel/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound());
  }

  @Test
  void validate_shouldReturnBadRequest_whenInputInvalid() throws Exception {
    when(resourceLoaderPort.loadResource(any()))
        .thenThrow(new IllegalArgumentException("Invalid input"));

    ValidationRequest request = new ValidationRequest("", "config.yml", false);

    mockMvc
        .perform(
            post("/api/excel/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }
}
