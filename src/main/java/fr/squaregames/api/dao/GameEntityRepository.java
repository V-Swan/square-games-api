package fr.squaregames.api.dao;

import fr.squaregames.api.dao.entity.GameEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface GameEntityRepository extends JpaRepository<GameEntity, UUID> {
}