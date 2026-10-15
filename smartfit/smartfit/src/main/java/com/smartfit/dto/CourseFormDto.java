package com.smartfit.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO folosit la creare/editare curs (Academie) din panoul admin/angajat academie.
 * Conține și lista de întrebări quiz asociate.
 */
public class CourseFormDto {

    private Long id;

    @NotBlank(message = "Titlul cursului este obligatoriu")
    @Size(max = 150, message = "Titlul poate avea maxim 150 caractere")
    private String title;

    @Size(max = 2000, message = "Descrierea poate avea maxim 2000 caractere")
    private String description;

    @Size(max = 100000, message = "Conținutul este prea lung")
    private String content;

    private String videoUrl;

    @NotNull(message = "Punctele de recompensă sunt obligatorii")
    @Min(value = 0, message = "Punctele nu pot fi negative")
    @Max(value = 5000, message = "Maxim 5000 puncte")
    private Integer rewardPoints;

    @NotNull(message = "Procentul minim de promovare este obligatoriu")
    @Min(value = 1, message = "Minim 1%")
    @Max(value = 100, message = "Maxim 100%")
    private Integer passScorePercentage;

    @NotBlank(message = "Dificultatea este obligatorie")
    private String difficulty; // Începător / Mediu / Avansat

    private String emoji;

    private String bgColor;

    @Valid
    private List<QuizQuestionFormDto> questions = new ArrayList<>();

    public CourseFormDto() {}

    // ── Getters & Setters ──
    public Long getId()                              { return id; }
    public void setId(Long id)                       { this.id = id; }
    public String getTitle()                         { return title; }
    public void setTitle(String title)               { this.title = title; }
    public String getDescription()                   { return description; }
    public void setDescription(String v)             { this.description = v; }
    public String getContent()                       { return content; }
    public void setContent(String content)           { this.content = content; }
    public String getVideoUrl()                      { return videoUrl; }
    public void setVideoUrl(String v)                { this.videoUrl = v; }
    public Integer getRewardPoints()                 { return rewardPoints; }
    public void setRewardPoints(Integer v)           { this.rewardPoints = v; }
    public Integer getPassScorePercentage()          { return passScorePercentage; }
    public void setPassScorePercentage(Integer v)    { this.passScorePercentage = v; }
    public String getDifficulty()                    { return difficulty; }
    public void setDifficulty(String v)              { this.difficulty = v; }
    public String getEmoji()                         { return emoji; }
    public void setEmoji(String emoji)               { this.emoji = emoji; }
    public String getBgColor()                       { return bgColor; }
    public void setBgColor(String v)                 { this.bgColor = v; }
    public List<QuizQuestionFormDto> getQuestions()  { return questions; }
    public void setQuestions(List<QuizQuestionFormDto> q) { this.questions = q; }

    /** Sub-DTO pentru o întrebare de quiz. */
    public static class QuizQuestionFormDto {
        private Long id;

        @NotBlank(message = "Textul întrebării este obligatoriu")
        private String questionText;

        @NotBlank(message = "Răspunsul corect este obligatoriu")
        private String correctAnswer;

        @NotBlank(message = "Răspunsul greșit 1 este obligatoriu")
        private String wrongAnswer1;

        @NotBlank(message = "Răspunsul greșit 2 este obligatoriu")
        private String wrongAnswer2;

        @NotBlank(message = "Răspunsul greșit 3 este obligatoriu")
        private String wrongAnswer3;

        public Long getId()                       { return id; }
        public void setId(Long id)                { this.id = id; }
        public String getQuestionText()           { return questionText; }
        public void setQuestionText(String v)     { this.questionText = v; }
        public String getCorrectAnswer()          { return correctAnswer; }
        public void setCorrectAnswer(String v)    { this.correctAnswer = v; }
        public String getWrongAnswer1()           { return wrongAnswer1; }
        public void setWrongAnswer1(String v)     { this.wrongAnswer1 = v; }
        public String getWrongAnswer2()           { return wrongAnswer2; }
        public void setWrongAnswer2(String v)     { this.wrongAnswer2 = v; }
        public String getWrongAnswer3()           { return wrongAnswer3; }
        public void setWrongAnswer3(String v)     { this.wrongAnswer3 = v; }
    }
}
