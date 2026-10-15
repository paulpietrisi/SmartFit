package com.smartfit.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "user_settings")
public class UserSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    // ══ GENERAL ══
    private String language = "Română";
    private String unitSystem = "metric";
    private String timezone = "Europe/Bucharest (UTC+3)";
    private String weekStart = "Luni";
    private String dateFormat = "DD / MM / YYYY";

    // ══ TRACKING ══
    private String dailyReminderTime = "20:00";
    private boolean dailyReminderEnabled = true;
    private String weightReminderTime = "07:30";
    private boolean weightReminderEnabled = true;
    private String hydrationInterval = "La 90 minute";
    private boolean hydrationEnabled = true;
    private String defaultMacros = "P + C + G + Fibre";
    private boolean streakNotification = true;

    // ══ OBIECTIVE ══
    private int dailyCalories = 2200;
    private int dailyProtein = 180;
    private int dailyCarbs = 240;
    private int dailyFat = 90;
    private int dailyFiber = 30;
    private double dailyWater = 3.0;

    // ══ ASPECT ══
    private String theme = "light";
    private String accentColor = "#0066CC";
    private int fontSize = 14;
    private boolean animationsEnabled = true;
    private boolean blurEffect = true;
    private boolean animatedCharts = true;
    private boolean compactMode = false;
    private boolean tooltipsEnabled = true;

    // ══ NOTIFICARI – CANALE ══
    private boolean emailNotifications = true;
    private boolean pushNotifications = true;
    private boolean smsNotifications = false;

    // ══ NOTIFICARI – TRACKING ══
    private boolean notifDailyJournal = true;
    private boolean notifCalorieGoal = true;
    private boolean notifCalorieExceed = true;
    private boolean notifHydration = true;

    // ══ NOTIFICARI – COMENZI ══
    private boolean notifOrderConfirm = true;
    private boolean notifDelivery = true;
    private boolean notifFavoriteBack = true;
    private boolean notifOffers = false;

    // ══ NOTIFICARI – ACADEMIE ══
    private boolean notifNewCourse = true;
    private boolean notifStreakWarning = true;
    private boolean notifNewLevel = true;
    private boolean notifAcademySummary = false;

    // ══ NOTIFICARI – RAPOARTE ══
    private boolean notifNutritionSummary = true;
    private boolean notifReportReady = true;
    private boolean notifNewLogin = true;

    // ══ PROGRAM SILENTIOS ══
    private boolean quietMode = false;
    private String quietFrom = "22:00";
    private String quietTo = "07:00";

    // ══ CONFIDENTIALITATE ══
    private String profileVisibility = "Prieteni";
    private boolean leaderboardVisible = true;
    private boolean shareStats = false;
    private boolean usageAnalytics = true;
    private boolean personalizationCookies = true;
    private boolean personalizedRecommendations = true;
    private boolean aiImprovement = false;

    // ══ SECURITATE ══
    private String sessionTimeout = "1 oră";

    // ══ RETENTIE DATE ══
    private String journalRetention = "Forever";
    private String ordersRetention = "Forever";
    private String reportsRetention = "90 zile";

    public UserSettings() {}

    // ══ GETTERS & SETTERS ══
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getUnitSystem() { return unitSystem; }
    public void setUnitSystem(String unitSystem) { this.unitSystem = unitSystem; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public String getWeekStart() { return weekStart; }
    public void setWeekStart(String weekStart) { this.weekStart = weekStart; }

    public String getDateFormat() { return dateFormat; }
    public void setDateFormat(String dateFormat) { this.dateFormat = dateFormat; }

    public String getDailyReminderTime() { return dailyReminderTime; }
    public void setDailyReminderTime(String dailyReminderTime) { this.dailyReminderTime = dailyReminderTime; }

    public boolean isDailyReminderEnabled() { return dailyReminderEnabled; }
    public void setDailyReminderEnabled(boolean dailyReminderEnabled) { this.dailyReminderEnabled = dailyReminderEnabled; }

    public String getWeightReminderTime() { return weightReminderTime; }
    public void setWeightReminderTime(String weightReminderTime) { this.weightReminderTime = weightReminderTime; }

    public boolean isWeightReminderEnabled() { return weightReminderEnabled; }
    public void setWeightReminderEnabled(boolean weightReminderEnabled) { this.weightReminderEnabled = weightReminderEnabled; }

    public String getHydrationInterval() { return hydrationInterval; }
    public void setHydrationInterval(String hydrationInterval) { this.hydrationInterval = hydrationInterval; }

    public boolean isHydrationEnabled() { return hydrationEnabled; }
    public void setHydrationEnabled(boolean hydrationEnabled) { this.hydrationEnabled = hydrationEnabled; }

    public String getDefaultMacros() { return defaultMacros; }
    public void setDefaultMacros(String defaultMacros) { this.defaultMacros = defaultMacros; }

    public boolean isStreakNotification() { return streakNotification; }
    public void setStreakNotification(boolean streakNotification) { this.streakNotification = streakNotification; }

    public int getDailyCalories() { return dailyCalories; }
    public void setDailyCalories(int dailyCalories) { this.dailyCalories = dailyCalories; }

    public int getDailyProtein() { return dailyProtein; }
    public void setDailyProtein(int dailyProtein) { this.dailyProtein = dailyProtein; }

    public int getDailyCarbs() { return dailyCarbs; }
    public void setDailyCarbs(int dailyCarbs) { this.dailyCarbs = dailyCarbs; }

    public int getDailyFat() { return dailyFat; }
    public void setDailyFat(int dailyFat) { this.dailyFat = dailyFat; }

    public int getDailyFiber() { return dailyFiber; }
    public void setDailyFiber(int dailyFiber) { this.dailyFiber = dailyFiber; }

    public double getDailyWater() { return dailyWater; }
    public void setDailyWater(double dailyWater) { this.dailyWater = dailyWater; }

    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }

    public String getAccentColor() { return accentColor; }
    public void setAccentColor(String accentColor) { this.accentColor = accentColor; }

    public int getFontSize() { return fontSize; }
    public void setFontSize(int fontSize) { this.fontSize = fontSize; }

    public boolean isAnimationsEnabled() { return animationsEnabled; }
    public void setAnimationsEnabled(boolean animationsEnabled) { this.animationsEnabled = animationsEnabled; }

    public boolean isBlurEffect() { return blurEffect; }
    public void setBlurEffect(boolean blurEffect) { this.blurEffect = blurEffect; }

    public boolean isAnimatedCharts() { return animatedCharts; }
    public void setAnimatedCharts(boolean animatedCharts) { this.animatedCharts = animatedCharts; }

    public boolean isCompactMode() { return compactMode; }
    public void setCompactMode(boolean compactMode) { this.compactMode = compactMode; }

    public boolean isTooltipsEnabled() { return tooltipsEnabled; }
    public void setTooltipsEnabled(boolean tooltipsEnabled) { this.tooltipsEnabled = tooltipsEnabled; }

    public boolean isEmailNotifications() { return emailNotifications; }
    public void setEmailNotifications(boolean emailNotifications) { this.emailNotifications = emailNotifications; }

    public boolean isPushNotifications() { return pushNotifications; }
    public void setPushNotifications(boolean pushNotifications) { this.pushNotifications = pushNotifications; }

    public boolean isSmsNotifications() { return smsNotifications; }
    public void setSmsNotifications(boolean smsNotifications) { this.smsNotifications = smsNotifications; }

    public boolean isNotifDailyJournal() { return notifDailyJournal; }
    public void setNotifDailyJournal(boolean notifDailyJournal) { this.notifDailyJournal = notifDailyJournal; }

    public boolean isNotifCalorieGoal() { return notifCalorieGoal; }
    public void setNotifCalorieGoal(boolean notifCalorieGoal) { this.notifCalorieGoal = notifCalorieGoal; }

    public boolean isNotifCalorieExceed() { return notifCalorieExceed; }
    public void setNotifCalorieExceed(boolean notifCalorieExceed) { this.notifCalorieExceed = notifCalorieExceed; }

    public boolean isNotifHydration() { return notifHydration; }
    public void setNotifHydration(boolean notifHydration) { this.notifHydration = notifHydration; }

    public boolean isNotifOrderConfirm() { return notifOrderConfirm; }
    public void setNotifOrderConfirm(boolean notifOrderConfirm) { this.notifOrderConfirm = notifOrderConfirm; }

    public boolean isNotifDelivery() { return notifDelivery; }
    public void setNotifDelivery(boolean notifDelivery) { this.notifDelivery = notifDelivery; }

    public boolean isNotifFavoriteBack() { return notifFavoriteBack; }
    public void setNotifFavoriteBack(boolean notifFavoriteBack) { this.notifFavoriteBack = notifFavoriteBack; }

    public boolean isNotifOffers() { return notifOffers; }
    public void setNotifOffers(boolean notifOffers) { this.notifOffers = notifOffers; }

    public boolean isNotifNewCourse() { return notifNewCourse; }
    public void setNotifNewCourse(boolean notifNewCourse) { this.notifNewCourse = notifNewCourse; }

    public boolean isNotifStreakWarning() { return notifStreakWarning; }
    public void setNotifStreakWarning(boolean notifStreakWarning) { this.notifStreakWarning = notifStreakWarning; }

    public boolean isNotifNewLevel() { return notifNewLevel; }
    public void setNotifNewLevel(boolean notifNewLevel) { this.notifNewLevel = notifNewLevel; }

    public boolean isNotifAcademySummary() { return notifAcademySummary; }
    public void setNotifAcademySummary(boolean notifAcademySummary) { this.notifAcademySummary = notifAcademySummary; }

    public boolean isNotifNutritionSummary() { return notifNutritionSummary; }
    public void setNotifNutritionSummary(boolean notifNutritionSummary) { this.notifNutritionSummary = notifNutritionSummary; }

    public boolean isNotifReportReady() { return notifReportReady; }
    public void setNotifReportReady(boolean notifReportReady) { this.notifReportReady = notifReportReady; }

    public boolean isNotifNewLogin() { return notifNewLogin; }
    public void setNotifNewLogin(boolean notifNewLogin) { this.notifNewLogin = notifNewLogin; }

    public boolean isQuietMode() { return quietMode; }
    public void setQuietMode(boolean quietMode) { this.quietMode = quietMode; }

    public String getQuietFrom() { return quietFrom; }
    public void setQuietFrom(String quietFrom) { this.quietFrom = quietFrom; }

    public String getQuietTo() { return quietTo; }
    public void setQuietTo(String quietTo) { this.quietTo = quietTo; }

    public String getProfileVisibility() { return profileVisibility; }
    public void setProfileVisibility(String profileVisibility) { this.profileVisibility = profileVisibility; }

    public boolean isLeaderboardVisible() { return leaderboardVisible; }
    public void setLeaderboardVisible(boolean leaderboardVisible) { this.leaderboardVisible = leaderboardVisible; }

    public boolean isShareStats() { return shareStats; }
    public void setShareStats(boolean shareStats) { this.shareStats = shareStats; }

    public boolean isUsageAnalytics() { return usageAnalytics; }
    public void setUsageAnalytics(boolean usageAnalytics) { this.usageAnalytics = usageAnalytics; }

    public boolean isPersonalizationCookies() { return personalizationCookies; }
    public void setPersonalizationCookies(boolean personalizationCookies) { this.personalizationCookies = personalizationCookies; }

    public boolean isPersonalizedRecommendations() { return personalizedRecommendations; }
    public void setPersonalizedRecommendations(boolean personalizedRecommendations) { this.personalizedRecommendations = personalizedRecommendations; }

    public boolean isAiImprovement() { return aiImprovement; }
    public void setAiImprovement(boolean aiImprovement) { this.aiImprovement = aiImprovement; }

    public String getSessionTimeout() { return sessionTimeout; }
    public void setSessionTimeout(String sessionTimeout) { this.sessionTimeout = sessionTimeout; }

    public String getJournalRetention() { return journalRetention; }
    public void setJournalRetention(String journalRetention) { this.journalRetention = journalRetention; }

    public String getOrdersRetention() { return ordersRetention; }
    public void setOrdersRetention(String ordersRetention) { this.ordersRetention = ordersRetention; }

    public String getReportsRetention() { return reportsRetention; }
    public void setReportsRetention(String reportsRetention) { this.reportsRetention = reportsRetention; }
}