package model.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import model.enums.Expertise;
import model.enums.Seniority;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class Developer extends User {
    private String hireDate;
    private Expertise expertiseArea;
    private Seniority seniority;
    private double performanceScore = 0.0;

    public Developer() {
        super();
    }

    public String getHireDate() {
        return hireDate;
    }

    public Expertise getExpertiseArea() {
        return expertiseArea;
    }

    public Seniority getSeniority() {
        return seniority;
    }

    public double getPerformanceScore() {
        return performanceScore;
    }

    public void setHireDate(final String hireDate) {
        this.hireDate = hireDate;
    }

    public void setExpertiseArea(final Expertise expertiseArea) {
        this.expertiseArea = expertiseArea;
    }

    public void setSeniority(final Seniority seniority) {
        this.seniority = seniority;
    }

    public void setPerformanceScore(final double performanceScore) {
        this.performanceScore = performanceScore;
    }
}
