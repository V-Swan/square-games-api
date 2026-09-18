package fr.squaregames.api.service;

import fr.le_campus_numerique.square_games.engine.CellPosition;
import fr.le_campus_numerique.square_games.engine.Game;
import fr.squaregames.api.dto.MoveParams;

import java.util.Collection;
import java.util.UUID;

public interface GameService {

    Game createGame(String gameType, Integer playerCount, Integer boardSize);

    Game getGame(UUID gameId);

    Collection<Game> getAllGames();

    void deleteGame(UUID gameId);

    Collection<CellPosition> getPossibleMoves(
            UUID gameId,
            String tokenName
    );

    Game playMove(
            UUID gameId,
            MoveParams moveParams
    );

}