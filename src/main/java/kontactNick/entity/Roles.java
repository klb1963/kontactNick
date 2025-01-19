package kontactNick.entity;

public enum Roles {
    ROLE_USER, ROLE_ADMIN;

    @Override
    public String toString() {
        return "ROLE_" + name(); // Префикс для Spring Security
    }
}
