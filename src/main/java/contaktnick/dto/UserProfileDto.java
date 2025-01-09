package contaktnick.dto;

import contaktnick.entity.Roles;

public class UserProfileDto {

    private String nick;
    private String email;
    private Roles role;

    // Конструктор
    public UserProfileDto(String nick, String email, Roles role) {
        this.nick = nick;
        this.email = email;
        this.role = role;
    }

    public String getNick() {
        return nick;
    }

    public String getEmail() {
        return email;
    }

    public Roles getRole() {
        return role;
    }
}
