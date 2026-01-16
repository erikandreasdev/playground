package com.example.working_with_excels.excel.infrastructure.adapter.input;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.working_with_excels.excel.application.dto.ImportReport;
import com.example.working_with_excels.excel.application.port.input.ExcelImportUseCase;
import com.example.working_with_excels.excel.domain.model.FileSource;
import com.example.working_with_excels.excel.domain.model.ImportMode;

import lombok.RequiredArgsConstructor;

/**
 * REST controller for Excel import operations.
 *
 * <p>
 * Supports multiple file sources: classpath, filesystem, multipart upload, URL
 * download.
 */
@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class ExcelImportController {

        private static final String DEFAULT_EXCEL = "import_data.xlsx";
        private static final String DEFAULT_CONFIG = "import_mapping.yml";

        private final ExcelImportUseCase importUseCase;

        /**
         * Quick test - DRY_RUN mode with default files.
         *
         * <p>
         * GET /api/import/dry-run
         */
        @GetMapping("/dry-run")
        public ResponseEntity<ImportReport> dryRun() throws IOException {
                ImportReport report = importUseCase.importExcel(DEFAULT_EXCEL, DEFAULT_CONFIG, ImportMode.DRY_RUN);
                return ResponseEntity.ok(report);
        }

        /**
         * Execute import with default files.
         *
         * <p>
         * POST /api/import/execute
         */
        @PostMapping("/execute")
        public ResponseEntity<ImportReport> execute() throws IOException {
                ImportReport report = importUseCase.importExcel(DEFAULT_EXCEL, DEFAULT_CONFIG, ImportMode.EXECUTE);
                return ResponseEntity.ok(report);
        }

        /**
         * Import from classpath resources.
         *
         * <p>
         * POST /api/import/classpath?excel=file.xlsx&config=mapping.yml&mode=DRY_RUN
         */
        @PostMapping("/classpath")
        public ResponseEntity<ImportReport> classpathImport(
                        @RequestParam(value = "excel", defaultValue = DEFAULT_EXCEL) String excelFile,
                        @RequestParam(value = "config", defaultValue = DEFAULT_CONFIG) String configFile,
                        @RequestParam(value = "mode", defaultValue = "DRY_RUN") ImportMode mode) throws IOException {

                ImportReport report = importUseCase.importExcel(
                                FileSource.classpath(excelFile),
                                FileSource.classpath(configFile),
                                mode);
                return ResponseEntity.ok(report);
        }

        /**
         * Import from filesystem paths.
         *
         * <p>
         * POST
         * /api/import/filesystem?excelPath=/path/to/file.xlsx&configPath=/path/to/mapping.yml
         */
        @PostMapping("/filesystem")
        public ResponseEntity<ImportReport> filesystemImport(
                        @RequestParam("excelPath") String excelPath,
                        @RequestParam("configPath") String configPath,
                        @RequestParam(value = "mode", defaultValue = "DRY_RUN") ImportMode mode) throws IOException {

                ImportReport report = importUseCase.importExcel(
                                FileSource.filesystem(Path.of(excelPath)),
                                FileSource.filesystem(Path.of(configPath)),
                                mode);
                return ResponseEntity.ok(report);
        }

        /**
         * Import via file upload.
         *
         * <p>
         * POST /api/import/upload (multipart/form-data)
         */
        @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<ImportReport> uploadImport(
                        @RequestParam("excel") MultipartFile excelFile,
                        @RequestParam("config") MultipartFile configFile,
                        @RequestParam(value = "mode", defaultValue = "DRY_RUN") ImportMode mode) throws IOException {

                ImportReport report = importUseCase.importExcel(
                                FileSource.stream(excelFile.getInputStream(), excelFile.getOriginalFilename()),
                                FileSource.stream(configFile.getInputStream(), configFile.getOriginalFilename()),
                                mode);
                return ResponseEntity.ok(report);
        }

        /**
         * Import from URLs (cloud storage).
         *
         * <p>
         * POST /api/import/url?excelUrl=https://...&configUrl=https://...
         */
        @PostMapping("/url")
        public ResponseEntity<ImportReport> urlImport(
                        @RequestParam("excelUrl") String excelUrl,
                        @RequestParam("configUrl") String configUrl,
                        @RequestParam(value = "mode", defaultValue = "DRY_RUN") ImportMode mode) throws IOException {

                ImportReport report = importUseCase.importExcel(
                                FileSource.url(URI.create(excelUrl)),
                                FileSource.url(URI.create(configUrl)),
                                mode);
                return ResponseEntity.ok(report);
        }
}
