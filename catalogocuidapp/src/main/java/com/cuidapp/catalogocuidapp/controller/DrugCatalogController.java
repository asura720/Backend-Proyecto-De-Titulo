package com.cuidapp.catalogocuidapp.controller;

import com.cuidapp.catalogocuidapp.dto.CatalogResponse;
import com.cuidapp.catalogocuidapp.dto.OcrRequest;
import com.cuidapp.catalogocuidapp.dto.OcrResponse;
import com.cuidapp.catalogocuidapp.model.DrugCatalog;
import com.cuidapp.catalogocuidapp.service.DrugCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DrugCatalogController {

    private final DrugCatalogService service;

    // GET /api/catalog/barcode/7891234567890
    @GetMapping("/barcode/{barcode}")
    public ResponseEntity<CatalogResponse> findByBarcode(@PathVariable String barcode) {
        return service.findByBarcode(barcode)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    // GET /api/catalog/search?q=paracetamol
    @GetMapping("/search")
    public ResponseEntity<List<CatalogResponse>> search(@RequestParam String q) {
        return ResponseEntity.ok(service.search(q));
    }

    // POST /api/catalog/identify  — identifica el medicamento por foto (Gemini)
    // Body: { "imageBase64": "..." }
    @PostMapping("/identify")
    public ResponseEntity<CatalogResponse> identify(@RequestBody java.util.Map<String, String> body) {
        String image = body.get("imageBase64");
        return service.identifyFromImage(image)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // GET /api/catalog/suggest?prefix=para
    @GetMapping("/suggest")
    public ResponseEntity<List<CatalogResponse>> suggest(@RequestParam String prefix) {
        return ResponseEntity.ok(service.suggest(prefix));
    }

    // POST /api/catalog/barcode  — crowd-sourcing: el usuario aporta un mapeo nuevo
    @PostMapping("/barcode")
    public ResponseEntity<CatalogResponse> create(@RequestBody DrugCatalog drug) {
        return new ResponseEntity<>(service.save(drug), HttpStatus.CREATED);
    }

    // POST /api/catalog/ocr  — recibe texto OCR crudo, retorna campos parseados
    @PostMapping("/ocr")
    public ResponseEntity<OcrResponse> parseOcr(@RequestBody OcrRequest request) {
        return ResponseEntity.ok(service.parseOcr(request));
    }
}
