package co.com.bancolombia.model.domains.status;

public enum StatusCode {
    PENDING(1L, "PENDIENTE"),
    APPROVED(2L, "APROBADO"),
    REJECTED(3L, "RECHAZADO"),
    CANCELED(4L, "CANCELADO"),
    IN_PROCESS(5L, "EN_PROCESO");

    private final long id;
    private final String dbName;

    StatusCode(long id, String dbName) {
        this.id = id;
        this.dbName = dbName;
    }

    public long id() {
        return id;
    }

    public String dbName() {
        return dbName;
    }

    public static StatusCode fromId(long id) {
        for (StatusCode s : values()) {
            if (s.id == id) return s;
        }
        throw new IllegalArgumentException("Unknown status id: " + id);
    }

    public static StatusCode fromDbName(String name) {
        if (name == null) throw new IllegalArgumentException("name is null");
        for (StatusCode s : values()) {
            if (s.dbName.equalsIgnoreCase(name)) return s;
        }
        throw new IllegalArgumentException("Unknown status name: " + name);
    }
}