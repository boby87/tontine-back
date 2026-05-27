package cm.ftg.tontine.auth.service

import cm.ftg.tontine.auth.entity.OtpCode
import cm.ftg.tontine.auth.repository.OtpCodeRepository
import cm.ftg.tontine.common.enums.OtpPurpose
import cm.ftg.tontine.common.exception.ApiException
import cm.ftg.tontine.integration.messaging.email.EmailSender
import cm.ftg.tontine.integration.messaging.sms.SmsSender
import org.springframework.security.crypto.password.PasswordEncoder
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import java.time.Instant

class OtpServiceSpec extends Specification {

    OtpCodeRepository repository = Mock()
    PasswordEncoder passwordEncoder = Mock()
    SmsSender smsSender = Mock()
    EmailSender emailSender = Mock()

    @Subject
    OtpService service = new OtpService(repository, passwordEncoder, smsSender, emailSender, 300L)

    def "issue : route vers SMS quand l'identifiant est un telephone"() {
        given:
        passwordEncoder.encode(_ as String) >> 'hashed-code'

        when:
        service.issue('+237699000111', OtpPurpose.REGISTRATION)

        then:
        1 * repository.save(_ as OtpCode) >> { OtpCode otp ->
            assert otp.identifier == '+237699000111'
            assert otp.purpose == OtpPurpose.REGISTRATION
            assert otp.codeHash == 'hashed-code'
            assert otp.expiresAt.isAfter(Instant.now())
            return otp
        }
        1 * smsSender.send('+237699000111', { it.startsWith('Votre code de verification : ') })
        0 * emailSender.send(_, _, _)
    }

    def "issue : route vers Email quand l'identifiant contient un @"() {
        given:
        passwordEncoder.encode(_ as String) >> 'hashed'

        when:
        service.issue('user@example.com', OtpPurpose.PASSWORD_RESET)

        then:
        1 * repository.save(_ as OtpCode)
        1 * emailSender.send('user@example.com', 'Code de verification Tontine', _ as String)
        0 * smsSender.send(_, _)
    }

    def "issue : remonte une 503 quand l'envoi echoue"() {
        given:
        passwordEncoder.encode(_ as String) >> 'hashed'
        smsSender.send(_, _) >> { throw new RuntimeException('boom') }

        when:
        service.issue('+237699000111', OtpPurpose.REGISTRATION)

        then:
        ApiException ex = thrown()
        ex.code == 'OTP_DELIVERY_FAILED'
    }

    def "verifyAndConsume : reussit avec un code valide non consomme"() {
        given:
        def otp = new OtpCode().tap {
            identifier = '+237699000111'
            codeHash = 'hashed'
            purpose = OtpPurpose.REGISTRATION
            expiresAt = Instant.now().plusSeconds(60)
        }
        repository.findTopByIdentifierAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                '+237699000111', OtpPurpose.REGISTRATION) >> Optional.of(otp)
        passwordEncoder.matches('123456', 'hashed') >> true

        when:
        service.verifyAndConsume('+237699000111', '123456', OtpPurpose.REGISTRATION)

        then:
        1 * repository.save({ OtpCode it -> it.consumedAt != null })
    }

    def "verifyAndConsume : rejette un code expire"() {
        given:
        def otp = new OtpCode().tap {
            expiresAt = Instant.now().minusSeconds(60)
        }
        repository.findTopByIdentifierAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(_, _) >> Optional.of(otp)

        when:
        service.verifyAndConsume('+237699000111', '123456', OtpPurpose.REGISTRATION)

        then:
        ApiException ex = thrown()
        ex.code == 'AUTH_OTP_INVALID'
        ex.message.contains('expire')
    }

    def "verifyAndConsume : echoue avec OTP introuvable"() {
        given:
        repository.findTopByIdentifierAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(_, _) >> Optional.empty()

        when:
        service.verifyAndConsume('+237699000111', '123456', OtpPurpose.REGISTRATION)

        then:
        ApiException ex = thrown()
        ex.code == 'AUTH_OTP_INVALID'
        ex.message.contains('introuvable')
    }

    @Unroll
    def "verifyAndConsume : bloque apres #attempts tentatives"() {
        given:
        def otp = new OtpCode().tap {
            expiresAt = Instant.now().plusSeconds(60)
            it.attempts = attempts
        }
        repository.findTopByIdentifierAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(_, _) >> Optional.of(otp)

        when:
        service.verifyAndConsume('+237699000111', '123456', OtpPurpose.REGISTRATION)

        then:
        ApiException ex = thrown()
        ex.code == 'AUTH_OTP_INVALID'
        ex.message.contains('Trop de tentatives')

        where:
        attempts << [5, 6, 10]
    }

    def "verifyAndConsume : incremente attempts et rejette un mauvais code"() {
        given:
        def otp = new OtpCode().tap {
            expiresAt = Instant.now().plusSeconds(60)
            codeHash = 'hashed'
            it.attempts = 0
        }
        repository.findTopByIdentifierAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(_, _) >> Optional.of(otp)
        passwordEncoder.matches('000000', 'hashed') >> false

        when:
        service.verifyAndConsume('+237699000111', '000000', OtpPurpose.REGISTRATION)

        then:
        ApiException ex = thrown()
        ex.code == 'AUTH_OTP_INVALID'
        ex.message.contains('incorrect')
        1 * repository.save({ OtpCode it -> it.attempts == 1 && it.consumedAt == null })
    }
}
