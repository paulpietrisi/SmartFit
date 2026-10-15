package com.smartfit.service;

import com.smartfit.dto.CourseFormDto;
import com.smartfit.entity.Course;
import com.smartfit.entity.QuizQuestion;
import com.smartfit.entity.User;
import com.smartfit.repository.CourseRepository;
import com.smartfit.repository.QuizQuestionRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Logică CRUD pentru cursuri (Academie) + întrebări quiz asociate.
 * Folosită de AdminController și EmployeeAcademyController.
 */
@Service
public class CourseAdminService {

    private final CourseRepository courseRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final AuditLogService auditLogService;

    public CourseAdminService(CourseRepository courseRepository,
                               QuizQuestionRepository quizQuestionRepository,
                               AuditLogService auditLogService) {
        this.courseRepository = courseRepository;
        this.quizQuestionRepository = quizQuestionRepository;
        this.auditLogService = auditLogService;
    }

    // ── LISTARE / CĂUTARE / FILTRARE ──────────────────────────
    public List<Course> search(String q, String difficulty, Integer minPoints, Integer maxPoints) {
        return courseRepository.search(blank(q), blank(difficulty), minPoints, maxPoints);
    }

    public List<Course> findAll() {
        return courseRepository.findAll();
    }

    public List<String> findAllDifficulties() {
        return courseRepository.findAllDifficulties();
    }

    public Course findById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cursul cu ID " + id + " nu a fost găsit"));
    }

    public CourseFormDto toFormDto(Course c) {
        CourseFormDto dto = new CourseFormDto();
        dto.setId(c.getId());
        dto.setTitle(c.getTitle());
        dto.setDescription(c.getDescription());
        dto.setContent(c.getContent());
        dto.setVideoUrl(c.getVideoUrl());
        dto.setRewardPoints(c.getRewardPoints());
        dto.setPassScorePercentage(c.getPassScorePercentage());
        dto.setDifficulty(c.getDifficulty());
        dto.setEmoji(c.getEmoji());
        dto.setBgColor(c.getBgColor());

        List<CourseFormDto.QuizQuestionFormDto> questions = quizQuestionRepository.findByCourseId(c.getId())
                .stream().map(q -> {
                    CourseFormDto.QuizQuestionFormDto qd = new CourseFormDto.QuizQuestionFormDto();
                    qd.setId(q.getId());
                    qd.setQuestionText(q.getQuestionText());
                    qd.setCorrectAnswer(q.getCorrectAnswer());
                    qd.setWrongAnswer1(q.getWrongAnswer1());
                    qd.setWrongAnswer2(q.getWrongAnswer2());
                    qd.setWrongAnswer3(q.getWrongAnswer3());
                    return qd;
                }).toList();
        dto.setQuestions(questions);

        return dto;
    }

    // ── ADAUGARE ───────────────────────────────────────────────
    @Transactional
    public Course create(CourseFormDto dto, User actor, HttpServletRequest request) {
        Course c = new Course();
        applyDto(c, dto);
        Course saved = courseRepository.save(c);

        saveQuestions(saved, dto);

        auditLogService.log(actor, "CREATE",
                "Curs adăugat: " + saved.getTitle() + " (" + saved.getDifficulty() + ", "
                        + (dto.getQuestions() != null ? dto.getQuestions().size() : 0) + " întrebări)",
                "courses", saved.getId(),
                "INSERT INTO courses (title, ...) VALUES ('" + escape(saved.getTitle()) + "', ...)",
                request);

        return saved;
    }

    // ── MODIFICARE ─────────────────────────────────────────────
    @Transactional
    public Course update(Long id, CourseFormDto dto, User actor, HttpServletRequest request) {
        Course c = findById(id);
        applyDto(c, dto);
        Course saved = courseRepository.save(c);

        // Resetăm și re-salvăm întrebările (simplu și predictibil pentru un proiect mic)
        quizQuestionRepository.deleteByCourseId(saved.getId());
        saveQuestions(saved, dto);

        auditLogService.log(actor, "UPDATE",
                "Curs modificat: " + saved.getTitle() + " (dificultate=" + saved.getDifficulty()
                        + ", recompensă=" + saved.getRewardPoints() + " pct)",
                "courses", saved.getId(),
                "UPDATE courses SET title='" + escape(saved.getTitle()) + "', difficulty='"
                        + escape(saved.getDifficulty()) + "' WHERE id=" + saved.getId(),
                request);

        return saved;
    }

    // ── ȘTERGERE ───────────────────────────────────────────────
    @Transactional
    public void delete(Long id, User actor, HttpServletRequest request) {
        Course c = findById(id);
        String title = c.getTitle();

        quizQuestionRepository.deleteByCourseId(id);
        courseRepository.deleteById(id);

        auditLogService.log(actor, "DELETE",
                "Curs șters: " + title,
                "courses", id,
                "DELETE FROM courses WHERE id = " + id, request);
    }

    // ── helpers ─────────────────────────────────────────────────
    private void saveQuestions(Course course, CourseFormDto dto) {
        if (dto.getQuestions() == null) return;
        for (CourseFormDto.QuizQuestionFormDto qd : dto.getQuestions()) {
            if (qd.getQuestionText() == null || qd.getQuestionText().isBlank()) continue;
            QuizQuestion q = new QuizQuestion();
            q.setCourse(course);
            q.setQuestionText(qd.getQuestionText().trim());
            q.setCorrectAnswer(qd.getCorrectAnswer().trim());
            q.setWrongAnswer1(qd.getWrongAnswer1().trim());
            q.setWrongAnswer2(qd.getWrongAnswer2().trim());
            q.setWrongAnswer3(qd.getWrongAnswer3().trim());
            quizQuestionRepository.save(q);
        }
    }

    private void applyDto(Course c, CourseFormDto dto) {
        c.setTitle(dto.getTitle().trim());
        c.setDescription(dto.getDescription());
        c.setContent(dto.getContent());
        c.setVideoUrl(dto.getVideoUrl());
        c.setRewardPoints(dto.getRewardPoints());
        c.setPassScorePercentage(dto.getPassScorePercentage());
        c.setDifficulty(dto.getDifficulty().trim());
        c.setEmoji(dto.getEmoji() != null && !dto.getEmoji().isBlank() ? dto.getEmoji() : "📚");
        c.setBgColor(dto.getBgColor() != null && !dto.getBgColor().isBlank() ? dto.getBgColor() : "#EAF3FF");
    }

    private String blank(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("'", "\\'");
    }
}
