package com.example.demo.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.domain.FileMetadata;
import com.example.demo.domain.ValidationMetrics;
import com.example.demo.domain.ValidationReport;
import com.example.demo.domain.ValidationStatus;
import com.example.demo.resource.LoadedResource;
import com.example.demo.resource.ResourceLoaderService;
import com.example.demo.resource.SourceType;
import com.example.demo.service.ExcelValidationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ExcelValidationController.class)
class ExcelValidationControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockBean private ResourceLoaderService resourceLoaderService;
  @MockBean private ExcelValidationService excelValidationService;

  @Test
  void validate_shouldReturnReport() throws Exception {

    LoadedResource mockResource =
        new LoadedResource(
            "test.xlsx",
            1024,
            SourceType.FILESYSTEM,
            org.springframework.http.MediaType.APPLICATION_OCTET_STREAM,
            new org.springframework.core.io.ByteArrayResource(new byte[0]));
    when(resourceLoaderService.loadResource(any())).thenReturn(mockResource);

    ValidationReport report =
        new ValidationReport(
            ValidationStatus.SUCCESS,
            new FileMetadata("test.xlsx", 1024, "FILESYSTEM"),
            new FileMetadata("config.yml", 500, "FILESYSTEM"),
            new ValidationMetrics(10, 10, 0, 0),
            Collections.emptyList(),
            Collections.emptyList());
    when(excelValidationService.validate(any(), any(), anyBoolean())).thenReturn(report);

    ValidationRequest request = new ValidationRequest("test.xlsx", "config.yml", false);

    mockMvc
        .perform(
            post("/api/excel/validate")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
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
            org.springframework.http.MediaType.APPLICATION_OCTET_STREAM,
            new org.springframework.core.io.ByteArrayResource(new byte[0]));
    when(resourceLoaderService.loadResource(any())).thenReturn(mockResource);

    ValidationReport report =
        new ValidationReport(
            ValidationStatus.SUCCESS,
            new FileMetadata("test.xlsx", 1024, "FILESYSTEM"),
            new FileMetadata("config.yml", 500, "FILESYSTEM"),
            new ValidationMetrics(10, 10, 0, 0),
            Collections.emptyList(),
            Collections.emptyList());
    when(excelValidationService.validate(any(), any(), anyBoolean())).thenReturn(report);

    // Perform request without 'persist' param (using constructor default if
    // applicable, or explicit null/missing in JSON if I could but I'll access the
    // constructor)
    ValidationRequest request = new ValidationRequest("test.xlsx", "config.yml", null);

    mockMvc
        .perform(
            post("/api/excel/validate")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());
  }

  @Test
  void validate_shouldReturnNotFound_whenResourceMissing() throws Exception {
    when(resourceLoaderService.loadResource(any()))
        .thenThrow(new com.example.demo.exception.ResourceNotFoundException("File not found"));

    ValidationRequest request = new ValidationRequest("missing.xlsx", "config.yml", false);

    mockMvc
        .perform(
            post("/api/excel/validate")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound());
  }

  @Test
  void validate_shouldReturnBadRequest_whenInputInvalid() throws Exception {
    when(resourceLoaderService.loadResource(any()))
        .thenThrow(new IllegalArgumentException("Invalid input"));

    ValidationRequest request = new ValidationRequest("", "config.yml", false);

    mockMvc
        .perform(
            post("/api/excel/validate")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }
}
