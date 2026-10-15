package com.smartfit.controller;

import com.smartfit.dto.QuizSubmitRequest;
import com.smartfit.entity.*;
import com.smartfit.repository.*;
import com.smartfit.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class AcademieController {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final CourseCompletionRepository completionRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final UserService userService;

    public AcademieController(UserRepository userRepository,
                              CourseRepository courseRepository,
                              CourseEnrollmentRepository enrollmentRepository,
                              CourseCompletionRepository completionRepository,
                              QuizQuestionRepository quizQuestionRepository,
                              UserService userService) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.completionRepository = completionRepository;
        this.quizQuestionRepository = quizQuestionRepository;
        this.userService = userService;
    }

    // ─────────────────────────────────────────────────────────────
    // Pagina principală Academie
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/academie")
    public String academie(Model model) {
        boolean auth = isLoggedIn();
        model.addAttribute("isAuthenticated", auth);

        List<Course> courses = courseRepository.findAllByOrderByIdAsc();
        model.addAttribute("courses", courses);

        if (auth) {
            User user = getUser();
            model.addAttribute("user", user);
            model.addAttribute("userInitials", initials(user));

            // Înscrieri
            List<CourseEnrollment> enrollments = enrollmentRepository.findByUserId(user.getId());
            Map<Long, CourseEnrollment> enrollmentMap = enrollments.stream()
                    .collect(Collectors.toMap(e -> e.getCourse().getId(), e -> e));
            model.addAttribute("enrollmentMap", enrollmentMap);

            // Finalizări
            List<CourseCompletion> completions = completionRepository.findByUserId(user.getId());
            Set<Long> completedIds = completions.stream()
                    .map(c -> c.getCourse().getId())
                    .collect(Collectors.toSet());
            model.addAttribute("completedIds", completedIds);
            model.addAttribute("completedCoursesCount", completedIds.size());
            model.addAttribute("certificates", completions);

            // XP & Level
            int fp = user.getFitPoints() != null ? user.getFitPoints() : 0;
            String xpLevel;
            String nextLevel;
            int xpThisTier, xpNextTier;

            if (fp < 500) {
                xpLevel = "Bronze";
                nextLevel = "Silver";
                xpThisTier = 0;
                xpNextTier = 500;
            } else if (fp < 1500) {
                xpLevel = "Silver";
                nextLevel = "Gold";
                xpThisTier = 500;
                xpNextTier = 1500;
            } else if (fp < 3000) {
                xpLevel = "Gold";
                nextLevel = "Platinum";
                xpThisTier = 1500;
                xpNextTier = 3000;
            } else {
                xpLevel = "Platinum";
                nextLevel = "Max";
                xpThisTier = 3000;
                xpNextTier = 5000;
            }

            int xpPct = (int) Math.min(100, ((fp - xpThisTier) * 100.0) / (xpNextTier - xpThisTier));
            int discount = Math.min(25, 5 + fp / 100);

            model.addAttribute("userXP", fp);
            model.addAttribute("xpLevel", xpLevel);
            model.addAttribute("nextLevel", nextLevel);
            model.addAttribute("xpNextTier", xpNextTier);
            model.addAttribute("xpProgressPct", xpPct);
            model.addAttribute("activeDiscount", discount);

            // Leaderboard – top 10
            List<User> top10 = userRepository.findTop10ByOrderByFitPointsDesc();
            model.addAttribute("leaderboard", top10);
        } else {
            model.addAttribute("user", null);
            model.addAttribute("userInitials", "?");
            model.addAttribute("enrollmentMap", Collections.emptyMap());
            model.addAttribute("completedIds", Collections.emptySet());
            model.addAttribute("completedCoursesCount", 0);
            model.addAttribute("certificates", Collections.emptyList());
            model.addAttribute("userXP", 0);
            model.addAttribute("xpLevel", "Bronze");
            model.addAttribute("nextLevel", "Silver");
            model.addAttribute("xpNextTier", 500);
            model.addAttribute("xpProgressPct", 0);
            model.addAttribute("activeDiscount", 0);
            model.addAttribute("leaderboard", Collections.emptyList());
        }

        return "academie";
    }

    // ─────────────────────────────────────────────────────────────
    // Pagina vizualizare curs individual
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/academie/course/{id}")
    public String courseView(@PathVariable Long id, Model model) {
        if (!isLoggedIn()) return "redirect:/login";

        User user = getUser();
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Curs negăsit: " + id));

        // Auto-înscrie dacă nu e deja înscris
        CourseEnrollment enrollment = enrollmentRepository
                .findByUserAndCourse(user.getId(), id)
                .orElseGet(() -> {
                    CourseEnrollment e = new CourseEnrollment();
                    e.setUser(user);
                    e.setCourse(course);
                    e.setProgressPct(5);
                    return enrollmentRepository.save(e);
                });

        boolean completed = completionRepository.findByUserAndCourse(user.getId(), id).isPresent();

        // Întrebări cu opțiuni amestecate server-side
        List<QuizQuestion> rawQ = quizQuestionRepository.findByCourseIdOrdered(id);

        List<Map<String, Object>> questionsData = rawQ.stream().map(q -> {
            List<String> opts = new ArrayList<>(Arrays.asList(
                    q.getCorrectAnswer(),
                    q.getWrongAnswer1(),
                    q.getWrongAnswer2(),
                    q.getWrongAnswer3()
            ));

            Collections.shuffle(opts);

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", q.getId());
            m.put("text", q.getQuestionText());
            m.put("options", opts);

            // NU expunem correctAnswer în HTML
            return m;

        }).collect(Collectors.toList());

        model.addAttribute("course", course);
        model.addAttribute("questionsData", questionsData);
        model.addAttribute("enrollment", enrollment);
        model.addAttribute("completed", completed);
        model.addAttribute("user", user);
        model.addAttribute("userInitials", initials(user));

        // Serializare JSON sigură pentru JS
        ObjectMapper mapper = new ObjectMapper();
        String questionsJson;

        try {
            questionsJson = mapper.writeValueAsString(questionsData);
        } catch (Exception e) {
            questionsJson = "[]";
        }

        model.addAttribute("questionsJson", questionsJson);

        return "course-view";
    }

    // ─────────────────────────────────────────────────────────────
    // API: Înscrie utilizatorul la un curs
    // ─────────────────────────────────────────────────────────────
    @PostMapping("/academie/enroll/{courseId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> enroll(@PathVariable Long courseId) {
        if (!isLoggedIn()) return ResponseEntity.status(401).body(Map.of("error", "Neautentificat"));

        User user = getUser();
        Course course = courseRepository.findById(courseId)
                .orElse(null);
        if (course == null) return ResponseEntity.notFound().build();

        if (!enrollmentRepository.existsByUserIdAndCourseId(user.getId(), courseId)) {
            CourseEnrollment e = new CourseEnrollment();
            e.setUser(user);
            e.setCourse(course);
            e.setProgressPct(0);
            enrollmentRepository.save(e);
        }
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ─────────────────────────────────────────────────────────────
    // API: Trimitere răspunsuri quiz
    // ─────────────────────────────────────────────────────────────
    @PostMapping("/academie/quiz/submit")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> submitQuiz(@RequestBody QuizSubmitRequest req) {
        if (!isLoggedIn()) return ResponseEntity.status(401).body(Map.of("error", "Neautentificat"));

        User user = getUser();
        Long courseId = req.getCourseId();

        // Deja finalizat?
        if (completionRepository.findByUserAndCourse(user.getId(), courseId).isPresent()) {
            return ResponseEntity.ok(Map.of(
                    "alreadyCompleted", true,
                    "message", "Ai finalizat deja acest curs!"));
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Curs negăsit"));
        List<QuizQuestion> questions = quizQuestionRepository.findByCourseIdOrdered(courseId);
        List<String> answers = req.getAnswers() != null ? req.getAnswers() : Collections.emptyList();

        // Calculează scorul
        int correct = 0;
        for (int i = 0; i < Math.min(questions.size(), answers.size()); i++) {
            if (questions.get(i).getCorrectAnswer().equals(answers.get(i))) correct++;
        }
        int scorePct = questions.isEmpty() ? 0 : (correct * 100) / questions.size();
        boolean passed = scorePct >= course.getPassScorePercentage();
        int xpEarned = 0;

        if (passed) {
            // Salvează finalizarea
            CourseCompletion completion = new CourseCompletion();
            completion.setUser(user);
            completion.setCourse(course);
            completion.setScoreObtained(scorePct);
            completion.setCompletedAt(LocalDateTime.now());
            completionRepository.save(completion);

            // Acordă XP
            xpEarned = course.getRewardPoints();
            user.setFitPoints((user.getFitPoints() != null ? user.getFitPoints() : 0) + xpEarned);
            userService.saveUser(user);

            // Progres 100%
            enrollmentRepository.findByUserAndCourse(user.getId(), courseId).ifPresent(e -> {
                e.setProgressPct(100);
                enrollmentRepository.save(e);
            });
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "passed", passed,
                "score", scorePct,
                "correct", correct,
                "total", questions.size(),
                "xpEarned", xpEarned,
                "passScore", course.getPassScorePercentage()
        ));
    }

    // ─────────────────────────────────────────────────────────────
    // Utilitare private
    // ─────────────────────────────────────────────────────────────
    private boolean isLoggedIn() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName());
    }

    private User getUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(auth.getName());
    }

    private String initials(User user) {
        if (user == null || user.getFirstName() == null || user.getFirstName().isEmpty()) return "?";
        return String.valueOf(user.getFirstName().charAt(0)).toUpperCase();
    }
}