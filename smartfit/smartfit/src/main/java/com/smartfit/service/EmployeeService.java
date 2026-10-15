package com.smartfit.service;

import com.smartfit.dto.EmployeeFormDto;
import com.smartfit.entity.EmployeeProfile;
import com.smartfit.entity.User;
import com.smartfit.repository.EmployeeProfileRepository;
import com.smartfit.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Gestionează angajații (creare/editare/ștergere) — exclusiv pentru ADMIN.
 * Un angajat = un rând în `users` (role=EMPLOYEE) + un rând în `employee_profiles`.
 */
@Service
public class EmployeeService {

    private final UserRepository userRepository;
    private final EmployeeProfileRepository employeeProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public EmployeeService(UserRepository userRepository,
                            EmployeeProfileRepository employeeProfileRepository,
                            PasswordEncoder passwordEncoder,
                            AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.employeeProfileRepository = employeeProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    public List<EmployeeProfile> findAll() {
        return employeeProfileRepository.searchByDepartment(null);
    }

    public List<EmployeeProfile> findByDepartment(String department) {
        return employeeProfileRepository.searchByDepartment(department);
    }

    public EmployeeProfile findById(Long employeeProfileId) {
        return employeeProfileRepository.findById(employeeProfileId)
                .orElseThrow(() -> new IllegalArgumentException("Angajatul nu a fost găsit"));
    }

    public EmployeeFormDto toFormDto(EmployeeProfile ep) {
        EmployeeFormDto dto = new EmployeeFormDto();
        dto.setUserId(ep.getUser().getId());
        dto.setEmail(ep.getUser().getEmail());
        dto.setFirstName(ep.getUser().getFirstName());
        dto.setLastName(ep.getUser().getLastName());
        dto.setDepartment(ep.getDepartment());
        dto.setCanCreate(ep.getCanCreate());
        dto.setCanEdit(ep.getCanEdit());
        dto.setCanDelete(ep.getCanDelete());
        dto.setHiredDate(ep.getHiredDate() != null ? ep.getHiredDate().toString() : null);
        dto.setNotes(ep.getNotes());
        return dto;
    }

    // ── CREARE ANGAJAT NOU ─────────────────────────────────────
    @Transactional
    public EmployeeProfile create(EmployeeFormDto dto, User actor, HttpServletRequest request) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Există deja un cont cu emailul " + dto.getEmail());
        }
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new IllegalArgumentException("Parola este obligatorie la crearea unui angajat nou");
        }
        if (!"ACADEMY".equals(dto.getDepartment()) && !"SHOP".equals(dto.getDepartment())) {
            throw new IllegalArgumentException("Departament invalid. Valori permise: ACADEMY, SHOP");
        }

        User user = new User();
        user.setEmail(dto.getEmail().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setFirstName(dto.getFirstName().trim());
        user.setLastName(dto.getLastName().trim());
        user.setRole("EMPLOYEE");
        user.setGoal("MAINTAIN");
        user.setFitPoints(0);
        User savedUser = userRepository.save(user);

        EmployeeProfile ep = new EmployeeProfile();
        ep.setUser(savedUser);
        applyProfileFields(ep, dto);
        EmployeeProfile saved = employeeProfileRepository.save(ep);

        auditLogService.log(actor, "CREATE",
                "Angajat nou: " + savedUser.getFirstName() + " " + savedUser.getLastName()
                        + " (" + savedUser.getEmail() + ") — departament " + saved.getDepartment(),
                "employee_profiles", saved.getId(),
                "INSERT INTO users (email, role, ...) VALUES ('" + savedUser.getEmail() + "', 'EMPLOYEE', ...); "
                        + "INSERT INTO employee_profiles (user_id, department, ...) VALUES (" + savedUser.getId()
                        + ", '" + saved.getDepartment() + "', ...)",
                request);

        return saved;
    }

    // ── MODIFICARE ANGAJAT ─────────────────────────────────────
    @Transactional
    public EmployeeProfile update(Long employeeProfileId, EmployeeFormDto dto, User actor, HttpServletRequest request) {
        EmployeeProfile ep = findById(employeeProfileId);
        User user = ep.getUser();

        // Email duplicat (dacă s-a schimbat)
        if (!user.getEmail().equalsIgnoreCase(dto.getEmail()) && userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Există deja un cont cu emailul " + dto.getEmail());
        }

        user.setEmail(dto.getEmail().trim().toLowerCase());
        user.setFirstName(dto.getFirstName().trim());
        user.setLastName(dto.getLastName().trim());

        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        }
        userRepository.save(user);

        applyProfileFields(ep, dto);
        EmployeeProfile saved = employeeProfileRepository.save(ep);

        auditLogService.log(actor, "UPDATE",
                "Angajat modificat: " + user.getFirstName() + " " + user.getLastName()
                        + " — departament=" + saved.getDepartment()
                        + ", permisiuni(create=" + saved.getCanCreate()
                        + ", edit=" + saved.getCanEdit()
                        + ", delete=" + saved.getCanDelete() + ")",
                "employee_profiles", saved.getId(),
                "UPDATE employee_profiles SET department='" + saved.getDepartment()
                        + "', can_create=" + b(saved.getCanCreate())
                        + ", can_edit=" + b(saved.getCanEdit())
                        + ", can_delete=" + b(saved.getCanDelete())
                        + " WHERE id=" + saved.getId(),
                request);

        return saved;
    }

    // ── ȘTERGERE ANGAJAT ───────────────────────────────────────
    @Transactional
    public void delete(Long employeeProfileId, User actor, HttpServletRequest request) {
        EmployeeProfile ep = findById(employeeProfileId);
        User user = ep.getUser();
        String name = user.getFirstName() + " " + user.getLastName();
        String email = user.getEmail();
        Long userId = user.getId();

        employeeProfileRepository.delete(ep);
        userRepository.delete(user);

        auditLogService.log(actor, "DELETE",
                "Angajat șters: " + name + " (" + email + ")",
                "users", userId,
                "DELETE FROM employee_profiles WHERE user_id=" + userId + "; "
                        + "DELETE FROM users WHERE id=" + userId,
                request);
    }

    private void applyProfileFields(EmployeeProfile ep, EmployeeFormDto dto) {
        ep.setDepartment(dto.getDepartment());
        ep.setCanCreate(Boolean.TRUE.equals(dto.getCanCreate()));
        ep.setCanEdit(Boolean.TRUE.equals(dto.getCanEdit()));
        ep.setCanDelete(Boolean.TRUE.equals(dto.getCanDelete()));
        ep.setNotes(dto.getNotes());
        if (dto.getHiredDate() != null && !dto.getHiredDate().isBlank()) {
            ep.setHiredDate(LocalDate.parse(dto.getHiredDate()));
        }
    }

    private int b(Boolean v) {
        return Boolean.TRUE.equals(v) ? 1 : 0;
    }
}
