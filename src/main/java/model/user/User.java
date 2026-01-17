package model.user;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import model.enums.Role;
import pattern.observer.Observer;

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

    protected List<String> notifications = new ArrayList<>();

    public User() {
    }

    public User(final String username, final String email, final Role role) {
        this.username = username;
        this.email = email;
        this.role = role;
    }

    @Override
    public final void update(final String message) {
        this.notifications.add(message);
    }

    public final List<String> getNotifications() {
        return new ArrayList<>(notifications);
    }

    /**
     * Clears the list of notifications for this user.
     */
    public final void clearNotifications() {
        this.notifications.clear();
    }

    public final String getUsername() {
        return username;
    }

    public final String getEmail() {
        return email;
    }

    public final Role getRole() {
        return role;
    }

    public final void setUsername(final String username) {
        this.username = username;
    }

    public final void setEmail(final String email) {
        this.email = email;
    }

    public final void setRole(final Role role) {
        this.role = role;
    }
}
