package cm.ftg.tontine.storage.service

import cm.ftg.tontine.audit.service.AuditService
import cm.ftg.tontine.common.exception.ApiException
import cm.ftg.tontine.member.entity.Member
import cm.ftg.tontine.member.repository.MemberRepository
import cm.ftg.tontine.storage.config.StorageProperties
import cm.ftg.tontine.storage.entity.StoredFile
import cm.ftg.tontine.storage.enums.StorageCategory
import cm.ftg.tontine.storage.repository.StoredFileRepository
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.multipart.MultipartFile
import spock.lang.Specification
import spock.lang.Subject

class StoredFileServiceSpec extends Specification {

    StoredFileRepository repository = Mock()
    FileStorageBackend backend = Mock()
    StorageProperties properties = new StorageProperties().tap {
        maxFileSizeBytes = 1024 * 1024
    }
    MemberRepository memberRepository = Mock()
    AuditService auditService = Mock()

    @Subject
    StoredFileService service = new StoredFileService(
            repository, backend, properties, memberRepository, auditService)

    UUID tontineId = UUID.randomUUID()
    UUID ownerId = UUID.randomUUID()
    UUID otherUserId = UUID.randomUUID()

    private MultipartFile imageFile(String mime = 'image/jpeg', byte[] content = 'JPEGDATA'.bytes) {
        new MockMultipartFile('file', 'photo.jpg', mime, content)
    }

    def "upload : refuse un fichier vide"() {
        given:
        def file = new MockMultipartFile('file', 'x.jpg', 'image/jpeg', new byte[0])

        when:
        service.upload(file, tontineId, ownerId, StorageCategory.KYC_PHOTO)

        then:
        ApiException ex = thrown()
        ex.code == 'FILE_EMPTY'
    }

    def "upload : refuse un fichier au-dessus de la limite"() {
        given:
        properties.maxFileSizeBytes = 4
        def file = imageFile('image/jpeg', 'X' * 16 as byte[])
        memberRepository.findByUserIdAndTontineId(ownerId, tontineId) >> Optional.of(new Member())

        when:
        service.upload(file, tontineId, ownerId, StorageCategory.KYC_PHOTO)

        then:
        ApiException ex = thrown()
        ex.code == 'FILE_TOO_LARGE'
    }

    def "upload : refuse un MIME non autorise pour KYC_PHOTO"() {
        given:
        def file = new MockMultipartFile('file', 'doc.pdf', 'application/pdf', 'PDF'.bytes)
        memberRepository.findByUserIdAndTontineId(ownerId, tontineId) >> Optional.of(new Member())

        when:
        service.upload(file, tontineId, ownerId, StorageCategory.KYC_PHOTO)

        then:
        ApiException ex = thrown()
        ex.code == 'FILE_TYPE_NOT_ALLOWED'
    }

    def "upload : refuse un utilisateur non membre de la tontine"() {
        given:
        def file = imageFile()
        memberRepository.findByUserIdAndTontineId(ownerId, tontineId) >> Optional.empty()

        when:
        service.upload(file, tontineId, ownerId, StorageCategory.KYC_PHOTO)

        then:
        ApiException ex = thrown()
        ex.code == 'FORBIDDEN'
        0 * backend.save(_, _, _)
    }

    def "upload : ecrit via le backend et persiste les metadata + audit"() {
        given:
        def file = imageFile()
        memberRepository.findByUserIdAndTontineId(ownerId, tontineId) >> Optional.of(new Member())
        backend.save(_, _, _) >> { stream, key, contentType ->
            // consommer le stream pour que le SHA-256 soit calcule
            while (stream.read() != -1) {}
            return key
        }
        repository.save(_) >> { StoredFile s -> s.id = UUID.randomUUID(); s }

        when:
        StoredFile saved = service.upload(file, tontineId, ownerId, StorageCategory.KYC_PHOTO)

        then:
        saved.ownerUserId == ownerId
        saved.tontineId == tontineId
        saved.category == StorageCategory.KYC_PHOTO
        saved.originalName == 'photo.jpg'
        saved.sizeBytes == 'JPEGDATA'.bytes.length
        saved.sha256 ==~ /[0-9a-f]{64}/
        saved.storageKey.startsWith('KYC_PHOTO/')
        saved.storageKey.endsWith('-photo.jpg')
        1 * auditService.record(ownerId, 'FILE_UPLOAD', 'StoredFile', _, tontineId, _)
    }

    def "getMetadata : autorise le proprietaire"() {
        given:
        def f = storedFile()
        repository.findByIdAndDeletedAtIsNull(f.id) >> Optional.of(f)

        when:
        def result = service.getMetadata(f.id, ownerId)

        then:
        result == f
    }

    def "getMetadata : autorise un autre membre de la meme tontine"() {
        given:
        def f = storedFile()
        repository.findByIdAndDeletedAtIsNull(f.id) >> Optional.of(f)
        memberRepository.findByUserIdAndTontineId(otherUserId, tontineId) >> Optional.of(new Member())

        when:
        def result = service.getMetadata(f.id, otherUserId)

        then:
        result == f
    }

    def "getMetadata : refuse un tiers sans appartenance"() {
        given:
        def f = storedFile()
        repository.findByIdAndDeletedAtIsNull(f.id) >> Optional.of(f)
        memberRepository.findByUserIdAndTontineId(otherUserId, tontineId) >> Optional.empty()

        when:
        service.getMetadata(f.id, otherUserId)

        then:
        ApiException ex = thrown()
        ex.code == 'FORBIDDEN'
    }

    def "delete : refuse un non-proprietaire meme membre de la tontine"() {
        given:
        def f = storedFile()
        repository.findByIdAndDeletedAtIsNull(f.id) >> Optional.of(f)

        when:
        service.delete(f.id, otherUserId)

        then:
        ApiException ex = thrown()
        ex.code == 'FORBIDDEN'
        0 * backend.delete(_)
    }

    def "delete : marque le fichier supprime et supprime du backend"() {
        given:
        def f = storedFile()
        repository.findByIdAndDeletedAtIsNull(f.id) >> Optional.of(f)
        repository.save(_) >> { it[0] }

        when:
        service.delete(f.id, ownerId)

        then:
        f.deletedAt != null
        1 * backend.delete(f.storageKey)
        1 * auditService.record(ownerId, 'FILE_DELETE', 'StoredFile', f.id.toString(), tontineId, null)
    }

    private StoredFile storedFile() {
        new StoredFile().tap {
            id = UUID.randomUUID()
            it.ownerUserId = this.ownerId
            it.tontineId = this.tontineId
            category = StorageCategory.OTHER
            originalName = 'doc.pdf'
            contentType = 'application/pdf'
            sizeBytes = 100L
            storageKey = 'OTHER/2026/05/26/uuid-doc.pdf'
            sha256 = '0' * 64
        }
    }
}
