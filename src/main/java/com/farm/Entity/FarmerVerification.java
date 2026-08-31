package com.farm.Entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
public class FarmerVerification implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    private Farmer farmer;

    private String fullName;

    // --- CHANGED FROM STRING TO ENTITY RELATIONSHIPS ---
    @ManyToOne
    @JoinColumn(name = "state_id")
    private State state;

    @ManyToOne
    @JoinColumn(name = "district_id")
    private District district;

    @ManyToOne
    @JoinColumn(name = "taluka_id")
    private Taluka taluka;
    // --------------------------------------------------

    private String village;
    private String pincode;

    @Column(unique = true, nullable = false)
    private String regNumber; // Survey No
    private Double farmArea;

    private String satBaraFile;
    private String aadhaarFile;

    private String status = "PENDING";
    private LocalDateTime submissionDate;

    @Column(length = 1000)
    private String adminRemark;

    public FarmerVerification() {
    }

    public FarmerVerification(Long id, Farmer farmer, String fullName, State state, District district, Taluka taluka, String village, String pincode, String regNumber, Double farmArea, String satBaraFile, String aadhaarFile, String status, LocalDateTime submissionDate, String adminRemark) {
        this.id = id;
        this.farmer = farmer;
        this.fullName = fullName;
        this.state = state;
        this.district = district;
        this.taluka = taluka;
        this.village = village;
        this.pincode = pincode;
        this.regNumber = regNumber;
        this.farmArea = farmArea;
        this.satBaraFile = satBaraFile;
        this.aadhaarFile = aadhaarFile;
        this.status = status;
        this.submissionDate = submissionDate;
        this.adminRemark = adminRemark;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Farmer getFarmer() {
        return farmer;
    }

    public void setFarmer(Farmer farmer) {
        this.farmer = farmer;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public District getDistrict() {
        return district;
    }

    public void setDistrict(District district) {
        this.district = district;
    }

    public Taluka getTaluka() {
        return taluka;
    }

    public void setTaluka(Taluka taluka) {
        this.taluka = taluka;
    }

    public String getVillage() {
        return village;
    }

    public void setVillage(String village) {
        this.village = village;
    }

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    public String getRegNumber() {
        return regNumber;
    }

    public void setRegNumber(String regNumber) {
        this.regNumber = regNumber;
    }

    public Double getFarmArea() {
        return farmArea;
    }

    public void setFarmArea(Double farmArea) {
        this.farmArea = farmArea;
    }

    public String getSatBaraFile() {
        return satBaraFile;
    }

    public void setSatBaraFile(String satBaraFile) {
        this.satBaraFile = satBaraFile;
    }

    public String getAadhaarFile() {
        return aadhaarFile;
    }

    public void setAadhaarFile(String aadhaarFile) {
        this.aadhaarFile = aadhaarFile;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getSubmissionDate() {
        return submissionDate;
    }

    public void setSubmissionDate(LocalDateTime submissionDate) {
        this.submissionDate = submissionDate;
    }

    public String getAdminRemark() {
        return adminRemark;
    }

    public void setAdminRemark(String adminRemark) {
        this.adminRemark = adminRemark;
    }
}