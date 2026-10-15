package com.smartfit.controller;

import com.smartfit.entity.Product;
import com.smartfit.entity.User;
import com.smartfit.repository.ProductRepository;
import com.smartfit.repository.UserRepository;
import com.smartfit.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ResponseBody;
import com.smartfit.repository.CourseCompletionRepository;
import com.smartfit.repository.CourseEnrollmentRepository;
import com.smartfit.entity.CourseEnrollment;
import com.smartfit.entity.Order;
import com.smartfit.service.OrderService;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.smartfit.repository.WeightHistoryRepository;
import com.smartfit.repository.DiaryEntryRepository;
import com.smartfit.repository.DailyLogRepository;
import com.smartfit.entity.WeightHistory;
import com.smartfit.entity.DiaryEntry;
import com.smartfit.entity.DailyLog;

@Controller
public class MainController {

    // ── Record pentru exerciții (Java 21) ────────────────────────
    record Exercise(String emoji, String name, String volume,
                    String tip, String color, String category) {}

    // ── Record pentru mesele afișate în dashboard ────────────────
    record MealView(String icon, String label, String foodName, double quantityG,
                    long kcal, long protein, long carbs, long fat) {}

    // ── Record pentru comenzile afișate în dashboard ──────────────
    record OrderDisplay(Order order, String productName, String statusLabel, String statusClass) {}

    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProductRepository productRepository;
    private final OrderService orderService;
    private final CourseCompletionRepository courseCompletionRepository;
    private final WeightHistoryRepository weightHistoryRepository;
    private final DiaryEntryRepository diaryEntryRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final DailyLogRepository dailyLogRepository;

    public MainController(UserService userService,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          ProductRepository productRepository,
                          CourseCompletionRepository courseCompletionRepository,
                          OrderService orderService,
                          WeightHistoryRepository weightHistoryRepository,
                          DiaryEntryRepository diaryEntryRepository,
                          CourseEnrollmentRepository courseEnrollmentRepository,
                          DailyLogRepository dailyLogRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.productRepository = productRepository;
        this.courseCompletionRepository = courseCompletionRepository;
        this.orderService = orderService;
        this.weightHistoryRepository = weightHistoryRepository;
        this.diaryEntryRepository = diaryEntryRepository;
        this.courseEnrollmentRepository = courseEnrollmentRepository;
        this.dailyLogRepository = dailyLogRepository;
    }

    // ====================== PAGINI STATICE ======================

    @GetMapping({"/", "/home"})
    public String home() { return "home"; }

    @GetMapping("/login")
    public String login() { return "login"; }

    @GetMapping("/orders")
    public String orders(Model model) {
        User user = getAuthenticatedUser();
        addUserToModel(model, user);
        List<Order> orders = orderService.getOrdersByUser(user);
        model.addAttribute("orders", orders);
        return "orders";
    }

    @GetMapping("/magazin")
    public String shop(Model model) {
        boolean auth = isLoggedIn();
        model.addAttribute("isAuthenticated", auth);

        List<Product> products = productRepository.findAll();
        model.addAttribute("products", products);

        if (auth) {
            User user = getAuthenticatedUser();
            addUserToModel(model, user);

            int fitPts = user.getFitPoints() != null ? user.getFitPoints() : 0;
            int discount = Math.min(25, 5 + fitPts / 100);
            model.addAttribute("discount", discount);
            model.addAttribute("fitPoints", fitPts);

            int completedCourses = courseCompletionRepository
                    .countCompletedCoursesByUserId(user.getId()).intValue();
            model.addAttribute("completedCoursesCount", completedCourses);
        } else {
            addAnonymousToModel(model);
            model.addAttribute("discount", 0);
            model.addAttribute("fitPoints", 0);
            model.addAttribute("completedCoursesCount", 0);
        }
        return "shop";
    }

    @GetMapping("/jurnal-alimentar")
    public String jurnalAlimentar(Model model) {
        addUserToModel(model);
        return "journal-alimentar";
    }



    // ====================== INREGISTRARE ======================

    @GetMapping("/register")
    public String registerPage() { return "register"; }

    @PostMapping("/register")
    public String processRegister(
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam(defaultValue = "M") String gender,
            Model model) {

        if (userRepository.findByEmail(email) != null) {
            model.addAttribute("emailError", true);
            model.addAttribute("firstName", firstName);
            model.addAttribute("lastName", lastName);
            model.addAttribute("emailVal", email);
            return "register";
        }

        if (password == null || password.length() < 6) {
            model.addAttribute("pwError", true);
            model.addAttribute("firstName", firstName);
            model.addAttribute("lastName", lastName);
            model.addAttribute("emailVal", email);
            return "register";
        }

        User user = new User();
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole("USER");
        user.setFitPoints(0);
        user.setGoal("MAINTAIN");
        user.setGender(gender);
        user.setActivityLevel(1.375);
        user.setPasswordHash(passwordEncoder.encode(password));
        userService.saveUser(user);

        return "redirect:/login?registered";
    }

