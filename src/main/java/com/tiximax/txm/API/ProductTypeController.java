package com.tiximax.txm.API;

import com.tiximax.txm.Entity.ProductType;
import com.tiximax.txm.Service.ProductTypeService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@CrossOrigin
@RequestMapping("/product-type")
@SecurityRequirement(name = "bearerAuth")

public class ProductTypeController {

    @Autowired
    private ProductTypeService productTypeService;

    @PostMapping
    public ResponseEntity<ProductType> createProductType(@RequestBody ProductType productType) {
        ProductType savedProductType = productTypeService.createProductType(productType);
        return ResponseEntity.ok(savedProductType);
    }

    @GetMapping
    public ResponseEntity<List<ProductType>> getAllProductTypes() {
        List<ProductType> productTypes = productTypeService.getAllProductTypes();
        return ResponseEntity.ok(productTypes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductType> getProductTypeById(@PathVariable Long id) {
        Optional<ProductType> productType = productTypeService.getProductTypeById(id);
        return productType.map(ResponseEntity::ok)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy loại sản phẩm với ID: " + id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductType> updateProductType(@PathVariable Long id, @RequestBody ProductType productTypeDetails) {
        ProductType updatedProductType = productTypeService.updateProductType(id, productTypeDetails);
        return ResponseEntity.ok(updatedProductType);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProductType(@PathVariable Long id) {
        productTypeService.deleteProductType(id);
        return ResponseEntity.ok().build();
    }
}