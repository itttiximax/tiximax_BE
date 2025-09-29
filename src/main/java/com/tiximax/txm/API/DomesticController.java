//package com.tiximax.txm.API;
//
//import com.tiximax.txm.Entity.Domestic;
//import com.tiximax.txm.Model.CreateDomesticRequest;
//import com.tiximax.txm.Service.DomesticService;
//import io.swagger.v3.oas.annotations.security.SecurityRequirement;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Sort;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@CrossOrigin
//@RequestMapping("/domestics")
//@SecurityRequirement(name = "bearerAuth")
//
//public class DomesticController {
//
//    @Autowired
//    private DomesticService domesticService;
//
//    @GetMapping("/{page}/{size}")
//    public ResponseEntity<Page<Domestic>> getDomesticsByPackingStatus(@PathVariable int page, @PathVariable int size) {
//        Sort sort = Sort.by("timestamp").descending();
//        Pageable pageable = PageRequest.of(page, size, sort);
//        Page<Domestic> domesticsPage = domesticService.getDomesticsByPackingStatus(pageable);
//        return ResponseEntity.ok(domesticsPage);
//    }
//
////    @PostMapping
////    public ResponseEntity<List<Domestic>> createDomestic(@RequestBody CreateDomesticRequest request) {
////        List<Domestic> domestics = domesticService.createDomestic(request.getPackingIds(), request.getNote());
////        return ResponseEntity.ok(domestics);
////    }
//
//}
