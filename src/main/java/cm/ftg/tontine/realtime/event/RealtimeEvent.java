package cm.ftg.tontine.realtime.event;

import java.time.Instant;

public record RealtimeEvent(String type, Object payload, Instant emittedAt) {

    public static RealtimeEvent of(String type, Object payload) {
        return new RealtimeEvent(type, payload, Instant.now());
    }
}
