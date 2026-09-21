package fr.squaregames.api.dao;

import fr.le_campus_numerique.square_games.engine.Game;
import fr.le_campus_numerique.square_games.engine.GameFactory;
import fr.le_campus_numerique.square_games.engine.Token;
import fr.le_campus_numerique.square_games.engine.TokenPosition;
import fr.squaregames.api.dao.entity.GameEntity;
import fr.squaregames.api.dao.entity.GamePlayerEntity;
import fr.squaregames.api.dao.entity.GameTokenEntity;

import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Repository
public class JpaGameDao implements GameDao {

    private final GameEntityRepository gameEntityRepository;
    private final List<GameFactory> factories;

    public JpaGameDao(
            GameEntityRepository gameEntityRepository,
            List<GameFactory> factories
    ) {
        this.gameEntityRepository = gameEntityRepository;
        this.factories = factories;
    }

    @Override
    public Stream<Game> findAll() {
        return gameEntityRepository.findAll()
                .stream()
                .map(this::toGame);
    }

    @Override
    public Optional<Game> findById(String gameId) {
        return gameEntityRepository.findById(UUID.fromString(gameId))
                .map(this::toGame);
    }

    @Override
    public Game upsert(Game game) {
        GameEntity entity = toEntity(game);
        gameEntityRepository.save(entity);
        return game;
    }

    @Override
    public void delete(String gameId) {
        gameEntityRepository.deleteById(UUID.fromString(gameId));
    }

    private Game toGame(GameEntity entity) {

        UUID gameId = entity.id;

        List<UUID> players = entity.players.stream()
                .map(player -> player.playerId)
                .toList();

        Collection<TokenPosition<UUID>> boardTokens = new ArrayList<>();
        Collection<TokenPosition<UUID>> removedTokens = new ArrayList<>();

        for (GameTokenEntity token : entity.tokens) {

            UUID ownerId = token.ownerId;

            if (token.removed) {

                removedTokens.add(
                        new TokenPosition<>(
                                ownerId,
                                token.name,
                                0,
                                0
                        )
                );

            } else if (token.x != null && token.y != null) {

                boardTokens.add(
                        new TokenPosition<>(
                                ownerId,
                                token.name,
                                token.x,
                                token.y
                        )
                );
            }
        }

        GameFactory factory = factories.stream()
                .filter(f -> f.getGameFactoryId().equals(entity.factoryId))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Factory inconnue : " + entity.factoryId
                        )
                );

        try {
            return factory.createGameWithIds(
                    gameId,
                    entity.boardSize,
                    players,
                    boardTokens,
                    removedTokens
            );
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Impossible de reconstruire la partie " + entity.id,
                    e
            );
        }
    }

    private GameEntity toEntity(Game game) {

        GameEntity entity = new GameEntity();

        entity.id = game.getId();
        entity.factoryId = game.getFactoryId();
        entity.boardSize = game.getBoardSize();

        entity.players = new ArrayList<>();

        int playerOrder = 0;

        for (UUID playerId : game.getPlayerIds()) {

            GamePlayerEntity playerEntity = new GamePlayerEntity();

            playerEntity.gameId = entity.id;
            playerEntity.playerId = playerId;
            playerEntity.playerOrder = playerOrder++;
            playerEntity.game = entity;

            entity.players.add(playerEntity);
        }

        entity.status = game.getStatus().toString();

        entity.currentPlayerId = game.getCurrentPlayerId();

        entity.tokens = new ArrayList<>();

        for (Token token : game.getBoard().values()) {

            GameTokenEntity tokenEntity = new GameTokenEntity();

            tokenEntity.game = entity;

            tokenEntity.ownerId = token.getOwnerId()
                    .orElse(null);

            tokenEntity.name = token.getName();
            tokenEntity.removed = false;

            if (token.getPosition() != null) {
                tokenEntity.x = token.getPosition().x();
                tokenEntity.y = token.getPosition().y();
            }

            entity.tokens.add(tokenEntity);
        }

        for (Token token : game.getRemovedTokens()) {

            GameTokenEntity tokenEntity = new GameTokenEntity();

            tokenEntity.game = entity;

            tokenEntity.ownerId = token.getOwnerId()
                    .orElse(null);

            tokenEntity.name = token.getName();
            tokenEntity.removed = true;
            tokenEntity.x = null;
            tokenEntity.y = null;

            entity.tokens.add(tokenEntity);
        }

        for (Token token : game.getRemainingTokens()) {

            GameTokenEntity tokenEntity = new GameTokenEntity();

            tokenEntity.game = entity;

            tokenEntity.ownerId = token.getOwnerId()
                    .orElse(null);

            tokenEntity.name = token.getName();
            tokenEntity.removed = false;
            tokenEntity.x = null;
            tokenEntity.y = null;

            entity.tokens.add(tokenEntity);
        }

        return entity;
    }
}