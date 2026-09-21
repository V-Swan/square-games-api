package fr.squaregames.api.dao;

import fr.le_campus_numerique.square_games.engine.*;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
    public Optional<Game> findById(String gameId) {

        UUID uuid = UUID.fromString(gameId);

        Map<String, Object> params = Map.of(
                "gameId", uuid
        );

        // ---------------------------------------------------------
        // 1. Récupérer les informations générales de la partie
        // ---------------------------------------------------------

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

                    // ---------------------------------------------------------
                    // 2. Récupérer les joueurs
                    // ---------------------------------------------------------

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

                    // ---------------------------------------------------------
                    // 3. Récupérer les tokens
                    // ---------------------------------------------------------

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

                                // Token placé sur le plateau
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
                                }

                                // Token retiré
                                else if (removed) {

                                    removedTokens.add(
                                            new TokenPosition<>(
                                                    playerId,
                                                    tokenName,
                                                    0,
                                                    0
                                            )
                                    );
                                }

                                // position_x = NULL
                                // position_y = NULL
                                // removed = false
                                //
                                // => token encore disponible
                                // => rien à faire
                            }
                    );


                    // ---------------------------------------------------------
                    // 5. Récupérer la GameFactory
                    // ---------------------------------------------------------

                    GameFactory factory = factories.get(factoryId);

                    if (factory == null) {
                        throw new IllegalArgumentException(
                                "Unknown game factory: " + factoryId
                        );
                    }

                    // ---------------------------------------------------------
                    // 6. Reconstruction du Game
                    // ---------------------------------------------------------

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

    @Transactional
    @Override
    public Game upsert(Game game) {

        // ---------------------------------------------------------
        // 1. Sauvegarder les informations générales de la partie
        // ---------------------------------------------------------

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


        // ---------------------------------------------------------
        // 2. Supprimer les anciens joueurs
        // ---------------------------------------------------------

        String deletePlayersSql = """
                DELETE FROM game_players
                WHERE game_id = :gameId
                """;

        jdbcTemplate.update(
                deletePlayersSql,
                Map.of("gameId", game.getId())
        );


        // ---------------------------------------------------------
        // 3. Réinsérer les joueurs actuels
        // ---------------------------------------------------------

        String playerSql = """
                INSERT INTO game_players (
                    game_id,
                    player_id,
                    player_order
                )
                VALUES (
                    :gameId,
                    :playerId,
                    :playerOrder
                )
                """;

        int playerOrder = 0;

        for (UUID playerId : game.getPlayerIds()) {

            Map<String, Object> playerParams = Map.of(
                    "gameId", game.getId(),
                    "playerId", playerId,
                    "playerOrder", playerOrder
            );

            jdbcTemplate.update(playerSql, playerParams);

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


        // ---------------------------------------------------------
        // 5.1 Tokens présents sur le plateau
        // ---------------------------------------------------------

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


        // ---------------------------------------------------------
        // 5.2 Tokens encore disponibles
        // ---------------------------------------------------------

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


        // ---------------------------------------------------------
        // 5.3 Tokens retirés
        // ---------------------------------------------------------

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


        // ---------------------------------------------------------
        // 6. Retourner la partie
        // ---------------------------------------------------------

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