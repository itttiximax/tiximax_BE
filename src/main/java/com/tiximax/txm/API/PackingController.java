package com.tiximax.txm.API;

import com.tiximax.txm.Entity.Packing;
import com.tiximax.txm.Model.PackingRequest;
import com.tiximax.txm.Model.WarehouseSummary;
import com.tiximax.txm.Service.PackingService;
import com.tiximax.txm.Service.WarehouseService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("/packings")
@SecurityRequirement(name = "bearerAuth")

public class PackingController {

}
