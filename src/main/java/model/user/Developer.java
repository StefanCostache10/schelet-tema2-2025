package model.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import model.enums.Expertise;
import model.enums.Seniority;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Developer extends User {
    private String hireDate;
    private Expertise expertiseArea;
    private Seniority seniority;
    private double performanceScore = 0.0; // Câmp nou necesar pentru Search

    public Developer() {
        super();
    }

    // Getters
    public String getHireDate() { return hireDate; }
    public Expertise getExpertiseArea() { return expertiseArea; }
    public Seniority getSeniority() { return seniority; }
    public double getPerformanceScore() { return performanceScore; }

    // Setters
    public void setHireDate(String hireDate) { this.hireDate = hireDate; }
    public void setExpertiseArea(Expertise expertiseArea) { this.expertiseArea = expertiseArea; }
    public void setSeniority(Seniority seniority) { this.seniority = seniority; }
    public void setPerformanceScore(double performanceScore) { this.performanceScore = performanceScore; }
}