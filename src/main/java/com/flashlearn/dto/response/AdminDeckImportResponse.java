package com.flashlearn.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AdminDeckImportResponse {
    DeckResponse deck;
    int importedCards;
}
