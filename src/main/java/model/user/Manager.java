package model.user;

import java.util.List;

public final class Manager extends User {
    private String hireDate;
    private List<String> subordinates;

    public Manager() {
        super();
    }

    public String getHireDate() {
        return hireDate;
    }

    public List<String> getSubordinates() {
        return subordinates;
    }

    public void setHireDate(final String hireDate) {
        this.hireDate = hireDate;
    }

    public void setSubordinates(final List<String> subordinates) {
        this.subordinates = subordinates;
    }
}
