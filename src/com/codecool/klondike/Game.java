package com.codecool.klondike;

import javafx.collections.FXCollections;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.*;

public class Game extends Pane {

    private static final int PILE_IS_FULL = 13;
    private List<Card> deck;

    private Pile stockPile;
    private Pile discardPile;
    private List<Pile> foundationPiles = FXCollections.observableArrayList();
    private List<Pile> tableauPiles = FXCollections.observableArrayList();

    private Stack<Move> moves = new Stack<>();

    private double dragStartX, dragStartY;
    private List<Card> draggedCards = FXCollections.observableArrayList();

    private static double STOCK_GAP = 1;
    private static double FOUNDATION_GAP = 0;
    private static double TABLEAU_GAP = 30;


    private EventHandler<MouseEvent> onMouseClickedHandler = e -> {
        Card card = (Card) e.getSource();
        if (card.getContainingPile().getPileType() == Pile.PileType.STOCK) {
            if (stockPile.numOfCards() < 1) {
                refillStockFromDiscard();
            }
            card.moveToPile(discardPile);
            card.flip();
            card.setMouseTransparent(false);
            System.out.println("Placed " + card + " to the waste.");
        }
        else if (e.getButton().equals(MouseButton.PRIMARY) && e.getClickCount() == 2) {
            for (Pile foundationPile : foundationPiles) {
                Card topCard = foundationPile.getTopCard();
                if ((card.getRank() == Rank.ACE && topCard == null) || (topCard != null &&
                        topCard.getSuit() == card.getSuit() && topCard.getRank().VALUE == card.getRank().VALUE - 1)) {
                    if (card.getContainingPile().getPileType() != Pile.PileType.DISCARD && card.getContainingPile().getCards().size() != 1) {
                        card.getContainingPile().getSecondTopCard().flip();
                    }
                    moves.push(new Move(card));
                    card.moveToPile(foundationPile);
                    checkWinCondition();
                    if (isGameWon()) winTheGame();
                    break;
                }
            }

        }
    };

    private EventHandler<MouseEvent> stockReverseCardsHandler = e -> {
        refillStockFromDiscard();
    };

    private EventHandler<MouseEvent> onMousePressedHandler = e -> {
        dragStartX = e.getSceneX();
        dragStartY = e.getSceneY();
    };

    private EventHandler<MouseEvent> onMouseDraggedHandler = e -> {
        Card card = (Card) e.getSource();
        Pile activePile = card.getContainingPile();

        int cardIndex = activePile.getCards().indexOf(card);
        ListIterator<Card> cards = activePile.getCards().listIterator(cardIndex);

        if (activePile.getPileType() == Pile.PileType.STOCK)
            return;
        double offsetX = e.getSceneX() - dragStartX;
        double offsetY = e.getSceneY() - dragStartY;
        draggedCards.clear();
        if(!card.isFaceDown()) {
            while (cards.hasNext()) {
                card = cards.next();
                draggedCards.add(card);
                card.getDropShadow().setRadius(20);
                card.getDropShadow().setOffsetX(10);
                card.getDropShadow().setOffsetY(10);

                card.toFront();
                card.setTranslateX(offsetX);
                card.setTranslateY(offsetY);
            }
        }
    };

    private EventHandler<MouseEvent> onMouseReleasedHandler = e -> {
        if (draggedCards.isEmpty())
            return;
        Card card = (Card) e.getSource();
        Pile tableauPile = getValidIntersectingPile(card, tableauPiles);
        Pile foundationPile = getValidIntersectingPile(card, foundationPiles);
        //TODO
        Pile cardsCurrentPile = card.getContainingPile();
        int countFaceDown = 0;
        for(Card currentCard: cardsCurrentPile.getCards()){
            if (currentCard.isFaceDown()){
                countFaceDown ++;
            }

        }
        if (tableauPile != null) {
            handleValidMove(card, tableauPile);
            if (countFaceDown != 0 && cardsCurrentPile.getPileType() != Pile.PileType.DISCARD && card.getContainingPile().getCards().get(card.getContainingPile().getCards().indexOf(card)-1).isFaceDown()) {
                 cardsCurrentPile.getCards().get(countFaceDown - 1).flip();
            }
        } else if (foundationPile != null) {
            handleValidMove(card, foundationPile);
            if (countFaceDown != 0 && cardsCurrentPile.getPileType() != Pile.PileType.DISCARD && card.getContainingPile().getCards().get(card.getContainingPile().getCards().indexOf(card)-1).isFaceDown()) {
                 cardsCurrentPile.getCards().get(countFaceDown - 1).flip();
            }
        } else {
            draggedCards.forEach(MouseUtil::slideBack);
            draggedCards.clear();
        }
    };

