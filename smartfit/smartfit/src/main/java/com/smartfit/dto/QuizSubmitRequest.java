package com.smartfit.dto;

import java.util.List;

public class QuizSubmitRequest {
    private Long courseId;
    private List<String> answers; // răspunsurile selectate, în ordinea întrebărilor

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public List<String> getAnswers() { return answers; }
    public void setAnswers(List<String> answers) { this.answers = answers; }
}