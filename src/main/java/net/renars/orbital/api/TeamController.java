package net.renars.orbital.api;

import net.renars.orbital.services.TeamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Komandu (publiskais) API.
 * --Renars
 */
@RestController
@RequestMapping("/team")
public class TeamController implements Controller {
    private final TeamRepository teamService;

    @Autowired
    public TeamController(TeamRepository teamService) {
        this.teamService = teamService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> createUser(
            @RequestBody Void request
    ) {
        return ok("");
    }
}
