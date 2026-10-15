package com.smartfit.config;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * Tratare globală a erorilor pentru panourile admin/angajat:
 * - SecurityException (permisiune lipsă)         → pagina 403
 * - IllegalArgumentException / IllegalStateException (validare business) → pagina 400
 *
 * Asigură "funcționarea fără erori" cerută: utilizatorul vede mereu
 * un mesaj prietenos, niciodată un stacktrace alb.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SecurityException.class)
    public ModelAndView handleAccessDenied(SecurityException ex) {
        ModelAndView mav = new ModelAndView("eroare-acces");
        mav.addObject("message", ex.getMessage());
        mav.setStatus(org.springframework.http.HttpStatus.FORBIDDEN);
        return mav;
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ModelAndView handleBadRequest(IllegalArgumentException ex) {
        ModelAndView mav = new ModelAndView("eroare-generala");
        mav.addObject("message", ex.getMessage());
        mav.setStatus(org.springframework.http.HttpStatus.BAD_REQUEST);
        return mav;
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleGeneral(Exception ex) {
        ModelAndView mav = new ModelAndView("eroare-generala");
        mav.addObject("message", "A apărut o eroare neașteptată: " + ex.getMessage());
        mav.setStatus(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        return mav;
    }
}
