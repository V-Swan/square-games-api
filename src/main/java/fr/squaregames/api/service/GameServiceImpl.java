package fr.squaregames.api.service;

import fr.le_campus_numerique.square_games.engine.CellPosition;
import fr.le_campus_numerique.square_games.engine.Game;
import fr.le_campus_numerique.square_games.engine.InvalidPositionException;
import fr.le_campus_numerique.square_games.engine.Token;
import fr.squaregames.api.dao.GameDao;
import fr.squaregames.api.dto.MoveParams;
import fr.squaregames.api.plugin.GamePlugin;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class GameServiceImpl implements GameService {

    private final Map<String, GamePlugin> pluginsMap;
    private final GameDao gameDao;

    public GameServiceImpl(
            List<GamePlugin> plugins,
            GameDao gameDao
    ) {
        this.pluginsMap = plugins.stream()
                .collect(Collectors.toMap(
                        GamePlugin::getId,
                        plugin -> plugin
                ));

        this.gameDao = gameDao;
    }

    @Override
    public Game createGame(
            String gameType,
            Integer playerCount,
            Integer boardSize,
            Long userId
    ) {
        GamePlugin plugin = pluginsMap.get(gameType);

        if (plugin == null) {
            throw new IllegalArgumentException(
                    "Jeu inconnu : " + gameType
            );
        }

        Game game;

        if (playerCount == null || boardSize == null) {
            game = plugin.createDefaultGame();
        } else {
            Set<UUID> playerIds = new HashSet<>();

            for (int i = 0; i < playerCount; i++) {
                playerIds.add(UUID.randomUUID());
            }

            game = plugin.createGame(
                    playerCount,
                    boardSize,
                    playerIds
            );
        }

        Map<UUID, Long> playerUserIds = new HashMap<>();

        playerUserIds.put(
                game.getCurrentPlayerId(),
                userId
        );

        gameDao.upsert(game, playerUserIds);

        return game;
    }

    @Override
    public Game getGame(UUID gameId) {
        return gameDao.findById(gameId.toString())
                .orElse(null);
    }

    @Override
    public Collection<CellPosition> getPossibleMoves(
            UUID gameId,
            String tokenName
    ) {
        Game game = gameDao.findById(gameId.toString())
                .orElse(null);

        if (game == null) {
            throw new IllegalArgumentException(
                    "Partie inconnue : " + gameId
            );
        }

        for (Token token : game.getBoard().values()) {
            if (token.getName().equals(tokenName)
                    && token.canMove()) {

                return token.getAllowedMoves();
            }
        }

        for (Token token : game.getRemainingTokens()) {
            if (token.getName().equals(tokenName)
                    && token.canMove()) {

                return token.getAllowedMoves();
            }
        }

        throw new IllegalArgumentException(
                "Token introuvable ou impossible à déplacer : "
                        + tokenName
        );
    }

    @Override
    public Game playMove(
            UUID gameId,
            MoveParams moveParams,
            Long userId
    ) {
        Game game = gameDao.findById(gameId.toString())
                .orElse(null);

        if (game == null) {
            throw new IllegalArgumentException(
                    "Partie inconnue : " + gameId
            );
        }

        UUID playerUuid = gameDao
                .findPlayerIdByUserId(
                        gameId.toString(),
                        userId
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.FORBIDDEN,
                                "Le joueur n'appartient pas à cette partie."
                        )
                );

        if (!playerUuid.equals(game.getCurrentPlayerId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Ce n'est pas le tour de ce joueur."
            );
        }

        Token selectedToken = null;

        for (Token token : game.getBoard().values()) {
            if (token.getName().equals(moveParams.getTokenName())
                    && token.getOwnerId().isPresent()
                    && token.getOwnerId().get().equals(playerUuid)) {

                selectedToken = token;
                break;
            }
        }

        if (selectedToken == null) {
            for (Token token : game.getRemainingTokens()) {
                if (token.getName().equals(moveParams.getTokenName())
                        && token.getOwnerId().isPresent()
                        && token.getOwnerId().get().equals(playerUuid)) {

                    selectedToken = token;
                    break;
                }
            }
        }

        if (selectedToken == null) {
            throw new IllegalArgumentException(
                    "Token introuvable."
            );
        }

        CellPosition targetPosition = new CellPosition(
                moveParams.getTargetPosition().getX(),
                moveParams.getTargetPosition().getY()
        );

        if (!selectedToken.getAllowedMoves().contains(targetPosition)) {
            throw new IllegalArgumentException(
                    "Déplacement impossible vers cette position."
            );
        }

        try {
            selectedToken.moveTo(targetPosition);
        } catch (InvalidPositionException e) {
            throw new IllegalArgumentException(
                    "Déplacement impossible vers cette position.",
                    e
            );
        }

        Map<UUID, Long> playerUserIds = new HashMap<>();

        playerUserIds.put(
                playerUuid,
                userId
        );

        gameDao.upsert(game, playerUserIds);

        return game;
    }

    @Override
    public Collection<Game> getAllGames(Long userId) {
        return gameDao.findAllByUserId(userId).toList();
    }

    @Override
    public void deleteGame(UUID gameId) {
        gameDao.delete(gameId.toString());
    }
}