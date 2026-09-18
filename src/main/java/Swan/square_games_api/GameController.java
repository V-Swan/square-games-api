package Swan.square_games_api;

import fr.le_campus_numerique.square_games.engine.CellPosition;
import fr.le_campus_numerique.square_games.engine.Game;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @PostMapping
    public ResponseEntity<Game> createGame(
            @RequestBody GameCreationParams params) {

        Game game = gameService.createGame(
                params.getGameType(),
                params.getPlayerCount(),
                params.getBoardSize()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(game);
    }

    @GetMapping("/all")
    public ResponseEntity<Collection<Game>> getAllGames() {

        Collection<Game> games = gameService.getAllGames();

        return ResponseEntity.ok(games);
    }

    @GetMapping("/{gameId}")
    public ResponseEntity<Game> getGameState(
            @PathVariable UUID gameId) {

        Game game = gameService.getGame(gameId);

        if (game == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(game);
    }

    @GetMapping("/{gameId}/tokens/{tokenName}/possible-moves")
    public ResponseEntity<Collection<CellPosition>> getPossibleMoves(
            @PathVariable UUID gameId,
            @PathVariable String tokenName) {

        Collection<CellPosition> possibleMoves =
                gameService.getPossibleMoves(gameId, tokenName);

        return ResponseEntity.ok(possibleMoves);
    }

    @PostMapping("/{gameId}/moves")
    public ResponseEntity<Game> playMove(
            @PathVariable UUID gameId,
            @RequestBody MoveParams moveParams) {

        Game game = gameService.playMove(
                gameId,
                moveParams
        );

        return ResponseEntity.ok(game);
    }

    @DeleteMapping("/{gameId}")
    public ResponseEntity<Void> deleteGame(
            @PathVariable UUID gameId) {

        Game game = gameService.getGame(gameId);

        if (game == null) {
            return ResponseEntity.notFound().build();
        }

        gameService.deleteGame(gameId);

        return ResponseEntity.noContent().build();
    }
}