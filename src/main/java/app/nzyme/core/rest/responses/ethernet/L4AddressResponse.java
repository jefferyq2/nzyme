package app.nzyme.core.rest.responses.ethernet;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import jakarta.annotation.Nullable;

@AutoValue
public abstract class L4AddressResponse {

    @JsonProperty("l4_type")
    public abstract L4AddressTypeResponse l4Type();
    @JsonProperty("mac")
    public abstract String mac();

    @JsonProperty("address")
    public abstract String address();

    @JsonProperty("port")
    public abstract int port();

    @JsonProperty("geo")
    public abstract L4AddressGeoResponse geo();

    @Nullable
    @JsonProperty("attributes")
    public abstract L4AddressAttributesResponse attributes();

    @Nullable
    @JsonProperty("context")
    public abstract L4AddressContextResponse context();

    public static L4AddressResponse create(L4AddressTypeResponse l4Type, String mac, String address, int port, L4AddressGeoResponse geo, L4AddressAttributesResponse attributes, L4AddressContextResponse context) {
        return builder()
                .l4Type(l4Type)
                .mac(mac)
                .address(address)
                .port(port)
                .geo(geo)
                .attributes(attributes)
                .context(context)
                .build();
    }

    public static Builder builder() {
        return new AutoValue_L4AddressResponse.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder l4Type(L4AddressTypeResponse l4Type);

        public abstract Builder mac(String mac);

        public abstract Builder address(String address);

        public abstract Builder port(int port);

        public abstract Builder geo(L4AddressGeoResponse geo);

        public abstract Builder attributes(L4AddressAttributesResponse attributes);

        public abstract Builder context(L4AddressContextResponse context);

        public abstract L4AddressResponse build();
    }
}
