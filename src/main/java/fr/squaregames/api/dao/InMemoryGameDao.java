package fr.squaregames.api.dao;

import fr.le_campus_numerique.square_games.engine.Game;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public class InMemoryGameDao implements GameDao {

    private final Map<String, Game> games = new HashMap<>();

    private final Map<String, Map<UUID, Long>> playerUserIds =
            new HashMap<>();

    @Override
    public Stream<Game> findAll() {
        return games.values().stream();
    }

    @Override
    public Stream<Game> findAllByUserId(Long userId) {
        return games.values().stream()
                .filter(game ->
                        playerUserIds
                                .getOrDefault(
                                        game.getId().toString(),
                                        Map.of()
                                )
                                .containsValue(userId)
                );
    }

    @Override
    public Optional<Game> findById(String gameId) {
        return Optional.ofNullable(games.get(gameId));
    }

    @Override
    public Optional<UUID> findPlayerIdByUserId(
            String gameId,
            Long userId
    ) {
        return playerUserIds
                .getOrDefault(gameId, Map.of())
                .entrySet()
                .stream()
                .filter(entry ->
                        userId.equals(entry.getValue())
                )
                .map(Map.Entry::getKey)
                .findFirst();
    }

    @Override
    public Game upsert(
            Game game,
            Map<UUID, Long> userIds
    ) {
        games.put(game.getId().toString(), game);

        Map<UUID, Long> existingUserIds =
                playerUserIds.getOrDefault(
                        game.getId().toString(),
                        new HashMap<>()
                );

        existingUserIds.putAll(userIds);

        playerUserIds.put(
                game.getId().toString(),
                existingUserIds
        );

        return game;
    }

    @Override
    public void delete(String gameId) {
        games.remove(gameId);
        playerUserIds.remove(gameId);
    }
}