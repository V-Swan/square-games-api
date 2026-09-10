package Swan.square_games_api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
public class HeartbeatController {

    @Autowired
    private HeartbeatSensor heartbeatSensor;
    @GetMapping("/heartbeat")
    int getHeartbeatSensor() {
        return heartbeatSensor.get();
    }
}