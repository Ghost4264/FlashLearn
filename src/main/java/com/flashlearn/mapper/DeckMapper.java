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
    DeckResponse toResponse(Deck deck);
}
