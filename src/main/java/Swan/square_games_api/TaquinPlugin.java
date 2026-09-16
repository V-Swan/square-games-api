package Swan.square_games_api;

import fr.le_campus_numerique.square_games.engine.Game;
import fr.le_campus_numerique.square_games.engine.taquin.TaquinGameFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import java.util.Locale;

@Component
public class TaquinPlugin implements GamePlugin {

    @Value("${game.taquin.default-player-count:1}")
    private int defaultPlayerCount;

    @Value("${game.taquin.default-board-size:4}")
    private int defaultBoardSize;

    private final MessageSource messageSource;
    private final TaquinGameFactory factory;

    public TaquinPlugin(MessageSource messageSource, TaquinGameFactory factory) {
        this.messageSource = messageSource;
        this.factory = factory;
    }

    @Override
    public String getId() {
        return "taquin";
    }

    @Override
    public String getName(Locale locale) {
        return messageSource.getMessage("game.taquin.name", null, locale);
    }

    @Override
    public Game createDefaultGame() {
        return factory.createGame(defaultPlayerCount, defaultBoardSize);
    }

    @Override
    public Game createGame(int playerCount, int boardSize) {
        return factory.createGame(playerCount, boardSize);
    }
}