package fr.squaregames.api.plugin;

import fr.le_campus_numerique.square_games.engine.Game;
import fr.le_campus_numerique.square_games.engine.connectfour.ConnectFourGameFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import java.util.Set;
import java.util.UUID;

import java.util.Locale;
@Component
public class ConnectFourPlugin implements GamePlugin {

    @Value("${game.connectfour.default-player-count:2}")
    private int defaultPlayerCount;

    @Value("${game.connectfour.default-board-size:7}")
    private int defaultBoardSize;

    private final MessageSource messageSource;
    private final ConnectFourGameFactory factory;

    public ConnectFourPlugin(MessageSource messageSource, ConnectFourGameFactory factory) {
        this.messageSource = messageSource;
        this.factory = factory;
    }

    @Override
    public String getId() {
        return "connectfour";
    }

    @Override
    public String getName(Locale locale) {
        return messageSource.getMessage("game.connectfour.name", null, locale);
    }

    @Override
    public Game createDefaultGame() {
        return factory.createGame(defaultPlayerCount, defaultBoardSize);
    }

    @Override
    public Game createGame(int playerCount, int boardSize) {
        return factory.createGame(playerCount, boardSize);
    }

    @Override
    public Game createGame(
            int playerCount,
            int boardSize,
            Set<UUID> playerIds
    ) {
        return factory.createGame(playerCount, playerIds);
    }
}
