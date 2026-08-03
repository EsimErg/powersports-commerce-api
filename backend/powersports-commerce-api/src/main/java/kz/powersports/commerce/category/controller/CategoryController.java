package kz.powersports.commerce.category.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import kz.powersports.commerce.category.dto.CategoryResponse;
import kz.powersports.commerce.category.service.CategoryService;
import kz.powersports.commerce.product.dto.ProductPageResponse;
import kz.powersports.commerce.product.service.ProductService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@Validated
public class CategoryController {

    private final CategoryService categoryService;
    private final ProductService productService;

    public CategoryController(
            CategoryService categoryService,
            ProductService productService
    ) {
        this.categoryService = categoryService;
        this.productService = productService;
    }

    @GetMapping
    public List<CategoryResponse> findAll() {
        return categoryService.findAll();
    }

    @GetMapping("/{slug}/products")
    public ProductPageResponse findProductsByCategory(

            @PathVariable
            String slug,

            @RequestParam(defaultValue = "0")
            @Min(0)
            int page,

            @RequestParam(defaultValue = "12")
            @Min(1)
            @Max(100)
            int size,

            @RequestParam(required = false)
            String search
    ) {
        return productService.findAll(
                page,
                size,
                search,
                slug
        );
    }
}