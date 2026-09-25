package fr.squaregames.api.security;

public class JwtUserPrincipal {

    private final Long userId;
    private final String username;
    private final String role;

    public JwtUserPrincipal(
            Long userId,
            String username,
            String role
    ) {
        this.userId = userId;
        this.username = username;
        this.role = role;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }
}