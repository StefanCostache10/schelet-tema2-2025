package model.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import model.enums.Expertise;
import model.enums.Seniority;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Developer extends User {
    private String hireDate;

    // MODIFICARE: Folosim Enum, nu List, deoarece în JSON e un singur string ("FRONTEND")
    private Expertise expertiseArea;

    // MODIFICARE: Folosim Enum Seniority
    private Seniority seniority;

    public Developer() {
        super();
    }

    // Getters
    public String getHireDate() { return hireDate; }
    public Expertise getExpertiseArea() { return expertiseArea; }
    public Seniority getSeniority() { return seniority; }

    // Setters
    public void setHireDate(String hireDate) { this.hireDate = hireDate; }
    public void setExpertiseArea(Expertise expertiseArea) { this.expertiseArea = expertiseArea; }
    public void setSeniority(Seniority seniority) { this.seniority = seniority; }
}