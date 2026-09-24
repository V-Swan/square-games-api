package fr.squaregames.api.dao.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(
        name = "game_players",
        indexes = {
                @Index(
                        name = "idx_game_players_game_order",
                        columnList = "game_id, player_order"
                )
        }
)
@IdClass(GamePlayerId.class)
public class GamePlayerEntity {

    @Id
    @Column(name = "game_id")
    public UUID gameId;

    @Id
    @Column(name = "player_id")
    public UUID playerId;

    @Column(name = "user_id")
    public Long userId;

    @ManyToOne
    @JoinColumn(
            name = "game_id",
            insertable = false,
            updatable = false
    )
    public GameEntity game;

    @Column(name = "player_order")
    public int playerOrder;
}