    public boolean isGameWon() {
        for (Pile foundationPile : foundationPiles) {
            if (foundationPile.numOfCards() != PILE_IS_FULL) return false;
        }
        return true;
    }

    public Game() {
        deck = Card.createNewDeck();
        addRedoButton();
        initPiles();
        dealCards();
    }

    public void addMouseEventHandlers(Card card) {
        card.setOnMousePressed(onMousePressedHandler);
        card.setOnMouseDragged(onMouseDraggedHandler);
        card.setOnMouseReleased(onMouseReleasedHandler);
        card.setOnMouseClicked(onMouseClickedHandler);
    }

    public void refillStockFromDiscard() {
        for(int i = discardPile.numOfCards()-1; i > -1; i--) {
            Card card = discardPile.getCards().get(i);
            card.moveToPile(stockPile);
            card.flip();
        }
        System.out.println("Stock refilled from discard pile.");
    }


    public boolean isMoveValid(Card card, Pile destPile) {
        int cardRank = card.getRank().VALUE;
        if (destPile.getPileType().equals(Pile.PileType.TABLEAU)) {
            if (cardRank == 13 && destPile.isEmpty()) {
                return true;
            } else if (destPile.isEmpty()) {
                return false;
            }
            Card topCard = destPile.getTopCard();
            int destCardRank = topCard.getRank().VALUE;

            if (Card.isOppositeColor(topCard, card) && cardRank == destCardRank - 1) {
                return true;
            } else {
                return false;
            }
        } else if (destPile.getPileType().equals(Pile.PileType.FOUNDATION)) {
            if (cardRank == 1 && destPile.isEmpty()) {
                return true;
            } else if (destPile.isEmpty()) {
                return false;
            }
            Card topCard = destPile.getTopCard();
            int destCardRank = topCard.getRank().VALUE;

            if (Card.isSameSuit(topCard, card) && cardRank == destCardRank + 1) {
                return true;
            } else {
                return false;
            }

        }
        return false;
    }

    private Pile getValidIntersectingPile(Card card, List<Pile> piles) {
        Pile result = null;
        for (Pile pile : piles) {
            if (!pile.equals(card.getContainingPile()) &&
                    isOverPile(card, pile) &&
                    isMoveValid(card, pile))
                result = pile;
        }
        return result;
    }

    private boolean isOverPile(Card card, Pile pile) {
        if (pile.isEmpty())
            return card.getBoundsInParent().intersects(pile.getBoundsInParent());
        else
            return card.getBoundsInParent().intersects(pile.getTopCard().getBoundsInParent());
    }

    private void handleValidMove(Card card, Pile destPile) {
        String msg = null;
        if (destPile.isEmpty()) {
            if (destPile.getPileType().equals(Pile.PileType.FOUNDATION))
                msg = String.format("Placed %s to the foundation.", card);
            if (destPile.getPileType().equals(Pile.PileType.TABLEAU))
                msg = String.format("Placed %s to a new pile.", card);
        } else {
            msg = String.format("Placed %s to %s.", card, destPile.getTopCard());
        }
        System.out.println(msg);
        moves.push(new Move(draggedCards));
        MouseUtil.slideToDest(draggedCards, destPile);
        draggedCards.clear();

        checkWinCondition();
        if (isGameWon()) winTheGame();
    }

