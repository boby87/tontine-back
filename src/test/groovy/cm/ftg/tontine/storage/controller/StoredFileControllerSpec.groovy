package cm.ftg.tontine.storage.controller

import cm.ftg.tontine.auth.repository.UserRepository
import cm.ftg.tontine.common.enums.UserRole
import cm.ftg.tontine.common.exception.ApiException
import cm.ftg.tontine.security.AuthenticatedUser
import cm.ftg.tontine.security.JwtService
import cm.ftg.tontine.storage.entity.StoredFile
import cm.ftg.tontine.storage.enums.StorageCategory
import cm.ftg.tontine.storage.service.StoredFileService
import cm.ftg.tontine.testutil.TestSecurityConfig
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

import java.time.Instant

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = StoredFileController)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig)
class StoredFileControllerSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @SpringBean
    StoredFileService service = Mock()

    @SpringBean
    JwtService jwtService = Mock()

    @SpringBean
    UserRepository userRepository = Mock()

    UUID userId = UUID.randomUUID()
    UUID tontineId = UUID.randomUUID()
    UUID fileId = UUID.randomUUID()
    AuthenticatedUser principal = new AuthenticatedUser(
            userId, 'u@x.com', '+237600000001', 'h',
            [UserRole.MEMBER] as Set, true)

    def setup() {
        SecurityContextHolder.setContext(new SecurityContextImpl(
                new UsernamePasswordAuthenticationToken(principal, null, principal.authorities)))
    }

    def cleanup() {
        SecurityContextHolder.clearContext()
    }

    StoredFile storedFile() {
        new StoredFile().tap {
            id = fileId
            it.tontineId = this.tontineId
            ownerUserId = userId
            category = StorageCategory.OTHER
            originalName = 'doc.pdf'
            contentType = 'application/pdf'
            sizeBytes = 1234L
            storageKey = 'OTHER/2026/05/27/uuid-doc.pdf'
            sha256 = '0' * 64
            createdAt = Instant.now()
        }
    }

    def "POST /files : upload multipart -> 200 + StoredFileDto"() {
        given:
        def filePart = new MockMultipartFile('file', 'doc.pdf',
                'application/pdf', 'CONTENT'.bytes)
        service.upload(_, tontineId, userId, StorageCategory.OTHER) >> storedFile()

        expect:
        mockMvc.perform(multipart('/files')
                .file(filePart)
                .param('category', 'OTHER')
                .param('tontineId', tontineId.toString())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.data.id').value(fileId.toString()))
                .andExpect(jsonPath('$.data.originalName').value('doc.pdf'))
                .andExpect(jsonPath('$.data.contentType').value('application/pdf'))
                .andExpect(jsonPath('$.data.sizeBytes').value(1234))
    }

    def "POST /files : remonte les 422 du service (FILE_TOO_LARGE)"() {
        given:
        def filePart = new MockMultipartFile('file', 'big.pdf',
                'application/pdf', 'X' * 100 as byte[])
        service.upload(_, _, _, _) >> {
            throw new ApiException('FILE_TOO_LARGE', 'Trop gros', HttpStatus.valueOf(422))
        }

        expect:
        mockMvc.perform(multipart('/files')
                .file(filePart)
                .param('category', 'OTHER')
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath('$.code').value('FILE_TOO_LARGE'))
    }

    def "GET /files/{id} : metadata serialise + ne fuite pas storageKey"() {
        given:
        service.getMetadata(fileId, userId) >> storedFile()

        expect:
        mockMvc.perform(get("/files/${fileId}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.data.id').value(fileId.toString()))
                .andExpect(jsonPath('$.data.sha256').exists())
    }

    def "GET /files/{id}/download : retourne le binaire avec Content-Disposition"() {
        given:
        def meta = storedFile().tap { originalName = 'rapport mensuel.pdf' }
        def resource = new ByteArrayResource('PDFDATA'.bytes)
        service.download(fileId, userId) >> new StoredFileService.DownloadResult(meta, resource)

        when:
        def result = mockMvc.perform(get("/files/${fileId}/download"))
                .andExpect(status().isOk())
                .andExpect(content().bytes('PDFDATA'.bytes))
                .andExpect(header().string('Content-Type', 'application/pdf'))
                .andReturn()

        then:
        def disposition = result.response.getHeader('Content-Disposition')
        disposition.startsWith("attachment; filename*=UTF-8''")
        disposition.contains('rapport%20mensuel.pdf')
    }

    def "DELETE /files/{id} : 200 avec data null + delegation au service"() {
        when:
        def result = mockMvc.perform(delete("/files/${fileId}"))

        then:
        result.andExpect(status().isOk())
        1 * service.delete(fileId, userId)
    }

    def "DELETE /files/{id} : 403 quand le service refuse"() {
        given:
        service.delete(fileId, userId) >> {
            throw new ApiException('FORBIDDEN', 'pas proprietaire', HttpStatus.FORBIDDEN)
        }

        expect:
        mockMvc.perform(delete("/files/${fileId}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath('$.code').value('FORBIDDEN'))
    }
}
