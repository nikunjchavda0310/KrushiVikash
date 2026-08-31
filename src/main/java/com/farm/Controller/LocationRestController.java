package com.farm.Controller;

import com.farm.Entity.District;
import com.farm.Entity.Taluka;
import com.farm.Repository.DistrictRepository;
import com.farm.Repository.TalukaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
public class LocationRestController {

    @Autowired
    private DistrictRepository districtRepo;
    @Autowired
    private TalukaRepository talukaRepo;

    @GetMapping("/districts/{stateId}")
    public List<District> getDistricts(@PathVariable Long stateId) {
        return districtRepo.findByStateId(stateId);
    }

    @GetMapping("/talukas/{districtId}")
    public List<Taluka> getTalukas(@PathVariable Long districtId) {
        return talukaRepo.findByDistrictId(districtId);
    }
}
