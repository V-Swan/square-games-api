package fr.squaregames.api.controller;

import fr.squaregames.api.plugin.GamePlugin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/games")
public class GameCatalogController {

    private final List<GamePlugin> plugins;

    public GameCatalogController(List<GamePlugin> plugins) {
        this.plugins = plugins;
    }

    @GetMapping
    public List<Map<String, String>> getAvailableGames(Locale locale) {

        return plugins.stream()
                .map(plugin -> Map.of(
                        "id", plugin.getId(),
                        "name", plugin.getName(locale)
                ))
                .toList();
    }
}
