package com.flashlearn.mapper;

import com.flashlearn.dto.response.CardResponse;
import com.flashlearn.entity.Card;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct маппер для карточек
 */
@Mapper(componentModel = "spring")
public interface CardMapper {

    /**
     * Конвертирует Card в CardResponse
     */
    @Mapping(source = "deck.id", target = "deckId")
    CardResponse toResponse(Card card);
}