    private void initPiles() {
        stockPile = new Pile(Pile.PileType.STOCK, "Stock", STOCK_GAP);
        stockPile.setBlurredBackground();
        stockPile.setLayoutX(95);
        stockPile.setLayoutY(20);
        stockPile.setOnMouseClicked(stockReverseCardsHandler);
        getChildren().add(stockPile);

        discardPile = new Pile(Pile.PileType.DISCARD, "Discard", STOCK_GAP);
        discardPile.setBlurredBackground();
        discardPile.setLayoutX(285);
        discardPile.setLayoutY(20);
        getChildren().add(discardPile);

        for (int i = 0; i < 4; i++) {
            Pile foundationPile = new Pile(Pile.PileType.FOUNDATION, "Foundation " + i, FOUNDATION_GAP);
            foundationPile.setBlurredBackground();
            foundationPile.setLayoutX(610 + i * 180);
            foundationPile.setLayoutY(20);
            foundationPiles.add(foundationPile);
            getChildren().add(foundationPile);
        }
        for (int i = 0; i < 7; i++) {
            Pile tableauPile = new Pile(Pile.PileType.TABLEAU, "Tableau " + i, TABLEAU_GAP);
            tableauPile.setBlurredBackground();
            tableauPile.setLayoutX(95 + i * 180);
            tableauPile.setLayoutY(275);
            tableauPiles.add(tableauPile);
            getChildren().add(tableauPile);
        }
    }

    public void dealCards() {
        Iterator<Card> deckIterator = deck.iterator();
        Collections.shuffle(deck);
        int cardsToDeal = 1;
        for(Pile tableauPile: tableauPiles) {
            if(cardsToDeal == 8) break;
            for(int i = 0; i < cardsToDeal; i++){
                Card cardToAdd = deckIterator.next();
                tableauPile.addCard(cardToAdd);
                addMouseEventHandlers(cardToAdd);
                getChildren().add(cardToAdd);
                deckIterator.remove();
            }
            tableauPile.getTopCard().flip();
            if(++cardsToDeal == 8) break;
        }

        deckIterator.forEachRemaining(card -> {
            stockPile.addCard(card);
            addMouseEventHandlers(card);
            getChildren().add(card);
        });

    }

    public void setTableBackground(Image tableBackground) {
        setBackground(new Background(new BackgroundImage(tableBackground,
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                BackgroundPosition.CENTER, BackgroundSize.DEFAULT)));
    }

    private void winTheGame() {
        Alert winBox = new Alert(Alert.AlertType.INFORMATION);
        winBox.setTitle("Congratulations!");
        winBox.setHeaderText(null);
        winBox.setContentText("You have won the game!");
        // winBox.initModality();
        winBox.showAndWait();

        // Temporary until game can be restarted
        Stage window = (Stage)this.getScene().getWindow();
        window.close();
    }

    public void undo() {
        if (moves.empty()) return;

        Move lastMove = moves.pop();
        MouseUtil.slideToDest(lastMove.getDraggedCards(), lastMove.getSource());
    }

    private void addRedoButton() {
        Button redoBtn = new Button("Redo");
        redoBtn.setLayoutX(500);
        redoBtn.setLayoutY(20);
        redoBtn.setOnAction(e -> undo());
        getChildren().add(redoBtn);
    }

    private void checkWinCondition() {
        if (!(stockPile.isEmpty() && discardPile.isEmpty())) return;
        for (Pile tableauPile : tableauPiles) {
            for (Card card : tableauPile.getCards()) {
                if (card.isFaceDown()) return;
            }
        }
        moveAllCardsToFoundationPiles();
    }

    private void moveAllCardsToFoundationPiles() {
        while (cardsInTableau()) {

            for (Pile tableauPile : tableauPiles) {
                if (tableauPile.isEmpty()) continue;
                Card topCard = tableauPile.getTopCard();
                for (Pile foundationPile : foundationPiles) {
                    Card targetCard = foundationPile.getTopCard();
                    if ((targetCard == null && topCard.getRank() == Rank.ACE) ||
                            (targetCard != null && targetCard.getSuit() == topCard.getSuit() &&
                                    targetCard.getRank().VALUE == topCard.getRank().VALUE - 1)) {
                        topCard.moveToPile(foundationPile);
                    }
                }
            }

        }
    }

    private boolean cardsInTableau() {
        for (Pile tableauPile : tableauPiles) {
            if (!tableauPile.isEmpty()) return true;
        }
        return false;
    }

}
