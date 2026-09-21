package fr.squaregames.api.dao.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class GamePlayerId implements Serializable {

    public UUID gameId;
    public UUID playerId;

    public GamePlayerId() {
    }

    public GamePlayerId(UUID gameId, UUID playerId) {
        this.gameId = gameId;
        this.playerId = playerId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof GamePlayerId that)) return false;

        return Objects.equals(gameId, that.gameId)
                && Objects.equals(playerId, that.playerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gameId, playerId);
    }
}