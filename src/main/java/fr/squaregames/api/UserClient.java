package fr.squaregames.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class UserClient {

    private final RestClient restClient;
    private final String usersApiUrl;

    public UserClient(
            RestClient restClient,
            @Value("${users.api.url}") String usersApiUrl
    ) {
        this.restClient = restClient;
        this.usersApiUrl = usersApiUrl;
    }

    public boolean isUserValid(Long userId) {
        Boolean result = restClient
                .get()
                .uri(usersApiUrl + "/users/{id}/valid", userId)
                .retrieve()
                .body(Boolean.class);

        return Boolean.TRUE.equals(result);
    }
}