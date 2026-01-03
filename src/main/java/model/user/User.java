package model.user;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import model.enums.Role;
import pattern.observer.Observer;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "role",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = Reporter.class, name = "REPORTER"),
        @JsonSubTypes.Type(value = Developer.class, name = "DEVELOPER"),
        @JsonSubTypes.Type(value = Manager.class, name = "MANAGER")
})
public abstract class User implements Observer {
    private String username;
    private String email;
    private Role role;

    public User() {}

    public User(String username, String email, Role role) {
        this.username = username;
        this.email = email;
        this.role = role;
    }

    @JsonIgnore
    private List<String> notifications = new ArrayList<>();

    @Override
    public void update(String message) {
        notifications.add(message);
    }

    public List<String> getNotifications() { return notifications; }
    public void clearNotifications() { notifications.clear(); }

    // Getters
    public String getUsername() { return username; }
    public String getEmail() { return email; } // Asta lipsea!
    public Role getRole() { return role; }

    // Setters - Jackson are nevoie de ei!
    public void setUsername(String username) { this.username = username; }
    public void setEmail(String email) { this.email = email; } // Asta lipsea!
    public void setRole(Role role) { this.role = role; }
}