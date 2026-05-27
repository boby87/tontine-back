package cm.ftg.tontine.auth.controller

import cm.ftg.tontine.auth.dto.AuthSessionDto
import cm.ftg.tontine.auth.dto.IdentifierResponse
import cm.ftg.tontine.auth.dto.TokensDto
import cm.ftg.tontine.auth.dto.UserDto
import cm.ftg.tontine.auth.repository.UserRepository
import cm.ftg.tontine.auth.service.AuthService
import cm.ftg.tontine.common.enums.UserRole
import cm.ftg.tontine.common.exception.ApiException
import cm.ftg.tontine.security.JwtService
import com.fasterxml.jackson.databind.ObjectMapper
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

import java.time.Instant

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = AuthController)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    ObjectMapper mapper = new ObjectMapper().registerModule(
            new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())

    @SpringBean
    AuthService authService = Mock()

    // Dependances du JwtAuthenticationFilter pulled in par le slice — stubs vides
    @SpringBean
    JwtService jwtService = Mock()

    @SpringBean
    UserRepository userRepository = Mock()

    def "POST /auth/register : retourne 201 + message OTP"() {
        given:
        def body = mapper.writeValueAsString([
                firstName: 'Jean', lastName: 'Dupont',
                phone: '+237699000111', email: 'jd@example.com',
                password: 'SecretPass123!'])
        authService.register(_) >> new IdentifierResponse('+237699000111')

        expect:
        mockMvc.perform(post('/auth/register')
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath('$.data.identifier').value('+237699000111'))
                .andExpect(jsonPath('$.message').value('Verifiez votre OTP pour activer le compte'))
    }

    def "POST /auth/register : 422 quand les champs sont invalides"() {
        given:
        def body = mapper.writeValueAsString([
                firstName: '', lastName: '',
                phone: 'invalid', email: 'pas-un-email',
                password: 'short'])

        expect:
        mockMvc.perform(post('/auth/register')
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath('$.code').value('VALIDATION_FAILED'))
                .andExpect(jsonPath('$.details').exists())
    }

    def "POST /auth/login : retourne 200 + AuthSessionDto"() {
        given:
        def body = mapper.writeValueAsString([
                identifier: '+237699000111', password: 'SecretPass123!'])
        def user = new UserDto(UUID.randomUUID(), 'Jean', 'Dupont', 'jd@example.com',
                '+237699000111', null, [UserRole.MEMBER] as Set, true, true, false,
                Instant.now(), Instant.now())
        authService.login(_) >> new AuthSessionDto(user,
                new TokensDto('access-token', 'refresh-token', 900L), null)

        expect:
        mockMvc.perform(post('/auth/login')
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.data.user.email').value('jd@example.com'))
                .andExpect(jsonPath('$.data.tokens.accessToken').value('access-token'))
                .andExpect(jsonPath('$.data.tokens.expiresIn').value(900))
    }

    def "POST /auth/login : 401 quand le service leve BadCredentials"() {
        given:
        def body = mapper.writeValueAsString([
                identifier: 'jd@example.com', password: 'wrong'])
        authService.login(_) >> {
            throw new org.springframework.security.authentication.BadCredentialsException('bad')
        }

        expect:
        mockMvc.perform(post('/auth/login')
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath('$.code').value('AUTH_INVALID_CREDENTIALS'))
    }

    def "POST /auth/login : 422 quand identifiant manque"() {
        given:
        def body = mapper.writeValueAsString([identifier: '', password: 'x'])

        expect:
        mockMvc.perform(post('/auth/login')
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isUnprocessableEntity())
    }

    def "POST /auth/verify-otp : 422 quand le code n'est pas 6 chiffres"() {
        given:
        def body = mapper.writeValueAsString([
                identifier: '+237699000111', code: 'abcd'])

        expect:
        mockMvc.perform(post('/auth/verify-otp')
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isUnprocessableEntity())
    }

    def "POST /auth/verify-otp : propage les exceptions ApiException"() {
        given:
        def body = mapper.writeValueAsString([
                identifier: '+237699000111', code: '123456'])
        authService.verifyOtp(_) >> {
            throw new ApiException('AUTH_OTP_INVALID', 'Code incorrect', HttpStatus.UNAUTHORIZED)
        }

        expect:
        mockMvc.perform(post('/auth/verify-otp')
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath('$.code').value('AUTH_OTP_INVALID'))
                .andExpect(jsonPath('$.message').value('Code incorrect'))
    }

    def "POST /auth/reset-password : retourne 204"() {
        given:
        def body = mapper.writeValueAsString([
                identifier: '+237699000111', code: '123456', newPassword: 'NewSecret123!'])

        expect:
        mockMvc.perform(post('/auth/reset-password')
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isNoContent())
    }

    def "POST /auth/refresh : retourne nouvelles tokens"() {
        given:
        def body = mapper.writeValueAsString([refreshToken: 'rt-xyz'])
        def user = new UserDto(UUID.randomUUID(), 'A', 'B', 'a@b.com', '+1',
                null, [] as Set, true, true, false, Instant.now(), Instant.now())
        authService.refresh(_) >> new AuthSessionDto(user,
                new TokensDto('new-access', 'rt-xyz', 900L), null)

        expect:
        mockMvc.perform(post('/auth/refresh')
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.data.tokens.accessToken').value('new-access'))
    }

}
