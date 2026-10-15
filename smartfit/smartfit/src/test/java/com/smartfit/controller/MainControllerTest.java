package com.smartfit.controller;

import com.smartfit.entity.User;
import com.smartfit.repository.UserRepository;
import com.smartfit.service.UserService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Teste de integrare pentru controllere HTTP (MainController, Login/Register).
 * <p>
 * Utilizează MockMvc pentru a simula cereri HTTP fără a porni serverul real.
 * Adnotarea {@code @SpringBootTest} încarcă contextul Spring complet (inclusiv SecurityConfig).
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("MainController - Teste de integrare HTTP")
class MainControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private UserRepository userRepository;

    private User userTest;

    @BeforeEach
    void setUp() {
        userTest = new User();
        userTest.setId(1L);
        userTest.setEmail("paul@smartfit.ro");
        userTest.setFirstName("Paul");
        userTest.setLastName("Pietrisi");
        userTest.setRole("USER");
        userTest.setFitPoints(500);
    }

    // -----------------------------------------------------------------------
    // Teste acces pagini publice (fără autentificare)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("GET /home returnează HTTP 200 pentru vizitator neautentificat")
    void getHome_farAuthentificare_returneaza200() throws Exception {
        mockMvc.perform(get("/home"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"));
    }

    @Test
    @DisplayName("GET /login returnează pagina de autentificare")
    void getLogin_returneazaPaginaLogin() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    @DisplayName("GET /register returnează pagina de înregistrare")
    void getRegister_returneazaPaginaRegister() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    // -----------------------------------------------------------------------
    // Teste acces pagini protejate (redirecționare la login)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("GET /dashboard fără autentificare redirecționează la /login")
    void getDashboard_farAuthentificare_redirectLaLogin() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("GET /admin/dashboard fără autentificare redirecționează la /login")
    void getAdminDashboard_farAuthentificare_redirectLaLogin() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("GET /admin/dashboard cu rol USER returnează HTTP 403 (Acces interzis)")
    @WithMockUser(username = "paul@smartfit.ro")  // roles default = USER, eliminat redundant
    void getAdminDashboard_rolUser_returneaza403() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    // -----------------------------------------------------------------------
    // Teste acces autentificat
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("GET /dashboard cu rol USER returnează HTTP 200")
    @WithMockUser(username = "paul@smartfit.ro")  // roles default = USER
    void getDashboard_cuRolUser_returneaza200() throws Exception {
        when(userRepository.findByEmail("paul@smartfit.ro")).thenReturn(userTest);

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"));
    }

    @Test
    @DisplayName("GET /admin/dashboard cu rol ADMIN returnează HTTP 200")
    @WithMockUser(username = "admin@smartfit.ro", roles = {"ADMIN"})
    void getAdminDashboard_cuRolAdmin_returneaza200() throws Exception {
        User admin = new User();
        admin.setId(2L);
        admin.setEmail("admin@smartfit.ro");
        admin.setRole("ADMIN");
        when(userRepository.findByEmail("admin@smartfit.ro")).thenReturn(admin);

        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-dashboard"));
    }

    // -----------------------------------------------------------------------
    // Teste formulare POST
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("POST /register cu date valide redirecționează la /login")
    void postRegister_dateValide_redirectLaLogin() throws Exception {
        // Nu există utilizator cu acest email
        when(userRepository.findByEmail("nou@smartfit.ro")).thenReturn(null);
        doNothing().when(userService).saveUser(any(User.class));

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("firstName", "Ion")
                        .param("lastName", "Popescu")
                        .param("email", "nou@smartfit.ro")
                        .param("password", "parolaSecreta123")
                        .param("gender", "M"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));
    }

    @Test
    @DisplayName("POST /register cu email duplicat returnează formularul cu eroare")
    void postRegister_emailDuplicat_returneazaFormularCuEroare() throws Exception {
        // Simulăm că există deja un utilizator cu acest email
        when(userRepository.findByEmail("paul@smartfit.ro")).thenReturn(userTest);

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("firstName", "Ion")
                        .param("lastName", "Popescu")
                        .param("email", "paul@smartfit.ro") // email existent
                        .param("password", "parolaSecreta123")
                        .param("gender", "M"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("emailError"));
    }
}