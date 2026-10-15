package com.smartfit.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.smartfit.entity.User;
import com.smartfit.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${groq.api.key:MISSING_KEY}")
    private String groqApiKey;

    public ChatController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // ── DTOs ──────────────────────────────────────────────────────────────────
    record MessageDto(String role, String content) {}
    record ChatRequest(List<MessageDto> messages) {}
    record ChatResponse(String reply) {}

    // ── ENDPOINT ──────────────────────────────────────────────────────────────
    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody ChatRequest request) {
        try {
            User user = getAuthenticatedUser();
            String systemPrompt = buildSystemPrompt(user);

            // Construieste payload-ul JSON pentru Groq
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("model", "llama-3.3-70b-versatile");
            payload.put("max_tokens", 500);
            payload.put("temperature", 0.7);

            ArrayNode messages = objectMapper.createArrayNode();

            // 1. System prompt cu profilul userului
            ObjectNode sysMsg = objectMapper.createObjectNode();
            sysMsg.put("role", "system");
            sysMsg.put("content", systemPrompt);
            messages.add(sysMsg);

            // 2. Istoricul conversatiei (max 20 mesaje = 10 schimburi)
            List<MessageDto> history = request.messages();
            if (history.size() > 20) {
                history = history.subList(history.size() - 20, history.size());
            }
            for (MessageDto msg : history) {
                ObjectNode msgNode = objectMapper.createObjectNode();
                msgNode.put("role", msg.role());
                msgNode.put("content", msg.content());
                messages.add(msgNode);
            }
            payload.set("messages", messages);

            // Apel HTTP catre Groq
            String jsonBody = objectMapper.writeValueAsString(payload);
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                    .header("Authorization", "Bearer " + groqApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(httpRequest,
                    HttpResponse.BodyHandlers.ofString());

            JsonNode groqResponse = objectMapper.readTree(response.body());

            // Gestioneaza erori API
            if (groqResponse.has("error")) {
                String errMsg = groqResponse.get("error").get("message").asText();
                return ResponseEntity.ok(new ChatResponse("⚠️ Eroare API: " + errMsg));
            }

            // Extrage raspunsul
            String reply = groqResponse
                    .get("choices").get(0)
                    .get("message").get("content").asText();

            return ResponseEntity.ok(new ChatResponse(reply));

        } catch (Exception e) {
            return ResponseEntity.ok(new ChatResponse(
                    "Îmi pare rău, a apărut o eroare tehnică. Te rog încearcă din nou. 🙏"));
        }
    }

    // ── SYSTEM PROMPT ─────────────────────────────────────────────────────────
    private String buildSystemPrompt(User user) {
        // Calculeaza varsta
        int age = 22;
        if (user.getBirthDate() != null)
            age = Period.between(user.getBirthDate(), LocalDate.now()).getYears();

        // Date fizice cu valori default sigure
        double weight = user.getWeightKg()      != null ? user.getWeightKg()      : 70.0;
        double height = user.getHeightCm()      != null ? user.getHeightCm()      : 170.0;
        String gender = user.getGender()        != null ? user.getGender()        : "M";
        String goal   = user.getGoal()          != null ? user.getGoal()          : "MAINTAIN";
        double actLvl = user.getActivityLevel() != null ? user.getActivityLevel() : 1.375;
        int fitPoints = user.getFitPoints()     != null ? user.getFitPoints()     : 0;

        // Calcule nutritionale (Mifflin-St Jeor)
        double sexOffset = "F".equals(gender) ? -161 : 5;
        long bmr  = Math.round(10 * weight + 6.25 * height - 5.0 * age + sexOffset);
        long tdee = Math.round(bmr * actLvl);

        String goalLabel = switch (goal) {
            case "LOSE_WEIGHT" -> "Slăbire (deficit -500 kcal/zi)";
            case "GAIN_MUSCLE" -> "Masă musculară (surplus +300 kcal/zi)";
            default            -> "Menținere (egal cu TDEE)";
        };
        long caloricTarget = switch (goal) {
            case "LOSE_WEIGHT" -> tdee - 500;
            case "GAIN_MUSCLE" -> tdee + 300;
            default            -> tdee;
        };

        return "Ești SmartFit AI, asistentul personal de fitness și nutriție al platformei SmartFit.\n"
                + "Răspunzi EXCLUSIV în limba română, cu un ton prietenos, motivant și profesionist.\n"
                + "Ești concis: maxim 4-5 propoziții sau bullet points scurte per răspuns.\n"
                + "Folosești emoji-uri relevante cu moderație pentru a face răspunsurile mai prietenoase.\n\n"

                + "=== PROFILUL UTILIZATORULUI ===\n"
                + "Nume: " + user.getFirstName() + " " + user.getLastName() + "\n"
                + "Gen: " + ("F".equals(gender) ? "Femeie" : "Bărbat") + " | Vârstă: " + age + " ani\n"
                + "Greutate: " + weight + " kg | Înălțime: " + (int) height + " cm\n"
                + "Obiectiv: " + goalLabel + "\n"
                + "BMR: " + bmr + " kcal/zi | TDEE: " + tdee + " kcal/zi\n"
                + "Țintă calorică zilnică: " + caloricTarget + " kcal\n"
                + "Proteină țintă: " + Math.round(weight * 2.0) + " g/zi\n"
                + "Carbohidrați țintă: " + Math.round(caloricTarget * 0.45 / 4.0) + " g/zi\n"
                + "Grăsimi țintă: " + Math.round(caloricTarget * 0.25 / 9.0) + " g/zi\n"
                + "Puncte XP acumulate: " + fitPoints + "\n\n"

                + "=== PRODUSE DISPONIBILE ÎN MAGAZIN SMARTFIT ===\n"
                + "SUPLIMENTE: Whey Protein Gold 2kg (149 lei), Creatine Monohydrate 500g (79 lei), "
                + "Omega-3 Complex 120 capsule (79 lei), Vitamina D3+K2 90 capsule (55 lei), BCAA 2:1:1 300g (89 lei).\n"
                + "ALIMENTE: Mix Nuts Premium 500g (64 lei), Batoane Proteice x12 (89 lei), "
                + "Ovăz Integral 1kg (22 lei), Unt de Arahide Natural 500g (39 lei), Granola Proteică 400g (45 lei).\n"
                + "VESTIMENTAȚIE: SmartFit Training Tee (89 lei), Pantaloni Jogger Pro (149 lei), "
                + "Șosete Compression x3 perechi (49 lei).\n"
                + "ECHIPAMENT: Rucsac Sport 25L (179 lei), Saltea Yoga Premium 6mm (129 lei).\n\n"

                + "=== CURSURI DISPONIBILE ÎN ACADEMIE ===\n"
                + "Nutriție fundamentală (+120 XP, Începător), Proteine & Recuperare (+200 XP, Mediu), "
                + "Suplimentare avansată (+350 XP, Avansat), Slăbire sănătoasă (+150 XP, Începător), "
                + "Alimentație pentru sport (+420 XP, Avansat), Hidratare & electroliți (+80 XP, Începător).\n\n"

                + "=== REGULI OBLIGATORII ===\n"
                + "- Recomandă SPECIFIC produse și cursuri din platforma SmartFit când e relevant\n"
                + "- Adaptează totul la profilul și obiectivul utilizatorului de mai sus\n"
                + "- Nu inventa informații medicale false sau periculoase\n"
                + "- Pentru întrebări medicale serioase, recomandă consultarea unui medic\n"
                + "- Rămâi pe subiectele: fitness, nutriție, suplimente, exerciții, cursuri, produse SmartFit\n"
                + "- Dacă întrebarea este complet în afara domeniului, redirecționează elegant";
    }

    // ── UTIL ──────────────────────────────────────────────────────────────────
    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(auth.getName());
    }
}