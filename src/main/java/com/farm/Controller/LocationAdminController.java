package com.farm.Controller;

import com.farm.Entity.*;
import com.farm.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/locations")
public class LocationAdminController {

    @Autowired private StateRepository stateRepo;
    @Autowired private DistrictRepository districtRepo;
    @Autowired private TalukaRepository talukaRepo;

    private void refreshLocationModel(Model model) {
        model.addAttribute("states", stateRepo.findAll());
        model.addAttribute("districts", districtRepo.findAll());
    }

    @GetMapping("/manage-fragment")
    public String getManageLocationFragment(Model model) {
        refreshLocationModel(model);
        return "admin/fragments/location-manage-fragment :: location-manage";
    }

    @PostMapping("/add-state")
    public String addState(@RequestParam String name, Model model) {
        String cleanName = name.trim();
        if (stateRepo.existsByNameIgnoreCase(cleanName)) {
            model.addAttribute("error", "State '" + cleanName + "' already exists!");
        } else {
            State state = new State();
            state.setName(cleanName);
            stateRepo.save(state);
            model.addAttribute("success", "State added!");
        }
        refreshLocationModel(model);
        return "admin/fragments/location-manage-fragment :: location-manage";
    }

    @PostMapping("/add-district")
    public String addDistrict(@RequestParam String name, @RequestParam Long stateId, Model model) {
        if (districtRepo.existsByNameIgnoreCaseAndStateId(name.trim(), stateId)) {
            model.addAttribute("error", "District exists in this state!");
        } else {
            State state = stateRepo.findById(stateId).orElseThrow();
            District district = new District();
            district.setName(name.trim());
            district.setState(state);
            districtRepo.save(district);
            model.addAttribute("success", "District added!");
        }
        refreshLocationModel(model);
        return "admin/fragments/location-manage-fragment :: location-manage";
    }

    @PostMapping("/add-taluka")
    public String addTaluka(@RequestParam String name, @RequestParam Long districtId, Model model) {
        if (talukaRepo.existsByNameIgnoreCaseAndDistrictId(name.trim(), districtId)) {
            model.addAttribute("error", "Taluka exists in this district!");
        } else {
            District district = districtRepo.findById(districtId).orElseThrow();
            Taluka taluka = new Taluka();
            taluka.setName(name.trim());
            taluka.setDistrict(district);
            talukaRepo.save(taluka);
            model.addAttribute("success", "Taluka added!");
        }
        refreshLocationModel(model);
        return "admin/fragments/location-manage-fragment :: location-manage";
    }

    @GetMapping("/delete-state/{id}")
    public String deleteState(@PathVariable Long id, Model model) {
        if (!stateRepo.findById(id).orElseThrow().getDistricts().isEmpty()) {
            model.addAttribute("error", "Delete Districts first!");
        } else {
            stateRepo.deleteById(id);
            model.addAttribute("success", "State deleted.");
        }
        refreshLocationModel(model);
        return "admin/fragments/location-manage-fragment :: location-manage";
    }

    @GetMapping("/delete-district/{id}")
    public String deleteDistrict(@PathVariable Long id, Model model) {
        if (!districtRepo.findById(id).orElseThrow().getTalukas().isEmpty()) {
            model.addAttribute("error", "Delete Talukas first!");
        } else {
            districtRepo.deleteById(id);
            model.addAttribute("success", "District deleted.");
        }
        refreshLocationModel(model);
        return "admin/fragments/location-manage-fragment :: location-manage";
    }

    @GetMapping("/delete-taluka/{id}")
    public String deleteTaluka(@PathVariable Long id, Model model) {
        talukaRepo.deleteById(id);
        model.addAttribute("success", "Taluka deleted.");
        refreshLocationModel(model);
        return "admin/fragments/location-manage-fragment :: location-manage";
    }
}