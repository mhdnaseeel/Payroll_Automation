package com.fci.automation.dto;

public class PayrollPeriodRequest {
    private Integer month;
    private Integer year;
    private String lastWorkingDay; // Accept as String to handle parsing manually

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getLastWorkingDay() {
        return lastWorkingDay;
    }

    public void setLastWorkingDay(String lastWorkingDay) {
        this.lastWorkingDay = lastWorkingDay;
    }
}
