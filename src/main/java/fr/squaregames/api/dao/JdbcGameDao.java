package fr.squaregames.api.dao;

import fr.le_campus_numerique.square_games.engine.*;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JdbcGameDao implements GameDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final Map<String, GameFactory> factories;

    public JdbcGameDao(
            NamedParameterJdbcTemplate jdbcTemplate,
            List<GameFactory> factories
    ) {
        this.jdbcTemplate = jdbcTemplate;

        this.factories = factories.stream()
                .collect(Collectors.toMap(
                        GameFactory::getGameFactoryId,
                        factory -> factory
                ));
    }

    @Override
    public Stream<Game> findAll() {

        String sql = """
                SELECT id
                FROM games
                """;

        return jdbcTemplate.queryForList(
                        sql,
                        Map.of(),
                        UUID.class
                )
                .stream()
                .map(UUID::toString)
                .map(this::findById)
                .flatMap(Optional::stream);
    }

    @Override
    public Stream<Game> findAllByUserId(Long userId) {

        String sql = """
                SELECT DISTINCT game_id
                FROM game_players
                WHERE user_id = :userId
                """;

        return jdbcTemplate.queryForList(
                        sql,
                        Map.of("userId", userId),
                        UUID.class
                )
                .stream()
                .map(UUID::toString)
                .map(this::findById)
                .flatMap(Optional::stream);
    }

    @Override
    public Optional<Game> findById(String gameId) {

        UUID uuid = UUID.fromString(gameId);

        Map<String, Object> params = Map.of(
                "gameId", uuid
        );

        String gameSql = """
                SELECT id, factory_id, status, current_player_id, board_size
                FROM games
                WHERE id = :gameId
                """;

        return jdbcTemplate.query(
                gameSql,
                params,
                resultSet -> {

                    if (!resultSet.next()) {
                        return Optional.empty();
                    }

                    UUID id = UUID.fromString(
                            resultSet.getString("id")
                    );

                    String factoryId =
                            resultSet.getString("factory_id");

                    String status =
                            resultSet.getString("status");

                    String currentPlayerId =
                            resultSet.getString("current_player_id");

                    int boardSize =
                            resultSet.getInt("board_size");

                    String playersSql = """
                            SELECT player_id, player_order
                            FROM game_players
                            WHERE game_id = :gameId
                            ORDER BY player_order
                            """;

                    List<UUID> playerIds = jdbcTemplate.query(
                            playersSql,
                            params,
                            (playersResultSet, rowNum) ->
                                    UUID.fromString(
                                            playersResultSet.getString("player_id")
                                    )
                    );

                    String tokensSql = """
                            SELECT player_id, token_name, position_x, position_y, removed
                            FROM game_tokens
                            WHERE game_id = :gameId
                            """;

                    List<TokenPosition<UUID>> boardTokens =
                            new ArrayList<>();

                    List<TokenPosition<UUID>> removedTokens =
                            new ArrayList<>();

                    jdbcTemplate.query(
                            tokensSql,
                            params,
                            tokensResultSet -> {

                                UUID playerId =
                                        tokensResultSet.getObject(
                                                "player_id",
                                                UUID.class
                                        );

                                String tokenName =
                                        tokensResultSet.getString("token_name");

                                Integer positionX =
                                        (Integer) tokensResultSet.getObject(
                                                "position_x"
                                        );

                                Integer positionY =
                                        (Integer) tokensResultSet.getObject(
                                                "position_y"
                                        );

                                boolean removed =
                                        tokensResultSet.getBoolean("removed");

                                if (!removed
                                        && positionX != null
                                        && positionY != null) {

                                    boardTokens.add(
                                            new TokenPosition<>(
                                                    playerId,
                                                    tokenName,
                                                    positionX,
                                                    positionY
                                            )
                                    );
                                } else if (removed) {

                                    removedTokens.add(
                                            new TokenPosition<>(
                                                    playerId,
                                                    tokenName,
                                                    0,
                                                    0
                                            )
                                    );
                                }
                            }
                    );

                    GameFactory factory = factories.get(factoryId);

                    if (factory == null) {
                        throw new IllegalArgumentException(
                                "Unknown game factory: " + factoryId
                        );
                    }

                    try {
                        Game game = factory.createGameWithIds(
                                id,
                                boardSize,
                                playerIds,
                                boardTokens,
                                removedTokens
                        );

                        return Optional.of(game);

                    } catch (InconsistentGameDefinitionException e) {
                        throw new IllegalStateException(
                                "Impossible de reconstruire la partie " + id,
                                e
                        );
                    }
                }
        );
    }

    @Override
    public Optional<UUID> findPlayerIdByUserId(
            String gameId,
            Long userId
    ) {

        String sql = """
                SELECT player_id
                FROM game_players
                WHERE game_id = :gameId
                  AND user_id = :userId
                """;

        List<UUID> playerIds = jdbcTemplate.query(
                sql,
                Map.of(
                        "gameId", UUID.fromString(gameId),
                        "userId", userId
                ),
                (resultSet, rowNum) ->
                        resultSet.getObject(
                                "player_id",
                                UUID.class
                        )
        );

        return playerIds.stream().findFirst();
    }

    @Transactional
    @Override
    public Game upsert(
            Game game,
            Map<UUID, Long> playerUserIds
    ) {

        String gameSql = """
                INSERT INTO games (
                    id,
                    factory_id,
                    status,
                    current_player_id,
                    board_size
                )
                VALUES (
                    :id,
                    :factoryId,
                    :status,
                    :currentPlayerId,
                    :boardSize
                )
                ON CONFLICT (id) DO UPDATE SET
                    factory_id = EXCLUDED.factory_id,
                    status = EXCLUDED.status,
                    current_player_id = EXCLUDED.current_player_id,
                    board_size = EXCLUDED.board_size
                """;

        MapSqlParameterSource gameParams =
                new MapSqlParameterSource()
                        .addValue("id", game.getId())
                        .addValue("factoryId", game.getFactoryId())
                        .addValue("status", game.getStatus().name())
                        .addValue(
                                "currentPlayerId",
                                game.getCurrentPlayerId()
                        )
                        .addValue("boardSize", game.getBoardSize());

        jdbcTemplate.update(gameSql, gameParams);

        Map<UUID, Long> existingUserIds = new HashMap<>();

        String existingPlayersSql = """
        SELECT player_id, user_id
        FROM game_players
        WHERE game_id = :gameId
        """;

        jdbcTemplate.query(
                existingPlayersSql,
                Map.of("gameId", game.getId()),
                resultSet -> {
                    UUID playerId = resultSet.getObject(
                            "player_id",
                            UUID.class
                    );

                    Long userId = resultSet.getObject(
                            "user_id",
                            Long.class
                    );

                    if (userId != null) {
                        existingUserIds.put(playerId, userId);
                    }
                }
        );

        String deletePlayersSql = """
                DELETE FROM game_players
                WHERE game_id = :gameId
                """;

        jdbcTemplate.update(
                deletePlayersSql,
                Map.of("gameId", game.getId())
        );

        String playerSql = """
                INSERT INTO game_players (
                    game_id,
                    player_id,
                    player_order,
                    user_id
                )
                VALUES (
                    :gameId,
                    :playerId,
                    :playerOrder,
                    :userId
                )
                """;

        int playerOrder = 0;

        for (UUID playerId : game.getPlayerIds()) {

            Long userId = playerUserIds.get(playerId);

            if (userId == null) {
                userId = existingUserIds.get(playerId);
            }

            MapSqlParameterSource playerParams =
                    new MapSqlParameterSource()
                            .addValue("gameId", game.getId())
                            .addValue("playerId", playerId)
                            .addValue("playerOrder", playerOrder)
                            .addValue("userId", userId);

            jdbcTemplate.update(
                    playerSql,
                    playerParams
            );

            playerOrder++;
        }
        // ---------------------------------------------------------
        // 4. Supprimer les anciens tokens
        // ---------------------------------------------------------

        String deleteTokensSql = """
                DELETE FROM game_tokens
                WHERE game_id = :gameId
                """;

        jdbcTemplate.update(
                deleteTokensSql,
                Map.of("gameId", game.getId())
        );

        // ---------------------------------------------------------
        // 5. Réinsérer les tokens actuels
        // ---------------------------------------------------------

        String tokenSql = """
                INSERT INTO game_tokens (
                    game_id,
                    player_id,
                    token_name,
                    position_x,
                    position_y,
                    removed
                )
                VALUES (
                    :gameId,
                    :playerId,
                    :tokenName,
                    :positionX,
                    :positionY,
                    :removed
                )
                """;

        // Tokens présents sur le plateau
        for (Map.Entry<CellPosition, Token> entry :
                game.getBoard().entrySet()) {

            CellPosition position = entry.getKey();
            Token token = entry.getValue();

            MapSqlParameterSource tokenParams =
                    new MapSqlParameterSource()
                            .addValue("gameId", game.getId())
                            .addValue(
                                    "playerId",
                                    token.getOwnerId().orElse(null)
                            )
                            .addValue("tokenName", token.getName())
                            .addValue("positionX", position.x())
                            .addValue("positionY", position.y())
                            .addValue("removed", false);

            jdbcTemplate.update(tokenSql, tokenParams);
        }

        // Tokens encore disponibles
        for (Token token : game.getRemainingTokens()) {

            MapSqlParameterSource tokenParams =
                    new MapSqlParameterSource()
                            .addValue("gameId", game.getId())
                            .addValue(
                                    "playerId",
                                    token.getOwnerId().orElse(null)
                            )
                            .addValue("tokenName", token.getName())
                            .addValue("positionX", null)
                            .addValue("positionY", null)
                            .addValue("removed", false);

            jdbcTemplate.update(tokenSql, tokenParams);
        }

        // Tokens retirés
        for (Token token : game.getRemovedTokens()) {

            MapSqlParameterSource tokenParams =
                    new MapSqlParameterSource()
                            .addValue("gameId", game.getId())
                            .addValue(
                                    "playerId",
                                    token.getOwnerId().orElse(null)
                            )
                            .addValue("tokenName", token.getName())
                            .addValue("positionX", null)
                            .addValue("positionY", null)
                            .addValue("removed", true);

            jdbcTemplate.update(tokenSql, tokenParams);
        }

        return game;
    }

    @Transactional
    @Override
    public void delete(String gameId) {

        UUID uuid = UUID.fromString(gameId);

        jdbcTemplate.update(
                """
                        DELETE FROM game_tokens
                        WHERE game_id = :gameId
                        """,
                Map.of("gameId", uuid)
        );

        jdbcTemplate.update(
                """
                        DELETE FROM game_players
                        WHERE game_id = :gameId
                        """,
                Map.of("gameId", uuid)
        );

        jdbcTemplate.update(
                """
                        DELETE FROM games
                        WHERE id = :gameId
                        """,
                Map.of("gameId", uuid)
        );
    }
}