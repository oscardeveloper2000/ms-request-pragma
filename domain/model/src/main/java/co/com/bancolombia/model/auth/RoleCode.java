package co.com.bancolombia.model.auth;
//import lombok.NoArgsConstructor;


public enum RoleCode {
    ADMIN(1L, "ADMIN"),
    ADVISOR(2L, "ADVISOR"),
    CLIENT(3L, "CLIENT");

    private final long id;
    private final String dbName;

    RoleCode(long id, String dbName) {
        this.id = id;
        this.dbName = dbName;
    }

    public long id() {
        return id;
    }

    public String dbName() {
        return dbName;
    }

    public static RoleCode fromId(long id) {
        for (RoleCode role : RoleCode.values()) {
            if (role.id == id) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown role id: " + id);
    }

    public static RoleCode fromDbName(String dbName) {
        for (RoleCode role : RoleCode.values()) {
            if (role.dbName.equals(dbName)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown role name: " + dbName);
    }
}
