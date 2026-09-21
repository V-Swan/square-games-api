package fr.squaregames.api.dao.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(
        name = "game_tokens",
        indexes = {
                @Index(name = "idx_game_tokens_game_id", columnList = "game_id")
        }
)
public class GameTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne
    @JoinColumn(name = "game_id", nullable = false)
    public GameEntity game;

    @Column(name = "player_id")
    public UUID ownerId;

    @Column(name = "token_name")
    public String name;

    public boolean removed;

    @Column(name = "position_x")
    public Integer x;

    @Column(name = "position_y")
    public Integer y;
}