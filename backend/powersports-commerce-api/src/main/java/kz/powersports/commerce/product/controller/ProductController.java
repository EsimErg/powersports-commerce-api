package kz.powersports.commerce.product.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import kz.powersports.commerce.product.dto.ProductPageResponse;
import kz.powersports.commerce.product.dto.ProductResponse;
import kz.powersports.commerce.product.service.ProductService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
@Validated
public class ProductController {

    private final ProductService productService;

    public ProductController(
            ProductService productService
    ) {
        this.productService = productService;
    }

    @GetMapping
    public ProductPageResponse findAll(

            @RequestParam(defaultValue = "0")
            @Min(0)
            int page,

            @RequestParam(defaultValue = "12")
            @Min(1)
            @Max(100)
            int size,

            @RequestParam(required = false)
            String search,

            @RequestParam(required = false)
            String category
    ) {
        return productService.findAll(
                page,
                size,
                search,
                category
        );
    }

    @GetMapping("/{slug}")
    public ProductResponse findBySlug(
            @PathVariable String slug
    ) {
        return productService.findBySlug(slug);
    }
}