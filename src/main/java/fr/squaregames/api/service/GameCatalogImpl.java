package fr.squaregames.api.service;

import fr.le_campus_numerique.square_games.engine.tictactoe.TicTacToeGameFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
@Component
public class GameCatalogImpl implements GameCatalog {

    private final TicTacToeGameFactory ticTacToeGameFactory;

    public GameCatalogImpl(TicTacToeGameFactory ticTacToeGameFactory) {
        this.ticTacToeGameFactory = ticTacToeGameFactory;
    }

    @Override
    public Collection<String> getAvailableGameIds() {
        return List.of(ticTacToeGameFactory.getGameFactoryId());
    }
}