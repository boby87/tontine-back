package cm.ftg.tontine.notification.controller

import cm.ftg.tontine.auth.repository.UserRepository
import cm.ftg.tontine.common.enums.UserRole
import cm.ftg.tontine.notification.entity.AppNotification
import cm.ftg.tontine.notification.enums.NotificationCategory
import cm.ftg.tontine.notification.enums.NotificationKind
import cm.ftg.tontine.notification.service.NotificationService
import cm.ftg.tontine.security.AuthenticatedUser
import cm.ftg.tontine.security.JwtService
import cm.ftg.tontine.testutil.TestSecurityConfig
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

import java.time.Instant

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = NotificationController)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig)
class NotificationControllerSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @SpringBean
    NotificationService service = Mock()

    @SpringBean
    JwtService jwtService = Mock()

    @SpringBean
    UserRepository userRepository = Mock()

    UUID userId = UUID.randomUUID()
    UUID tontineId = UUID.randomUUID()
    AuthenticatedUser principal = new AuthenticatedUser(
            userId, 'jd@example.com', '+237699000111', 'h',
            [UserRole.MEMBER] as Set, true)

    def setup() {
        SecurityContextHolder.setContext(new SecurityContextImpl(
                new UsernamePasswordAuthenticationToken(principal, null, principal.authorities)))
    }

    def cleanup() {
        SecurityContextHolder.clearContext()
    }

    AppNotification notif(String title) {
        new AppNotification().tap {
            id = UUID.randomUUID()
            it.userId = this.userId
            it.tontineId = this.tontineId
            kind = NotificationKind.INFO
            category = NotificationCategory.GENERAL
            it.title = title
            message = "msg ${title}"
            link = "/x"
            createdAt = Instant.now()
        }
    }

    def "GET /notifications : pagine et serialise le contenu"() {
        given:
        def pageContent = [notif('A'), notif('B'), notif('C')]
        service.list(userId, null, null, _ as Pageable) >> new PageImpl<>(pageContent)

        expect:
        mockMvc.perform(get('/notifications'))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.data').isArray())
                .andExpect(jsonPath('$.data[0].title').value('A'))
                .andExpect(jsonPath('$.data[2].title').value('C'))
                .andExpect(jsonPath('$.meta').exists())
    }

    def "GET /notifications : propage filtres unreadOnly + tontineId"() {
        given:
        service.list(userId, tontineId, true, _ as Pageable) >> new PageImpl<>([notif('X')])

        expect:
        mockMvc.perform(get('/notifications')
                .header('X-Tontine-Id', tontineId.toString())
                .param('unreadOnly', 'true')
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.data[0].title').value('X'))
    }

    def "GET /notifications/unread-count : retourne le compteur"() {
        given:
        service.countUnread(userId) >> 7L

        expect:
        mockMvc.perform(get('/notifications/unread-count'))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.data').value(7))
    }

    def "POST /notifications/{id}/read : marque comme lu et renvoie le DTO"() {
        given:
        UUID nid = UUID.randomUUID()
        def n = notif('Y').tap { id = nid; read = true; readAt = Instant.now() }
        service.markRead(nid, userId) >> n

        expect:
        mockMvc.perform(post("/notifications/${nid}/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.data.id').value(nid.toString()))
                .andExpect(jsonPath('$.data.read').value(true))
    }

    def "POST /notifications/read-all : retourne le nombre marque"() {
        given:
        service.markAllRead(userId) >> 12

        expect:
        mockMvc.perform(post('/notifications/read-all'))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.data.markedCount').value(12))
    }

    def "DELETE /notifications/{id} : retourne 204"() {
        given:
        UUID nid = UUID.randomUUID()

        when:
        def result = mockMvc.perform(delete("/notifications/${nid}"))

        then:
        result.andExpect(status().isNoContent())
        1 * service.delete(nid, userId)
    }
}
