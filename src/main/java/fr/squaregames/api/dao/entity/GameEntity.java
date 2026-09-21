package fr.squaregames.api.dao.entity;

import jakarta.persistence.*;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "games")
public class GameEntity {

    @Id
    public UUID id;

    public String factoryId;

    public int boardSize;

    @OneToMany(
            mappedBy = "game",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("playerOrder ASC")
    public List<GamePlayerEntity> players;

    public String status;

    public UUID currentPlayerId;

    @OneToMany(
            mappedBy = "game",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    public List<GameTokenEntity> tokens;
}