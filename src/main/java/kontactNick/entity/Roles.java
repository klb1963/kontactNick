package kontactNick.entity;

public enum Roles {
    USER, // Обычный пользователь
    ADMIN, // Администратор
    MODERATOR; // Дополнительная роль, если нужна

    @Override
    public String toString() {
        return "ROLE_" + name(); // Префикс для Spring Security
    }
}
