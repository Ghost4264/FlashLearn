package com.flashlearn.mapper;

import com.flashlearn.dto.response.DeckResponse;
import com.flashlearn.entity.Deck;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct маппер для колод
 */
@Mapper(componentModel = "spring")
public interface DeckMapper {

    /**
     * Конвертирует Deck в DeckResponse
     */
    @Mapping(target = "cardCount", ignore = true)
    @Mapping(target = "dueCardCount", ignore = true)
    @Mapping(target = "alreadyCloned", ignore = true)
    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    DeckResponse toResponse(Deck deck);
}
