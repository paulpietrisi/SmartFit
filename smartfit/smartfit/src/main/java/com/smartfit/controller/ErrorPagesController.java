package com.smartfit.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Pagină simplă de "Acces interzis" — folosită de SecurityConfig
 * (.exceptionHandling().accessDeniedPage("/eroare/403")).
 */
@Controller
public class ErrorPagesController {

    @GetMapping("/eroare/403")
    public String accessDenied(Model model) {
        model.addAttribute("message", "Nu ai permisiunea necesară pentru a accesa această pagină.");
        return "eroare-acces";
    }
}
