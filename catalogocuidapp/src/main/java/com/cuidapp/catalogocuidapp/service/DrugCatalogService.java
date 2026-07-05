package com.cuidapp.catalogocuidapp.service;

import com.cuidapp.catalogocuidapp.dto.CatalogResponse;
import com.cuidapp.catalogocuidapp.dto.OcrRequest;
import com.cuidapp.catalogocuidapp.dto.OcrResponse;
import com.cuidapp.catalogocuidapp.model.DrugCatalog;
import com.cuidapp.catalogocuidapp.repository.DrugCatalogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DrugCatalogService {

    private final DrugCatalogRepository repository;
    private final OcrParserService ocrParser;
    private final RestTemplate restTemplate;

    @Value("${upcitemdb.api.url}")
    private String upcItemDbUrl;

    @Value("${wikipedia.api.url:https://es.wikipedia.org/api/rest_v1/page/summary/}")
    private String wikipediaUrl;

    // API de Gemini (Google AI Studio) para identificar el medicamento por foto.
    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.model:gemini-2.0-flash}")
    private String geminiModel;

    /**
     * Identifica un medicamento a partir de la foto de su caja usando Gemini.
     * Devuelve el resultado del catálogo (si existe) enriquecido, o los datos que
     * entregue Gemini. Si no hay API key o falla, devuelve vacío.
     */
    public Optional<CatalogResponse> identifyFromImage(String base64Image) {
        if (geminiApiKey == null || geminiApiKey.isBlank() || base64Image == null) {
            return Optional.empty();
        }
        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + geminiModel + ":generateContent?key=" + geminiApiKey;

            String prompt = "Identifica el medicamento en la foto de su caja. "
                    + "Responde SOLO un JSON con las claves: name, dosage, uso, efectos. "
                    + "name = nombre comercial o principio activo (sin la dosis). "
                    + "dosage = concentracion como '500 mg' o ''. "
                    + "uso = para que sirve, 1 o 2 frases en espanol. "
                    + "efectos = efectos secundarios frecuentes, 1 o 2 frases en espanol. "
                    + "Si no se ve un medicamento, name debe ser ''.";

            Map<String, Object> textPart = Map.of("text", prompt);
            Map<String, Object> imgPart = Map.of("inline_data",
                    Map.of("mime_type", "image/jpeg", "data", base64Image));
            Map<String, Object> body = Map.of(
                    "contents", List.of(Map.of("parts", List.of(textPart, imgPart))),
                    "generationConfig", Map.of("response_mime_type", "application/json"));

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.postForObject(
                    url, new HttpEntity<>(body, headers), Map.class);
            if (resp == null) return Optional.empty();

            // candidates[0].content.parts[0].text -> el JSON pedido
            String json = extractGeminiText(resp);
            if (json == null || json.isBlank()) return Optional.empty();

            Map<String, Object> data =
                    org.springframework.boot.json.JsonParserFactory.getJsonParser().parseMap(json);
            String name = str(data.get("name"));
            if (name.isBlank()) return Optional.empty();
            String dosage = str(data.get("dosage"));
            String uso = str(data.get("uso"));
            String efectos = str(data.get("efectos"));

            // ¿Existe en el catálogo? (por nombre completo o principio activo)
            List<DrugCatalog> local = repository.searchByName(name);
            if (local.isEmpty()) {
                String first = name.split("\\s+")[0];
                if (first.length() >= 4) local = repository.searchByName(first);
            }
            if (!local.isEmpty()) {
                DrugCatalog d = local.get(0);
                boolean changed = false;
                if ((d.getUso() == null || d.getUso().isBlank()) && !uso.isBlank()) {
                    d.setUso(uso);
                    changed = true;
                }
                if ((d.getEfectosSecundarios() == null || d.getEfectosSecundarios().isBlank())
                        && !efectos.isBlank()) {
                    d.setEfectosSecundarios(efectos);
                    changed = true;
                }
                if (changed) repository.save(d);
                return Optional.of(toResponse(d));
            }

            // No está en el catálogo: guardar lo que dio Gemini
            DrugCatalog c = DrugCatalog.builder()
                    .name(name)
                    .genericName(name.split("\\s+")[0])
                    .dosage(dosage.isBlank() ? null : dosage)
                    .uso(uso.isBlank() ? null : uso)
                    .efectosSecundarios(efectos.isBlank() ? null : efectos)
                    .source("gemini")
                    .country("CL")
                    .verified(false)
                    .build();
            return Optional.of(toResponse(repository.save(c)));
        } catch (Exception e) {
            System.err.println("Gemini identify failed: " + e.getMessage());
            return Optional.empty();
        }
    }

    private String str(Object o) {
        return (o == null) ? "" : o.toString().trim();
    }

    @SuppressWarnings("unchecked")
    private String extractGeminiText(Map<String, Object> resp) {
        try {
            var candidates = (List<Map<String, Object>>) resp.get("candidates");
            if (candidates == null || candidates.isEmpty()) return null;
            var content = (Map<String, Object>) candidates.get(0).get("content");
            var parts = (List<Map<String, Object>>) content.get("parts");
            if (parts == null || parts.isEmpty()) return null;
            return (String) parts.get(0).get("text");
        } catch (Exception e) {
            return null;
        }
    }

    public Optional<CatalogResponse> findByBarcode(String barcode) {
        // 1. Buscar en catálogo local primero (y completar uso/efectos si faltan)
        Optional<DrugCatalog> local = repository.findByBarcode(barcode);
        if (local.isPresent()) {
            return Optional.of(toResponse(enrichInfo(local.get())));
        }

        // 2. Fallback a UPCitemdb (100 req/día gratis)
        try {
            String url = upcItemDbUrl + "?upc=" + barcode;
            Map<?, ?> result = restTemplate.getForObject(url, Map.class);
            if (result != null && "OK".equals(result.get("code"))) {
                List<?> items = (List<?>) result.get("items");
                if (items != null && !items.isEmpty()) {
                    Map<?, ?> item = (Map<?, ?>) items.get(0);
                    DrugCatalog catalog = buildFromUpcItemDb(barcode, item);
                    repository.save(catalog);
                    // Completar uso/efectos desde internet con el nombre obtenido
                    return Optional.of(toResponse(enrichInfo(catalog)));
                }
            }
        } catch (Exception e) {
            // UPCitemdb no disponible o límite alcanzado — continuar sin fallar
            System.err.println("UPCitemdb lookup failed for " + barcode + ": " + e.getMessage());
        }

        return Optional.empty();
    }

    /**
     * Si al medicamento le falta la descripción de uso, la busca en Wikipedia
     * (por su principio activo o nombre) y la guarda. Devuelve el mismo objeto.
     */
    private DrugCatalog enrichInfo(DrugCatalog d) {
        if (d.getUso() != null && !d.getUso().isBlank()) return d;
        String q = (d.getGenericName() != null && !d.getGenericName().isBlank())
                ? d.getGenericName() : d.getName();
        if (q == null || q.isBlank()) return d;
        String extract = wikipediaExtract(q);
        if (extract != null) {
            d.setUso(extract);
            repository.save(d);
        }
        return d;
    }

    public List<CatalogResponse> search(String query) {
        List<DrugCatalog> local = repository.searchByName(query);
        if (!local.isEmpty()) {
            return local.stream().map(this::toResponse).toList();
        }
        // No está en el catálogo: buscar en internet (Wikipedia ES) y cachear.
        return fetchFromWikipedia(query)
                .map(d -> List.of(toResponse(d)))
                .orElseGet(List::of);
    }

    /**
     * Consulta el resumen de Wikipedia en español para el medicamento y, si lo
     * encuentra, lo guarda en el catálogo (caché) con su descripción de uso.
     */
    private Optional<DrugCatalog> fetchFromWikipedia(String query) {
        String title = query.trim().split("\\d")[0].trim();
        if (title.isEmpty()) title = query.trim();
        if (title.length() < 3) return Optional.empty();

        // Evitar volver a crear si ya existe uno guardado con ese nombre
        List<DrugCatalog> existing = repository.searchByName(title);
        if (!existing.isEmpty()) return Optional.of(existing.get(0));

        String extract = wikipediaExtract(title);
        if (extract == null) return Optional.empty();

        DrugCatalog c = DrugCatalog.builder()
                .name(title)
                .genericName(title)
                .uso(extract)
                .source("wikipedia")
                .country("CL")
                .verified(false)
                .build();
        return Optional.of(repository.save(c));
    }

    /** Trae el resumen de Wikipedia ES para un término. Devuelve null si no hay. */
    private String wikipediaExtract(String term) {
        try {
            String title = term.trim().split("\\d")[0].trim();
            if (title.length() < 3) return null;
            String url = wikipediaUrl
                    + UriUtils.encodePathSegment(title, StandardCharsets.UTF_8);
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "CuidApp/1.0 (cuidappnoreply@gmail.com)");
            ResponseEntity<Map> resp = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            Map<?, ?> body = resp.getBody();
            if (body == null) return null;
            String type = String.valueOf(body.get("type"));
            if (type.contains("not_found") || type.contains("disambiguation")) {
                return null;
            }
            String extract = (String) body.get("extract");
            return (extract == null || extract.isBlank()) ? null : extract;
        } catch (Exception e) {
            System.err.println("Wikipedia lookup failed for " + term + ": " + e.getMessage());
            return null;
        }
    }

    public List<CatalogResponse> suggest(String prefix) {
        return repository.findByNameStartingWith(prefix).stream().limit(10).map(this::toResponse).toList();
    }

    public CatalogResponse save(DrugCatalog drug) {
        if (drug.getSource() == null) drug.setSource("user");
        return toResponse(repository.save(drug));
    }

    public OcrResponse parseOcr(OcrRequest request) {
        return ocrParser.parse(request.getText());
    }

    private DrugCatalog buildFromUpcItemDb(String barcode, Map<?, ?> item) {
        String title = (String) item.get("title");
        String brand = (String) item.get("brand");

        return DrugCatalog.builder()
            .barcode(barcode)
            .name(title)
            .brand(brand)
            .source("upcitemdb")
            .verified(false)
            .build();
    }

    private CatalogResponse toResponse(DrugCatalog d) {
        return CatalogResponse.builder()
            .id(d.getId())
            .barcode(d.getBarcode())
            .name(d.getName())
            .genericName(d.getGenericName())
            .brand(d.getBrand())
            .dosage(d.getDosage())
            .form(d.getForm())
            .manufacturer(d.getManufacturer())
            .uso(d.getUso())
            .efectosSecundarios(d.getEfectosSecundarios())
            .source(d.getSource())
            .verified(d.getVerified())
            .build();
    }
}
