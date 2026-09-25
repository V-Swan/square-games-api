package fr.squaregames.api.controller;

import fr.le_campus_numerique.square_games.engine.CellPosition;
import fr.le_campus_numerique.square_games.engine.Game;
import fr.squaregames.api.dto.GameCreationParams;
import fr.squaregames.api.dto.MoveParams;
import fr.squaregames.api.security.JwtUserPrincipal;
import fr.squaregames.api.service.GameService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.UUID;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @Operation(
            summary = "Créer une partie",
            description = "Crée une nouvelle partie pour l'utilisateur authentifié."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Partie créée avec succès"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Utilisateur non authentifié"
            )
    })
    @PostMapping
    public ResponseEntity<Game> createGame(
            Authentication authentication,
            @RequestBody GameCreationParams params) {

        JwtUserPrincipal principal =
                (JwtUserPrincipal) authentication.getPrincipal();

        Long userId = principal.getUserId();

        Game game = gameService.createGame(
                params.getGameType(),
                params.getPlayerCount(),
                params.getBoardSize(),
                userId
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(game);
    }

    @Operation(
            summary = "Lister les parties de l'utilisateur",
            description = "Retourne uniquement les parties auxquelles l'utilisateur authentifié participe."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Liste des parties récupérée avec succès"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Utilisateur non authentifié"
            )
    })
    @GetMapping("/all")
    public ResponseEntity<Collection<Game>> getAllGames(
            Authentication authentication) {

        JwtUserPrincipal principal =
                (JwtUserPrincipal) authentication.getPrincipal();

        Long userId = principal.getUserId();

        Collection<Game> games =
                gameService.getAllGames(userId);

        return ResponseEntity.ok(games);
    }

    @Operation(
            summary = "Récupérer une partie",
            description = "Retourne l'état actuel d'une partie."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Partie trouvée"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Partie introuvable"
            )
    })
    @GetMapping("/{gameId}")
    public ResponseEntity<Game> getGameState(
            @Parameter(
                    description = "Identifiant UUID de la partie",
                    required = true
            )
            @PathVariable UUID gameId) {

        Game game = gameService.getGame(gameId);

        if (game == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(game);
    }

    @Operation(
            summary = "Récupérer les déplacements possibles",
            description = "Retourne les positions vers lesquelles un token peut être déplacé."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Déplacements récupérés avec succès"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Partie introuvable"
            )
    })
    @GetMapping("/{gameId}/tokens/{tokenName}/possible-moves")
    public ResponseEntity<Collection<CellPosition>> getPossibleMoves(
            @Parameter(
                    description = "Identifiant UUID de la partie",
                    required = true
            )
            @PathVariable UUID gameId,

            @Parameter(
                    description = "Nom du token",
                    required = true
            )
            @PathVariable String tokenName) {

        Collection<CellPosition> possibleMoves =
                gameService.getPossibleMoves(
                        gameId,
                        tokenName
                );

        return ResponseEntity.ok(possibleMoves);
    }

    @Operation(
            summary = "Jouer un coup",
            description = "Effectue un déplacement pour l'utilisateur authentifié."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Coup joué avec succès"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Utilisateur non authentifié"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Utilisateur absent de la partie ou ce n'est pas son tour"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Partie introuvable"
            )
    })
    @PostMapping("/{gameId}/moves")
    public ResponseEntity<Game> playMove(
            @Parameter(
                    description = "Identifiant UUID de la partie",
                    required = true
            )
            @PathVariable UUID gameId,

            Authentication authentication,

            @RequestBody MoveParams moveParams) {

        JwtUserPrincipal principal =
                (JwtUserPrincipal) authentication.getPrincipal();

        Long userId = principal.getUserId();

        Game game = gameService.playMove(
                gameId,
                moveParams,
                userId
        );

        return ResponseEntity.ok(game);
    }

    @Operation(
            summary = "Supprimer une partie",
            description = "Supprime une partie existante."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Partie supprimée avec succès"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Utilisateur non authentifié"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Partie introuvable"
            )
    })
    @DeleteMapping("/{gameId}")
    public ResponseEntity<Void> deleteGame(
            @Parameter(
                    description = "Identifiant UUID de la partie",
                    required = true
            )
            @PathVariable UUID gameId) {

        Game game = gameService.getGame(gameId);

        if (game == null) {
            return ResponseEntity.notFound().build();
        }

        gameService.deleteGame(gameId);

        return ResponseEntity.noContent().build();
    }
}