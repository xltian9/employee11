package com.limiti.employee.service;

//import com.limiti.employee.dto.PerformanceInfoDTO;
import com.limiti.employee.entity.Performance;
import com.limiti.employee.repository.PerformanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class PerformanceService {

    @Autowired
    private PerformanceRepository performanceRepository;

    public Performance createPerformance(Performance performance) {
        return performanceRepository.save(performance);
    }

    public Performance updatePerformance(Performance performance) {
        return performanceRepository.save(performance);
    }

    public List<Performance> getAllPerformances() {
        return performanceRepository.findAll();
    }

    public Performance getPerformanceById(Long id) {
        return performanceRepository.findById(id).orElse(null);
    }

//    public List<PerformanceInfoDTO> list(Map<String, Object> params) {
//        List<PerformanceInfoDTO> list = performanceRepository.list(params);
//        return list;
//    }

    
}