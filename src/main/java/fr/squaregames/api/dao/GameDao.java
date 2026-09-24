package fr.squaregames.api.dao;

import fr.le_campus_numerique.square_games.engine.Game;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public interface GameDao {

    Stream<Game> findAll();

    Stream<Game> findAllByUserId(Long userId);

    Optional<Game> findById(String gameId);

    Optional<UUID> findPlayerIdByUserId(
            String gameId,
            Long userId
    );

    Game upsert(
            Game game,
            Map<UUID, Long> playerUserIds
    );

    void delete(String gameId);
}