package com.smartfit.service;

import com.smartfit.dto.CourseFormDto;
import com.smartfit.entity.Course;
import com.smartfit.entity.QuizQuestion;
import com.smartfit.entity.User;
import com.smartfit.repository.CourseRepository;
import com.smartfit.repository.QuizQuestionRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CourseAdminService - Teste CRUD cursuri")
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private QuizQuestionRepository quizQuestionRepository;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private CourseAdminService courseService;

    private Course curs;
    private User admin;
    private CourseFormDto formDto;

    @BeforeEach
    void setUp() {
        admin = new User();
        admin.setId(1L);
        admin.setEmail("admin@smartfit.ro");
        admin.setRole("ADMIN");

        curs = new Course();
        curs.setId(1L);
        curs.setTitle("Nutritie fundamentala");
        curs.setDescription("Curs despre nutritie de baza");
        curs.setContent("Continutul cursului...");
        curs.setRewardPoints(120);
        curs.setPassScorePercentage(60);
        curs.setDifficulty("INTERMEDIAR");
        curs.setEmoji("🥗");
        curs.setBgColor("#EAF3FF");

        // CourseFormDto este o clasă obișnuită cu setter-e
        formDto = new CourseFormDto();
        formDto.setId(1L);
        formDto.setTitle("Nutritie fundamentala");
        formDto.setDescription("Curs despre nutritie de baza");
        formDto.setContent("Continutul cursului...");
        formDto.setVideoUrl(null);
        formDto.setRewardPoints(120);
        formDto.setPassScorePercentage(60);
        formDto.setDifficulty("INTERMEDIAR");
        formDto.setEmoji("🥗");
        formDto.setBgColor("#EAF3FF");
        formDto.setQuestions(null);
    }

    // -----------------------------------------------------------------------
    // Teste CRUD - Metode existente in CourseAdminService
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("findAll returneaza toate cursurile")
    void findAll_returneazaToateCursurile() {
        List<Course> cursuri = List.of(curs, new Course());
        when(courseRepository.findAll()).thenReturn(cursuri);

        List<Course> rezultat = courseService.findAll();

        assertEquals(2, rezultat.size());
        verify(courseRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("findById returneaza cursul daca exista")
    void findById_cursExistent_returneazaCurs() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(curs));

        Course rezultat = courseService.findById(1L);

        assertNotNull(rezultat);
        assertEquals("Nutritie fundamentala", rezultat.getTitle());
    }

    @Test
    @DisplayName("findById arunca exceptie daca cursul nu exista")
    void findById_cursInexistent_aruncaExceptie() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> courseService.findById(999L));
    }

    @Test
    @DisplayName("create salveaza cursul in baza de date")
    void create_salveazaCursul() {
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        Course rezultat = courseService.create(formDto, admin, request);

        assertNotNull(rezultat);
        assertEquals("Nutritie fundamentala", rezultat.getTitle());
        verify(courseRepository, times(1)).save(any(Course.class));
    }

    @Test
    @DisplayName("update modifica cursul existent")
    void update_modificaCursulExistent() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(curs));
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        CourseFormDto updatedDto = new CourseFormDto();
        updatedDto.setId(1L);
        updatedDto.setTitle("Nutritie avansata");
        updatedDto.setDescription("Curs despre nutritie avansata");
        updatedDto.setContent("Continut actualizat...");
        updatedDto.setVideoUrl(null);
        updatedDto.setRewardPoints(150);
        updatedDto.setPassScorePercentage(70);
        updatedDto.setDifficulty("AVANSAT");
        updatedDto.setEmoji("🔥");
        updatedDto.setBgColor("#FFE4E1");

        Course rezultat = courseService.update(1L, updatedDto, admin, request);

        assertEquals("Nutritie avansata", rezultat.getTitle());
        verify(courseRepository, times(1)).save(any(Course.class));
    }

    @Test
    @DisplayName("delete sterge cursul din baza de date")
    void delete_stergeCursul() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(curs));
        doNothing().when(courseRepository).deleteById(1L);
        doNothing().when(quizQuestionRepository).deleteByCourseId(1L);

        courseService.delete(1L, admin, request);

        verify(courseRepository, times(1)).deleteById(1L);
        verify(quizQuestionRepository, times(1)).deleteByCourseId(1L);
    }

    @Test
    @DisplayName("search filtreaza cursurile dupa criterii")
    void search_filtreazaCursurile() {
        List<Course> cursuriFiltrate = List.of(curs);
        when(courseRepository.search(any(), any(), any(), any())).thenReturn(cursuriFiltrate);

        List<Course> rezultat = courseService.search("Nutritie", "INTERMEDIAR", 0, 200);

        assertEquals(1, rezultat.size());
        verify(courseRepository, times(1)).search(any(), any(), any(), any());
    }

    @Test
    @DisplayName("findAllDifficulties returneaza toate dificultatile")
    void findAllDifficulties_returneazaDificultatile() {
        List<String> dificultati = List.of("BEGINNER", "INTERMEDIAR", "AVANSAT");
        when(courseRepository.findAllDifficulties()).thenReturn(dificultati);

        List<String> rezultat = courseService.findAllDifficulties();

        assertEquals(3, rezultat.size());
        verify(courseRepository, times(1)).findAllDifficulties();
    }

    @Test
    @DisplayName("toFormDto converteste cursul in DTO cu intrebari")
    void toFormDto_convertesteCursulInDto() {
        when(quizQuestionRepository.findByCourseId(1L)).thenReturn(Collections.emptyList());

        CourseFormDto rezultat = courseService.toFormDto(curs);

        assertNotNull(rezultat);
        assertEquals("Nutritie fundamentala", rezultat.getTitle());
        assertEquals("INTERMEDIAR", rezultat.getDifficulty());
    }

    // -----------------------------------------------------------------------
    // Teste pentru QuizQuestionRepository - Metode existente
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("create salveaza si intrebarile quiz-ului")
    void create_salveazaSiIntrebarile() {
        // Cream o intrebare DTO folosind constructorul implicit
        CourseFormDto.QuizQuestionFormDto qDto = new CourseFormDto.QuizQuestionFormDto();
        qDto.setQuestionText("Ce este proteina?");
        qDto.setCorrectAnswer("Un nutrient");
        qDto.setWrongAnswer1("Un carbohidrat");
        qDto.setWrongAnswer2("O grasime");
        qDto.setWrongAnswer3("O vitamina");
        List<CourseFormDto.QuizQuestionFormDto> questions = List.of(qDto);

        CourseFormDto dtoCuIntrebari = new CourseFormDto();
        dtoCuIntrebari.setId(1L);
        dtoCuIntrebari.setTitle("Nutritie fundamentala");
        dtoCuIntrebari.setDescription("Descriere");
        dtoCuIntrebari.setContent("Continut");
        dtoCuIntrebari.setVideoUrl(null);
        dtoCuIntrebari.setRewardPoints(120);
        dtoCuIntrebari.setPassScorePercentage(60);
        dtoCuIntrebari.setDifficulty("INTERMEDIAR");
        dtoCuIntrebari.setEmoji("🥗");
        dtoCuIntrebari.setBgColor("#EAF3FF");
        dtoCuIntrebari.setQuestions(questions);

        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));
        when(quizQuestionRepository.save(any(QuizQuestion.class))).thenAnswer(inv -> inv.getArgument(0));

        courseService.create(dtoCuIntrebari, admin, request);

        verify(quizQuestionRepository, times(1)).save(any(QuizQuestion.class));
    }

    @Test
    @DisplayName("update sterge intrebarile vechi si le inlocuieste cu cele noi")
    void update_inlocuiesteIntrebarile() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(curs));
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(quizQuestionRepository).deleteByCourseId(1L);

        courseService.update(1L, formDto, admin, request);

        verify(quizQuestionRepository, times(1)).deleteByCourseId(1L);
    }
}