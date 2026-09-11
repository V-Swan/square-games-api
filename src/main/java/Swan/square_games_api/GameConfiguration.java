package Swan.square_games_api;

import fr.le_campus_numerique.square_games.engine.tictactoe.TicTacToeGameFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GameConfiguration {

    @Bean
    public TicTacToeGameFactory ticTacToeGameFactory() {
        return new TicTacToeGameFactory();
    }
}