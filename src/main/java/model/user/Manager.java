package model.user;

import java.util.List;

public class Manager extends User {
    private String hireDate;
    private List<String> subordinates;

    public Manager() { super(); }

    // Getters
    public String getHireDate() { return hireDate; }
    public List<String> getSubordinates() { return subordinates; }

    // Setters
    public void setHireDate(String hireDate) { this.hireDate = hireDate; }
    public void setSubordinates(List<String> subordinates) { this.subordinates = subordinates; }
}