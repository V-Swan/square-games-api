package Swan.square_games_api;

import fr.le_campus_numerique.square_games.engine.Game;

import java.util.Locale;

public interface GamePlugin {
    String getName(Locale locale);
    Game createDefaultGame();
    Game createGame(int playerCount, int boardSize);
    String getId();
}