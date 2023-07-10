package app.nzyme.core.rest.responses.dot11;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.joda.time.DateTime;

@AutoValue
public abstract class SignalWaterfallTrackResponse {

    @JsonProperty
    public abstract DateTime start();

    @JsonProperty
    public abstract DateTime end();

    @JsonProperty("centerline")
    public abstract int centerline();

    @JsonProperty("min_signal")
    public abstract int minSignal();

    @JsonProperty("max_signal")
    public abstract int maxSignal();

    public static SignalWaterfallTrackResponse create(DateTime start, DateTime end, int centerline, int minSignal, int maxSignal) {
        return builder()
                .start(start)
                .end(end)
                .centerline(centerline)
                .minSignal(minSignal)
                .maxSignal(maxSignal)
                .build();
    }

    public static Builder builder() {
        return new AutoValue_SignalWaterfallTrackResponse.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder start(DateTime start);

        public abstract Builder end(DateTime end);

        public abstract Builder centerline(int centerline);

        public abstract Builder minSignal(int minSignal);

        public abstract Builder maxSignal(int maxSignal);

        public abstract SignalWaterfallTrackResponse build();
    }
}
