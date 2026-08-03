package kz.powersports.commerce.category.dto;

public record CategoryResponse(

        Long id,

        String name,

        String slug,

        String description,

        Long parentId,

        int productCount,

        String imageUrl

) {
}