package com.codecool.klondike;

import javafx.collections.FXCollections;
import java.util.List;


class Move {

    private List<Card> draggedCards;
    private Pile source;

    Move(List<Card> draggedCards) {
        this.draggedCards = FXCollections.observableArrayList(draggedCards);
        this.source = this.draggedCards.get(0).getContainingPile();
    }

    public List<Card> getDraggedCards() {
        return draggedCards;
    }

    public Pile getSource() {
        return source;
    }

}
