package Swan.square_games_api;

import fr.le_campus_numerique.square_games.engine.CellPosition;
import fr.le_campus_numerique.square_games.engine.Game;
import fr.le_campus_numerique.square_games.engine.Token;
import fr.le_campus_numerique.square_games.engine.InvalidPositionException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
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
            Integer boardSize
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
            game = plugin.createGame(playerCount, boardSize);
        }

        gameDao.upsert(game);

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
            MoveParams moveParams
    ) {
        Game game = gameDao.findById(gameId.toString())
                .orElse(null);

        if (game == null) {
            throw new IllegalArgumentException(
                    "Partie inconnue : " + gameId
            );
        }

        UUID playerUuid = UUID.fromString(
                moveParams.getPlayerUuid()
        );

        if (!game.getPlayerIds().contains(playerUuid)) {
            throw new IllegalArgumentException(
                    "Le joueur n'appartient pas à cette partie."
            );
        }

        if (!playerUuid.equals(game.getCurrentPlayerId())) {
            throw new IllegalArgumentException(
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

        return game;
    }
}