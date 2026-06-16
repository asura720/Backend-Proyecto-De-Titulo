package com.cuidapp.catalogocuidapp.service;

import com.cuidapp.catalogocuidapp.dto.OcrResponse;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OcrParserService {

    private static final Pattern DOSAGE_PATTERN =
        Pattern.compile("(\\d+(?:[.,]\\d+)?\\s*(?:mg|ml|mcg|g|UI|ug|µg|%))", Pattern.CASE_INSENSITIVE);

    private static final Pattern FORM_PATTERN =
        Pattern.compile("\\b(comprimido[s]?|cápsula[s]?|capsula[s]?|jarabe|solución|solucion|" +
                        "crema|gel|pomada|inyectable|ampolla[s]?|tableta[s]?|gragea[s]?|" +
                        "supositorio[s]?|colirio|suspensión|suspension|parche[s]?|" +
                        "spray|aerosol|inhalador|gotas|polvo|granulado|sobre[s]?|" +
                        "ungüento|unguento|loción|locion|emulsión|emulsion|" +
                        "liberación prolongada|liberacion prolongada|retard|LP|MR)\\b",
                        Pattern.CASE_INSENSITIVE);

    // Medicamentos comunes en Chile — fuente: CENABAST / Formulario Nacional ISP
    private static final String[] KNOWN_GENERICS = {
        // Analgésicos / Antiinflamatorios
        "Paracetamol", "Ibuprofeno", "Diclofenaco", "Naproxeno", "Ketoprofeno",
        "Meloxicam", "Celecoxib", "Tramadol", "Codeína", "Morfina", "Ketorolaco",
        "Ácido acetilsalicílico", "Aspirina", "Metamizol", "Dipirona",

        // Antibióticos
        "Amoxicilina", "Amoxicilina-clavulánico", "Azitromicina", "Claritromicina",
        "Ciprofloxacino", "Levofloxacino", "Metronidazol", "Cotrimoxazol",
        "Ampicilina", "Cefalexina", "Ceftriaxona", "Doxiciclina", "Eritromicina",
        "Nitrofurantoína", "Clindamicina", "Vancomicina", "Gentamicina",

        // Cardiovascular
        "Enalapril", "Losartán", "Losartan", "Valsartán", "Irbesartán", "Olmesartán",
        "Amlodipino", "Nifedipino", "Diltiazem", "Verapamilo",
        "Atenolol", "Metoprolol", "Carvedilol", "Bisoprolol", "Propranolol",
        "Hidroclorotiazida", "Furosemida", "Espironolactona", "Torasemida",
        "Simvastatina", "Atorvastatina", "Rosuvastatina", "Pravastatina",
        "Aspirina", "Clopidogrel", "Warfarina", "Acenocumarol",
        "Digoxina", "Amiodarona", "Nitroglicerina", "Isosorbide",

        // Diabetes
        "Metformina", "Glibenclamida", "Glipizida", "Gliclazida",
        "Sitagliptina", "Empagliflozina", "Dapagliflozina",
        "Insulina", "Insulina glargina", "Insulina lispro", "Insulina aspártica",

        // Sistema nervioso
        "Alprazolam", "Clonazepam", "Diazepam", "Lorazepam", "Bromazepam",
        "Sertralina", "Fluoxetina", "Paroxetina", "Escitalopram", "Citalopram",
        "Amitriptilina", "Imipramina", "Venlafaxina", "Duloxetina", "Mirtazapina",
        "Haloperidol", "Risperidona", "Quetiapina", "Olanzapina", "Clozapina",
        "Carbamazepina", "Valproato", "Ácido valproico", "Lamotrigina", "Fenitoína",
        "Levodopa", "Carbidopa", "Donepezilo", "Memantina",
        "Zolpidem", "Zopiclona",

        // Gastroenterología
        "Omeprazol", "Pantoprazol", "Lansoprazol", "Esomeprazol", "Ranitidina",
        "Metoclopramida", "Domperidona", "Ondansetrón", "Loperamida",
        "Mesalazina", "Sulfasalazina", "Lactuosa", "Bisacodilo",

        // Respiratorio
        "Salbutamol", "Ipratropio", "Tiotropio", "Salmeterol", "Formoterol",
        "Fluticasona", "Budesonida", "Beclometasona",
        "Montelukast", "Loratadina", "Cetirizina", "Fexofenadina", "Desloratadina",
        "Bromhexina", "Ambroxol", "Acetilcisteína", "Dextrometorfano",

        // Endocrinología / Hormonas
        "Levotiroxina", "Metimazol", "Propiltiouracilo",
        "Prednisona", "Dexametasona", "Hidrocortisona", "Betametasona",
        "Estradiol", "Progesterona", "Testosterona", "Anticonceptivo",

        // Antimicóticos / Antivirales
        "Fluconazol", "Ketoconazol", "Itraconazol", "Clotrimazol", "Miconazol",
        "Aciclovir", "Valaciclovir", "Oseltamivir",

        // Vitaminas / Minerales (frecuentes en recetas)
        "Ácido fólico", "Hierro", "Ferroso", "Calcio", "Vitamina D",
        "Vitamina B12", "Zinc", "Magnesio",

        // Oftalmología / Dermatología
        "Timolol", "Latanoprost", "Brimonidina",
        "Tretinoína", "Benzoilperóxido", "Permetrina",

        // Urología / Nefrología
        "Tamsulosina", "Finasterida", "Sildenafil", "Tadalafil",
        "Alopurinol", "Colchicina", "Probenecid"
    };

    public OcrResponse parse(String rawText) {
        String name = extractName(rawText);
        String dosage = extractDosage(rawText);
        String form = extractForm(rawText);

        return OcrResponse.builder()
            .name(name)
            .dosage(dosage)
            .form(form)
            .nameConfidence(name != null ? 0.8 : 0.0)
            .dosageConfidence(dosage != null ? 0.9 : 0.0)
            .formConfidence(form != null ? 0.85 : 0.0)
            .rawText(rawText)
            .build();
    }

    private String extractName(String text) {
        for (String generic : KNOWN_GENERICS) {
            if (text.toLowerCase().contains(generic.toLowerCase())) {
                return generic;
            }
        }
        // Fallback: primera línea con mayúscula
        String[] lines = text.split("[\n\r]+");
        for (String line : lines) {
            line = line.trim();
            if (line.length() > 3 && Character.isUpperCase(line.charAt(0)) && line.matches(".*[a-zA-Z].*")) {
                return line.split("\\s+")[0];
            }
        }
        return null;
    }

    private String extractDosage(String text) {
        Matcher m = DOSAGE_PATTERN.matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    private String extractForm(String text) {
        Matcher m = FORM_PATTERN.matcher(text);
        if (m.find()) {
            String found = m.group(1).toLowerCase();
            return Character.toUpperCase(found.charAt(0)) + found.substring(1);
        }
        return null;
    }
}
