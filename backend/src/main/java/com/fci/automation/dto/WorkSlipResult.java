package com.fci.automation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WorkSlipResult {

    @JsonProperty("work_slip_no")
    private String workSlipNo;

    @JsonProperty("issue")
    private String issue;

    @JsonProperty("date")
    private String date;

    @JsonProperty("bags")
    private Integer bags;

    public String getWorkSlipNo() {
        return workSlipNo;
    }

    public void setWorkSlipNo(String workSlipNo) {
        this.workSlipNo = workSlipNo;
    }

    public String getIssue() {
        return issue;
    }

    public void setIssue(String issue) {
        this.issue = issue;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Integer getBags() {
        return bags;
    }

    public void setBags(Integer bags) {
        this.bags = bags;
    }
}