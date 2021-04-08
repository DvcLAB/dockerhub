package com.dvclab.dockerhub.auth;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;

class KeycloakAdapterTest {

    @Test
    void deleteResource() throws IOException, URISyntaxException {
        String resource_id = "ed166e5d-80ff-b502-5696-54004748a4bd";
        KeycloakAdapter.getInstance().deleteResource(resource_id);
    }
}