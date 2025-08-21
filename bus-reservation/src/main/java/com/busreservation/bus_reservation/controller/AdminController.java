package com.busreservation.bus_reservation.controller;

import com.busreservation.bus_reservation.dto.ExcelImportDto;
import com.busreservation.bus_reservation.service.ExcelImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final ExcelImportService excelImportService;

    public AdminController(ExcelImportService excelImportService) {
        this.excelImportService = excelImportService;
    }

    @PostMapping("/import")
    public ResponseEntity<ExcelImportDto.ImportResponse> importExcel(
            @RequestParam("file") MultipartFile file
    ) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(
                ExcelImportDto.ImportResponse.builder()
                    .success(false)
                    .message("File is empty")
                    .build()
            );
        }

        if (!file.getOriginalFilename().endsWith(".xlsx")) {
            return ResponseEntity.badRequest().body(
                ExcelImportDto.ImportResponse.builder()
                    .success(false)
                    .message("Only .xlsx files are supported")
                    .build()
            );
        }

        ExcelImportDto.ImportResponse response = excelImportService.importExcel(file);
        return ResponseEntity.ok(response);
    }
}