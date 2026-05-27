package cm.ftg.tontine

import spock.lang.Specification

class SanitySpec extends Specification {

    def "le harnais Spock fonctionne"() {
        expect:
        1 + 1 == 2
    }
}