    // ====================== DASHBOARD ======================

    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {
        User user = getAuthenticatedUser();
        addUserToModel(model, user);

        double weight = user.getWeightKg()      != null ? user.getWeightKg()      : 70.0;
        double height = user.getHeightCm()      != null ? user.getHeightCm()      : 170.0;
        String gender = user.getGender()        != null ? user.getGender()        : "M";
        String goal   = user.getGoal()          != null ? user.getGoal()          : "MAINTAIN";
        double actLvl = user.getActivityLevel() != null ? user.getActivityLevel() : 1.375;
        int fitPts    = user.getFitPoints()     != null ? user.getFitPoints()     : 0;

        int age = 22;
        if (user.getBirthDate() != null)
            age = Period.between(user.getBirthDate(), LocalDate.now()).getYears();

        double sexOffset = "F".equals(gender) ? -161 : 5;
        long bmr  = Math.round(10 * weight + 6.25 * height - 5.0 * age + sexOffset);
        long tdee = Math.round(bmr * actLvl);

        long caloricTarget; String goalLabel;
        switch (goal) {
            case "LOSE_WEIGHT" -> { caloricTarget = tdee - 500; goalLabel = "Slăbire"; }
            case "GAIN_MUSCLE" -> { caloricTarget = tdee + 300; goalLabel = "Masă musculară"; }
            default            -> { caloricTarget = tdee;       goalLabel = "Menținere"; }
        }

        long proteinTarget = Math.round(weight * 2.0);
        long carbTarget    = Math.round(caloricTarget * 0.45 / 4.0);
        long fatTarget     = Math.round(caloricTarget * 0.25 / 9.0);

        String xpLevel, nextLevel;
        int xpThisTier, xpNextTier;
        if      (fitPts < 500)  { xpLevel = "Bronze";   nextLevel = "Silver";   xpThisTier = 0;    xpNextTier = 500;  }
        else if (fitPts < 1500) { xpLevel = "Silver";   nextLevel = "Gold";     xpThisTier = 500;  xpNextTier = 1500; }
        else if (fitPts < 3000) { xpLevel = "Gold";     nextLevel = "Platinum"; xpThisTier = 1500; xpNextTier = 3000; }
        else                    { xpLevel = "Platinum"; nextLevel = "Max";      xpThisTier = 3000; xpNextTier = 5000; }

        int xpInTier      = fitPts - xpThisTier;
        int tierSize      = xpNextTier - xpThisTier;
        int xpProgressPct = (int) Math.min(100, (xpInTier * 100.0) / tierSize);
        int xpToNextLevel = Math.max(0, xpNextTier - fitPts);
        int discount      = Math.min(25, 5 + fitPts / 100);

        String todayFormatted = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("ro")));

        model.addAttribute("bmr",            bmr);
        model.addAttribute("tdee",           tdee);
        model.addAttribute("caloricTarget",  caloricTarget);
        model.addAttribute("goalLabel",      goalLabel);
        model.addAttribute("proteinTarget",  proteinTarget);
        model.addAttribute("carbTarget",     carbTarget);
        model.addAttribute("fatTarget",      fatTarget);
        model.addAttribute("xpLevel",        xpLevel);
        model.addAttribute("nextLevel",      nextLevel);
        model.addAttribute("xpProgressPct",  xpProgressPct);
        model.addAttribute("xpToNextLevel",  xpToNextLevel);
        model.addAttribute("discount",       discount);
        model.addAttribute("todayFormatted", todayFormatted);

        // ── GRAFIC GREUTATE — ULTIMELE 30 ZILE ──────────────────────
        LocalDate today   = LocalDate.now();
        LocalDate start30 = today.minusDays(29);

        List<WeightHistory> weightHistory = weightHistoryRepository.findLast30Days(user, start30, today);
        Map<LocalDate, Double> weightMap = new HashMap<>();
        for (WeightHistory wh : weightHistory) {
            weightMap.put(wh.getLoggedDate(), wh.getWeightKg());
        }

        List<Double> weightSeries = new ArrayList<>();
        Double lastKnownWeight = weight;
        for (int i = 0; i < 30; i++) {
            LocalDate d = start30.plusDays(i);
            if (weightMap.containsKey(d)) lastKnownWeight = weightMap.get(d);
            weightSeries.add(lastKnownWeight);
        }
        model.addAttribute("weightSeries", weightSeries);
        model.addAttribute("weightMin", String.format(Locale.US, "%.1f",
                weightSeries.stream().min(Double::compare).orElse(weight)));
        model.addAttribute("weightMax", String.format(Locale.US, "%.1f",
                weightSeries.stream().max(Double::compare).orElse(weight)));

        // ── MACRO-NUTRIENȚI & MESE — ZIUA DE AZI ────────────────────
        List<DiaryEntry> todayEntries = diaryEntryRepository.findByUserAndDate(user, today);

        double consumedKcal = 0, consumedProtein = 0, consumedCarbs = 0, consumedFat = 0;
        List<MealView> mealViews = new ArrayList<>();

        for (DiaryEntry e : todayEntries) {
            double factor = e.getQuantityG() / 100.0;
            double kcal = e.getFood().getKcalPer100g() * factor;
            double prot = e.getFood().getProtein()     * factor;
            double carb = e.getFood().getCarbs()       * factor;
            double fat  = e.getFood().getFat()         * factor;

            consumedKcal    += kcal;
            consumedProtein += prot;
            consumedCarbs   += carb;
            consumedFat     += fat;

            String icon, label;
            switch (e.getMealType()) {
                case "BREAKFAST" -> { icon = "🥣"; label = "Mic dejun"; }
                case "LUNCH"     -> { icon = "🍗"; label = "Prânz"; }
                case "SNACK"     -> { icon = "🥤"; label = "Gustare"; }
                case "DINNER"    -> { icon = "🥗"; label = "Cină"; }
                default          -> { icon = "🍽️"; label = e.getMealType(); }
            }

            mealViews.add(new MealView(icon, label, e.getFood().getName(),
                    e.getQuantityG(), Math.round(kcal), Math.round(prot), Math.round(carb), Math.round(fat)));
        }

        int proteinPct = proteinTarget > 0 ? (int) Math.min(100, Math.round(consumedProtein / proteinTarget * 100)) : 0;
        int carbPct    = carbTarget    > 0 ? (int) Math.min(100, Math.round(consumedCarbs   / carbTarget    * 100)) : 0;
        int fatPct     = fatTarget     > 0 ? (int) Math.min(100, Math.round(consumedFat     / fatTarget     * 100)) : 0;

        model.addAttribute("consumedKcal",    Math.round(consumedKcal));
        model.addAttribute("consumedProtein", Math.round(consumedProtein));
        model.addAttribute("consumedCarbs",   Math.round(consumedCarbs));
        model.addAttribute("consumedFat",     Math.round(consumedFat));
        model.addAttribute("proteinPct", proteinPct);
        model.addAttribute("carbPct",    carbPct);
        model.addAttribute("fatPct",     fatPct);
        model.addAttribute("mealViews",  mealViews);

        // Segmente donut (cerc r=46 → circumferință ≈ 289.0)
        double circumference = 289.0;
        double proteinKcal = consumedProtein * 4;
        double carbKcal    = consumedCarbs   * 4;
        double fatKcal     = consumedFat     * 9;
        double proteinDash = caloricTarget > 0 ? circumference * (proteinKcal / caloricTarget) : 0;
        double carbDash    = caloricTarget > 0 ? circumference * (carbKcal    / caloricTarget) : 0;
        double fatDash     = caloricTarget > 0 ? circumference * (fatKcal     / caloricTarget) : 0;

        model.addAttribute("circumference", String.format(Locale.US, "%.1f", circumference));
        model.addAttribute("proteinDash",    String.format(Locale.US, "%.1f", proteinDash));
        model.addAttribute("carbDash",       String.format(Locale.US, "%.1f", carbDash));
        model.addAttribute("fatDash",        String.format(Locale.US, "%.1f", fatDash));
        model.addAttribute("carbOffset",     String.format(Locale.US, "%.1f", -proteinDash));
        model.addAttribute("fatOffset",      String.format(Locale.US, "%.1f", -(proteinDash + carbDash)));

        // ── COMENZI RECENTE ──────────────────────────────────────────
        List<Order> allOrders = orderService.getOrdersByUser(user);
        List<Order> raw = allOrders.size() > 2 ? allOrders.subList(0, 2) : allOrders;

        List<OrderDisplay> recentOrders = raw.stream().map(o -> {
            String pName;
            if (o.getItems() == null || o.getItems().isEmpty()) {
                pName = "Comandă";
            } else {
                pName = o.getItems().get(0).getProduct().getName();
                if (o.getItems().size() > 1)
                    pName += " + " + (o.getItems().size() - 1) + " alte produse";
            }
            String sLabel, sClass;
            switch (o.getStatus()) {
                case "DELIVERED" -> { sLabel = "● Livrat";        sClass = "b-green"; }
                case "PENDING"   -> { sLabel = "✓ Confirmed";  sClass = "b-orange"; }
                default          -> { sLabel = "◌ " + o.getStatus(); sClass = "b-blue"; }
            }
            return new OrderDisplay(o, pName, sLabel, sClass);
        }).toList();
        model.addAttribute("recentOrders", recentOrders);

        // ── CURSURI ÎN PROGRES ────────────────────────────────────────
        List<CourseEnrollment> enrollments = courseEnrollmentRepository.findByUserId(user.getId());
        List<CourseEnrollment> inProgress = enrollments.stream()
                .filter(e -> e.getProgressPct() != null && e.getProgressPct() < 100)
                .sorted((a, b) -> b.getEnrolledAt().compareTo(a.getEnrolledAt()))
                .limit(3)
                .toList();
        model.addAttribute("inProgressCourses", inProgress);

        // ── SIDEBAR BADGES ────────────────────────────────────────────
        try {
            @SuppressWarnings("unchecked")
            List<?> cartItems = (List<?>) session.getAttribute("cartItems");
            model.addAttribute("cartCount", cartItems != null ? cartItems.size() : 0);
        } catch (Exception ignored) {
            model.addAttribute("cartCount", 0);
        }
        int completedCourses = courseCompletionRepository.countCompletedCoursesByUserId(user.getId()).intValue();
        model.addAttribute("completedCoursesCount", completedCourses);

        // ── STATISTICI RAPIDE (hidratare, pași, somn) ───────────────
        DailyLog todayLog = dailyLogRepository.findByUserAndDate(user, today)
                .orElseGet(() -> {
                    DailyLog dl = new DailyLog();
                    dl.setUser(user);
                    dl.setLogDate(today);
                    return dl;
                });

        int waterMl       = todayLog.getWaterMl()      != null ? todayLog.getWaterMl()      : 0;
        int steps         = todayLog.getSteps()        != null ? todayLog.getSteps()        : 0;
        int sleepMinutes  = todayLog.getSleepMinutes() != null ? todayLog.getSleepMinutes() : 0;

        int waterTargetMl  = (int) Math.round(weight * 35); // 35ml/kg
        int stepsTarget    = 10000;
        int sleepTargetMin = 480; // 8h

        int waterPct = waterTargetMl > 0 ? (int) Math.min(100, Math.round(waterMl * 100.0 / waterTargetMl)) : 0;
        int stepsPct = (int) Math.min(100, Math.round(steps * 100.0 / stepsTarget));
        int sleepPct = (int) Math.min(100, Math.round(sleepMinutes * 100.0 / sleepTargetMin));

        model.addAttribute("waterMl",        waterMl);
        model.addAttribute("waterTargetL",   String.format(Locale.US, "%.1f", waterTargetMl / 1000.0));
        model.addAttribute("waterCurrentL",  String.format(Locale.US, "%.1f", waterMl / 1000.0));
        model.addAttribute("waterPct",       waterPct);
        model.addAttribute("steps",          steps);
        model.addAttribute("stepsPct",       stepsPct);
        model.addAttribute("sleepHours",     sleepMinutes / 60);
        model.addAttribute("sleepMins",      sleepMinutes % 60);
        model.addAttribute("sleepPct",       sleepPct);

        return "dashboard";
    }

    @PostMapping("/dashboard/quick-update")
    public String quickUpdate(
            @RequestParam(required = false) Integer waterMl,
            @RequestParam(required = false) Integer steps,
            @RequestParam(required = false) Integer sleepHours,
            @RequestParam(required = false) Integer sleepMins) {

        User user = getAuthenticatedUser();
        LocalDate today = LocalDate.now();

        DailyLog log = dailyLogRepository.findByUserAndDate(user, today)
                .orElseGet(() -> {
                    DailyLog dl = new DailyLog();
                    dl.setUser(user);
                    dl.setLogDate(today);
                    return dl;
                });

        if (waterMl != null) {
            int current = log.getWaterMl() != null ? log.getWaterMl() : 0;
            log.setWaterMl(Math.max(0, current + waterMl)); // increment relativ
        }
        if (steps != null) {
            log.setSteps(steps); // valoare absolută
        }
        if (sleepHours != null || sleepMins != null) {
            int h = sleepHours != null ? sleepHours : 0;
            int m = sleepMins  != null ? sleepMins  : 0;
            log.setSleepMinutes(h * 60 + m);
        }

        dailyLogRepository.save(log);
        return "redirect:/dashboard";
    }

    // ====================== TRACKING ======================

    @GetMapping("/tracking")
    public String tracking(@RequestParam(required = false) String date, Model model) {
        User user = getAuthenticatedUser();
        addUserToModel(model, user);

        // Determinăm data țintă (fie cea din URL, fie ziua de azi)
        LocalDate targetDate = LocalDate.now();
        if (date != null && !date.isEmpty()) {
            targetDate = LocalDate.parse(date);
        }
        LocalDate start30 = targetDate.minusDays(29);

        double weight = user.getWeightKg()      != null ? user.getWeightKg()      : 70.0;
        double height = user.getHeightCm()      != null ? user.getHeightCm()      : 170.0;
        String gender = user.getGender()        != null ? user.getGender()        : "M";
        String goal   = user.getGoal()          != null ? user.getGoal()          : "MAINTAIN";
        double actLvl = user.getActivityLevel() != null ? user.getActivityLevel() : 1.375;

        int age = 22;
        if (user.getBirthDate() != null)
            age = Period.between(user.getBirthDate(), LocalDate.now()).getYears();

        // ── BMR / TDEE ─────────────────────────────────────────────
        double sexOffset = "F".equals(gender) ? -161 : 5;
        long bmr  = Math.round(10 * weight + 6.25 * height - 5.0 * age + sexOffset);
        long tdee = Math.round(bmr * actLvl);

        // ── ȚINTĂ CALORICĂ ─────────────────────────────────────────
        long   caloricTarget;
        String goalLabel, goalTip, nutritionAdvice;

        switch (goal) {
            case "LOSE_WEIGHT" -> {
                caloricTarget   = tdee - 500;
                goalLabel       = "Slăbire";
                goalTip         = "Deficit de 500 kcal/zi → −0.5 kg/săptămână";
                nutritionAdvice = "Prioritizează proteinele pentru a păstra masa musculară. " +
                        "Alege glucide complexe și limitează zaharurile simple.";
            }
            case "GAIN_MUSCLE" -> {
                caloricTarget   = tdee + 300;
                goalLabel       = "Masă musculară";
                goalTip         = "Surplus de 300 kcal/zi → creștere musculară curată";
                nutritionAdvice = "Consumă 2g proteine/kg corp zilnic. Glucidele sunt " +
                        "combustibilul antrenamentelor — nu le evita.";
            }
            default -> {
                caloricTarget   = tdee;
                goalLabel       = "Menținere";
                goalTip         = "Aport egal cu TDEE";
                nutritionAdvice = "Menține echilibrul: 40% glucide, 30% proteine, 30% grăsimi.";
            }
        }

        // ── MACROS TARGET ──────────────────────────────────────────
        long proteinTarget, carbTarget, fatTarget;
        switch (goal) {
            case "LOSE_WEIGHT" -> {
                proteinTarget = Math.round(weight * 1.8);
                fatTarget     = Math.round(caloricTarget * 0.28 / 9.0);
                carbTarget    = Math.round((caloricTarget - proteinTarget * 4 - fatTarget * 9) / 4.0);
            }
            case "GAIN_MUSCLE" -> {
                proteinTarget = Math.round(weight * 2.0);
                fatTarget     = Math.round(caloricTarget * 0.25 / 9.0);
                carbTarget    = Math.round((caloricTarget - proteinTarget * 4 - fatTarget * 9) / 4.0);
            }
            default -> {
                proteinTarget = Math.round(weight * 1.6);
                fatTarget     = Math.round(caloricTarget * 0.28 / 9.0);
                carbTarget    = Math.round((caloricTarget - proteinTarget * 4 - fatTarget * 9) / 4.0);
            }
        }

        double waterTarget  = Math.round(weight * 0.035 * 10.0) / 10.0;
        int    waterGlasses = (int) Math.round(waterTarget / 0.25);

        int proteinPct = (int) Math.round(proteinTarget * 4.0 / caloricTarget * 100);
        int fatPct     = (int) Math.round(fatTarget     * 9.0 / caloricTarget * 100);
        int carbPct    = 100 - proteinPct - fatPct;

        // ── MACROS CONSUMATE IN ZIUA TINTA ─────────────────────────
        List<DiaryEntry> targetDateEntries = diaryEntryRepository.findByUserAndDate(user, targetDate);

        double consumedKcal = 0, consumedProtein = 0, consumedCarbs = 0, consumedFat = 0;

        for (DiaryEntry e : targetDateEntries) {
            double factor = e.getQuantityG() / 100.0;
            consumedKcal    += e.getFood().getKcalPer100g() * factor;
            consumedProtein += e.getFood().getProtein()     * factor;
            consumedCarbs   += e.getFood().getCarbs()       * factor;
            consumedFat     += e.getFood().getFat()         * factor;
        }

        int pctP = proteinTarget > 0 ? (int) Math.min(100, Math.round(consumedProtein / proteinTarget * 100)) : 0;
        int pctC = carbTarget    > 0 ? (int) Math.min(100, Math.round(consumedCarbs   / carbTarget    * 100)) : 0;
        int pctF = fatTarget     > 0 ? (int) Math.min(100, Math.round(consumedFat     / fatTarget     * 100)) : 0;

        // ── WEIGHT HISTORY 30 zile → Lista Completa ─────────────────
        List<WeightHistory> weightHistory = weightHistoryRepository.findLast30Days(user, start30, targetDate);
        Map<LocalDate, Double> weightMap = new HashMap<>();
        for (WeightHistory wh : weightHistory) {
            weightMap.put(wh.getLoggedDate(), wh.getWeightKg().doubleValue());
        }

        List<Double> finalWeights = new ArrayList<>();
        Double lastKnownWeight = user.getWeightKg() != null ? user.getWeightKg() : 70.0;

        for (int i = 0; i < 30; i++) {
            LocalDate currentDay = start30.plusDays(i);
            if (weightMap.containsKey(currentDay)) {
                lastKnownWeight = weightMap.get(currentDay);
            }
            finalWeights.add(lastKnownWeight);
        }

        // ── CALORIC HISTORY 30 zile → Lista Completa ────────────────
        List<Object[]> caloricRows = diaryEntryRepository.findDailyKcalLast30Days(user, start30, targetDate);
        Map<LocalDate, Long> kcalByDay = new HashMap<>();
        for (Object[] row : caloricRows) {
            LocalDate d = (LocalDate) row[0];
            long k = Math.round(((Number) row[1]).doubleValue());
            kcalByDay.put(d, k);
        }

        List<Long> finalCals = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            LocalDate currentDay = start30.plusDays(i);
            finalCals.add(kcalByDay.getOrDefault(currentDay, 0L));
        }

        // ── MODEL ATTRIBUTES ───────────────────────────────────────
        model.addAttribute("bmr",             bmr);
        model.addAttribute("tdee",            tdee);
        model.addAttribute("caloricTarget",   caloricTarget);
        model.addAttribute("goalLabel",       goalLabel);
        model.addAttribute("goalTip",         goalTip);
        model.addAttribute("nutritionAdvice", nutritionAdvice);
        model.addAttribute("proteinTarget",   proteinTarget);
        model.addAttribute("carbTarget",      carbTarget);
        model.addAttribute("fatTarget",       fatTarget);
        model.addAttribute("waterTarget",     waterTarget);
        model.addAttribute("waterGlasses",    waterGlasses);
        model.addAttribute("proteinPct",      proteinPct);
        model.addAttribute("carbPct",         carbPct);
        model.addAttribute("fatPct",          fatPct);
        // Valori consumate în ziua selectată
        model.addAttribute("consumedKcal",    (long) consumedKcal);
        model.addAttribute("consumedProtein", (long) consumedProtein);
        model.addAttribute("consumedCarbs",   (long) consumedCarbs);
        model.addAttribute("consumedFat",     (long) consumedFat);
        model.addAttribute("pctP",            pctP);
        model.addAttribute("pctC",            pctC);
        model.addAttribute("pctF",            pctF);
        // Liste convertite spre JSON prin metoda toString()
        // Trimitem listele direct către HTML
        model.addAttribute("weightJson",      finalWeights);
        model.addAttribute("calsJson",        finalCals);

        // ── EXERCIȚII ───────────────────
        List<Exercise> exercises;
        int    dailySteps;
        String workoutNote, workoutFrequency;

        switch (goal) {
            case "GAIN_MUSCLE" -> {
                dailySteps = 8000; workoutFrequency = "4 zile/săptămână";
                workoutNote = "Antrenament de forță cu progresie liniară. Pauze 90–120s.";
                exercises = List.of(
                        new Exercise("🏋️","Bench Press","4 × 6–8 rep",
                                "Greutate progresivă săptămânal","#EAF3FF","Piept"),
                        new Exercise("🦵","Back Squat","4 × 6–8 rep",
                                "Coboară până la paralel · genunchi peste vârf","#E8F8EF","Picioare"),
                        new Exercise("🔱","Barbell Row","4 × 8–10 rep",
                                "Spatele drept · coatele trase posterior","#FFF9EC","Spate"),
                        new Exercise("💀","Deadlift","3 × 5 rep",
                                "Cea mai importantă mișcare compusă · spate neutru","#FFF0E8","Spate/Picioare"),
                        new Exercise("🏃","Pull-ups","3 × max rep",
                                "Grip pronație · corp controlat la coborâre","#F0E8FF","Spate/Biceps"),
                        new Exercise("💪","Dumbbell Press","3 × 10–12 rep",
                                "Mișcare completă · control la coborâre","#EAF3FF","Umeri")
                );
            }
            case "LOSE_WEIGHT" -> {
                dailySteps = 12000; workoutFrequency = "5 zile/săptămână";
                workoutNote = "Cardio + circuit. Pauze scurte 30–45s.";
                exercises = List.of(
                        new Exercise("🏃","Alergare / Jogging","30–45 min",
                                "65–75% FCmax","#EAF3FF","Cardio"),
                        new Exercise("🦵","Bodyweight Squat","4 × 20 rep",
                                "Tempo controlat","#E8F8EF","Picioare"),
                        new Exercise("🌊","Burpees","4 × 15 rep",
                                "Exercițiu complet · arde maxim calorii","#FFF9EC","Full Body"),
                        new Exercise("💪","Push-ups","3 × 15–20 rep",
                                "Variante inclinat / declinat","#F0E8FF","Piept/Triceps"),
                        new Exercise("🧘","Plank","3 × 60 sec",
                                "Core activ · șolduri la nivelul corpului","#FFF0E8","Core"),
                        new Exercise("🚴","Cycling HIIT","20 min",
                                "1 min rapid / 2 min lent · 6–7 runde","#EAF3FF","Cardio")
                );
            }
            default -> {
                dailySteps = 10000; workoutFrequency = "3 zile/săptămână";
                workoutNote = "Antrenament echilibrat forță + cardio.";
                exercises = List.of(
                        new Exercise("🏋️","Bench Press","3 × 10 rep",
                                "Greutate moderată · tehnică corectă","#EAF3FF","Piept"),
                        new Exercise("🦵","Squat cu bara","3 × 10 rep",
                                "Profunzime bună · genunchi în ax","#E8F8EF","Picioare"),
                        new Exercise("🏃","Alergare","20–30 min",
                                "Menținerea condiției cardio","#FFF9EC","Cardio"),
                        new Exercise("🔱","Tracțiuni + Flotări","3 × 10–12 rep",
                                "Full body · propria greutate","#F0E8FF","Spate/Piept"),
                        new Exercise("🧘","Stretching","15 min",
                                "Flexibilitate și prevenirea accidentărilor","#FFF0E8","Recuperare")
                );
            }
        }

        model.addAttribute("exercises",       exercises);
        model.addAttribute("dailySteps",      dailySteps);
        model.addAttribute("workoutNote",     workoutNote);
        model.addAttribute("workoutFrequency",workoutFrequency);

        return "tracking";
    }

    @PostMapping("/tracking/update")
    public String updateTracking(
            @RequestParam Double weightKg,
            @RequestParam Double heightCm,
            @RequestParam String birthDate,
            @RequestParam String gender,
            @RequestParam String goal,
            @RequestParam(required = false, defaultValue = "1.375") Double activityLevel) {

        User user = getAuthenticatedUser();
        user.setWeightKg(weightKg);
        user.setHeightCm(heightCm);
        user.setBirthDate(LocalDate.parse(birthDate));
        user.setGender(gender);
        user.setGoal(goal);
        user.setActivityLevel(activityLevel);
        userService.saveUser(user);
        return "redirect:/tracking?success";
    }

    // ====================== PROFIL ======================

    @GetMapping("/profil")
    public String profil(Model model) {
        User user = getAuthenticatedUser();
        addUserToModel(model, user);
        model.addAttribute("profilePhoto", user.getProfilePhoto());

        double weight = user.getWeightKg()      != null ? user.getWeightKg()      : 70.0;
        double height = user.getHeightCm()      != null ? user.getHeightCm()      : 170.0;
        String gender = user.getGender()        != null ? user.getGender()        : "M";
        String goal   = user.getGoal()          != null ? user.getGoal()          : "MAINTAIN";
        double actLvl = user.getActivityLevel() != null ? user.getActivityLevel() : 1.375;

        int age = 22;
        if (user.getBirthDate() != null)
            age = Period.between(user.getBirthDate(), LocalDate.now()).getYears();

        model.addAttribute("userAge",     age);
        model.addAttribute("genderLabel", "F".equals(gender) ? "Feminin" : "Masculin");

        double bmiRaw = weight / Math.pow(height / 100.0, 2);
        model.addAttribute("bmi", String.format("%.1f", bmiRaw));

        String bmiCategory; int bmiMarkerPct;
        if (bmiRaw < 18.5) {
            bmiCategory   = "Subponderal";
            bmiMarkerPct  = 10;
        } else if (bmiRaw < 25.0) {
            bmiCategory   = "Normal (Greutate sănătoasă)";
            bmiMarkerPct  = (int) ((bmiRaw - 18.5) / 6.5 * 30 + 10);
        } else if (bmiRaw < 30.0) {
            bmiCategory   = "Supraponderal";
            bmiMarkerPct  = (int) ((bmiRaw - 25.0) / 5.0 * 20 + 40);
        } else {
            bmiCategory   = "Obezitate";
            bmiMarkerPct  = (int) Math.min(95, (bmiRaw - 30.0) / 10.0 * 25 + 60);
        }
        model.addAttribute("bmiCategory",  bmiCategory);
        model.addAttribute("bmiMarkerPct", bmiMarkerPct);

        double sexOffset = "F".equals(gender) ? -161 : 5;
        long bmr  = Math.round(10 * weight + 6.25 * height - 5.0 * age + sexOffset);
        long tdee = Math.round(bmr * actLvl);
        model.addAttribute("bmr",  bmr);
        model.addAttribute("tdee", tdee);

        long caloricTarget; String goalLabel;
        switch (goal) {
            case "LOSE_WEIGHT" -> { caloricTarget = tdee - 500; goalLabel = "Slăbire"; }
            case "GAIN_MUSCLE" -> { caloricTarget = tdee + 300; goalLabel = "Masă musculară"; }
            default            -> { caloricTarget = tdee;       goalLabel = "Menținere"; }
        }
        model.addAttribute("caloricTarget", caloricTarget);
        model.addAttribute("goalLabel",     goalLabel);
        model.addAttribute("proteinTarget", Math.round(weight * 2.0));
        model.addAttribute("carbTarget",    Math.round(caloricTarget * 0.45 / 4.0));
        model.addAttribute("fatTarget",     Math.round(caloricTarget * 0.25 / 9.0));

        return "profil";
    }

    @PostMapping("/profil/update")
    public String updateProfile(
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam Double weightKg,
            @RequestParam Double heightCm,
            @RequestParam String birthDate,
            @RequestParam String gender,
            @RequestParam String goal) {

        User user = getAuthenticatedUser();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setWeightKg(weightKg);
        user.setHeightCm(heightCm);
        user.setBirthDate(LocalDate.parse(birthDate));
        user.setGender(gender);
        user.setGoal(goal);
        userService.saveUser(user);
        return "redirect:/profil?success";
    }
   

    @PostMapping("/profil/upload-foto")
    @ResponseBody
    public ResponseEntity<Map<String, String>> uploadProfilePhoto(
            @RequestParam("photo") MultipartFile file) {
        try {
            String email = getAuthenticatedUser().getEmail();
            String url = userService.saveProfilePhoto(email, file);
            return ResponseEntity.ok(Map.of("url", url, "success", "true"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/profil/sterge-foto")
    @ResponseBody
    public ResponseEntity<Map<String, String>> deleteProfilePhoto() {
        try {
            String email = getAuthenticatedUser().getEmail();
            userService.deleteProfilePhoto(email);
            return ResponseEntity.ok(Map.of("success", "true"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ====================== UTILITARE PRIVATE ======================

    private boolean isLoggedIn() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getName());
    }

    private void addUserToModel(Model model) {
        addUserToModel(model, getAuthenticatedUser());
    }

    private void addUserToModel(Model model, User user) {
        model.addAttribute("user",         user);
        model.addAttribute("userInitials", getInitials(user));
    }

    private void addAnonymousToModel(Model model) {
        model.addAttribute("user",         null);
        model.addAttribute("userInitials", "?");
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getName())) {
            throw new IllegalStateException(
                    "Utilizator neautentificat – verifică SecurityConfig");
        }
        return userRepository.findByEmail(auth.getName());
    }

    private String getInitials(User user) {
        if (user == null || user.getFirstName() == null
                || user.getFirstName().isEmpty()) return "?";
        return String.valueOf(user.getFirstName().charAt(0)).toUpperCase();
    }
}