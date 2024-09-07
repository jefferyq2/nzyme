package app.nzyme.core.context.db;

import com.google.auto.value.AutoValue;
import jakarta.annotation.Nullable;
import org.joda.time.DateTime;

import java.util.UUID;

@AutoValue
public abstract class MacAddressContextEntry {

    public abstract long id();
    public abstract UUID uuid();
    public abstract String macAddress();
    @Nullable
    public abstract String name();
    public abstract String description();
    @Nullable
    public abstract String notes();
    public abstract UUID organizationId();
    public abstract UUID tenantId();
    public abstract DateTime createdAt();
    public abstract DateTime updatedAt();

    public static MacAddressContextEntry create(long id, UUID uuid, String macAddress, String name, String description, String notes, UUID organizationId, UUID tenantId, DateTime createdAt, DateTime updatedAt) {
        return builder()
                .id(id)
                .uuid(uuid)
                .macAddress(macAddress)
                .name(name)
                .description(description)
                .notes(notes)
                .organizationId(organizationId)
                .tenantId(tenantId)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    public static Builder builder() {
        return new AutoValue_MacAddressContextEntry.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder id(long id);

        public abstract Builder uuid(UUID uuid);

        public abstract Builder macAddress(String macAddress);


        public abstract Builder name(String name);

        public abstract Builder description(String description);

        public abstract Builder notes(String notes);

        public abstract Builder organizationId(UUID organizationId);

        public abstract Builder tenantId(UUID tenantId);

        public abstract Builder createdAt(DateTime createdAt);

        public abstract Builder updatedAt(DateTime updatedAt);

        public abstract MacAddressContextEntry build();
    }
}
