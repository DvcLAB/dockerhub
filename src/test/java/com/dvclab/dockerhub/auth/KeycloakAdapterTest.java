package com.dvclab.dockerhub.auth;

import com.dvclab.dockerhub.service.ContainerService;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

public class KeycloakAdapterTest {

    @Test
    void deleteResource() throws IOException, URISyntaxException {
        String resource_id = "ed166e5d-80ff-b502-5696-54004748a4bd";
        KeycloakAdapter.getInstance().deleteResource(resource_id);
    }

    @Test
    void exchangeToken() throws IOException, URISyntaxException {
        String token = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJGTU9MU2xvTFZHQzQwRXE4VVROZ3liQXhtUzNrRTRPV21ibExERmY3ZkswIn0.eyJleHAiOjE2MTc4ODg3MDksImlhdCI6MTYxNzg4ODQwOSwiYXV0aF90aW1lIjoxNjE3ODg3NTkyLCJqdGkiOiJlOTJmMGJiNy1hMjcyLTRmYmMtYTc1Ni00ZjNjMGM4OGMxMTEiLCJpc3MiOiJodHRwczovL2F1dGguZHZjbGFiLmNvbS9hdXRoL3JlYWxtcy9EdmNMQUIiLCJhdWQiOiJhY2NvdW50Iiwic3ViIjoiOGY1ZmNiMTktODU1MC00YWQ4LWE2ZTQtMTM2MGZjNTY1OGQ3IiwidHlwIjoiQmVhcmVyIiwiYXpwIjoiZnJvbnRfZW5kIiwibm9uY2UiOiIyYzEyNThjMC02NzhlLTQ2NWQtODNiNS04OWI1YTAyZDZiOWMiLCJzZXNzaW9uX3N0YXRlIjoiN2YzOTBkNzctOTZhOC00OGFjLTk2OWMtMGM1ZGY4M2E3NjJhIiwiYWNyIjoiMCIsImFsbG93ZWQtb3JpZ2lucyI6WyIqIl0sInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJET0NLSFVCX1VTRVIiLCJET0NLSFVCX0FETUlOIl19LCJyZXNvdXJjZV9hY2Nlc3MiOnsiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYWNjb3VudCIsIm1hbmFnZS1hY2NvdW50LWxpbmtzIiwidmlldy1wcm9maWxlIl19fSwic2NvcGUiOiJvcGVuaWQgcHJvZmlsZSBlbWFpbCB1c2VyaW5mbyIsImVtYWlsX3ZlcmlmaWVkIjpmYWxzZSwiYXZhdGFyX3VybCI6Imh0dHBzOi8vdGhpcmR3eC5xbG9nby5jbi9tbW9wZW4vdmlfMzIvTWRXY3JJdVVJVk9Zc3JGQjRYRmJmVXZST2xlYWZ4REo4aWJlMlU5QjNtODFKdXU2N3NUTlE1dlVpYTgzbm1pYWs3M3lYWWJkc2tOcm1nQ002UkptWUxrU3cvMTMyIiwibmFtZSI6Ikxhbmh1aSBMaSIsInByZWZlcnJlZF91c2VybmFtZSI6Imdvb2RuaWdodCIsImdpdmVuX25hbWUiOiJMYW5odWkiLCJmYW1pbHlfbmFtZSI6IkxpIiwiZW1haWwiOiJnb29kbmlnaHRsaWxhbmh1aUBnbWFpbC5jb20iLCJlbmFibGVkIjp0cnVlfQ.Z-KgvJyKFjnvvEwYuzsSkOomMTp9p2rMmy8sAdhmau856WiW8xI3UTHL8OiBG-LtxnZ8vHO6gGtchjIIt2oY8aj8p863-92zg1RmnrxXXFMlWjis8oArhftmDAVxUDv7MiOp4QqdrS6ICgAn0V1lWC6Iuci95Jb7TczXHV4zVeajUGxV9Jtym-auWYAnZVh0Kh6k78HBO0w750a5dlpkElYtwkBem4f7N6CVYLvIyZURM--ib__IMpwZx0Vgtl5SdAmxKVGnmZGuoW8Z0a1MsN2ELkTzHOXHPV33M3BV7t_BtCIj0gcQAViH3udJgDhp1NlpzXZJ67Cvt4_wwXwr6g";
        System.out.println(KeycloakAdapter.getInstance().exchangeToken(token));
    }

    @Test
    void applyResourcePolicy() throws IOException, URISyntaxException {

        String token = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJGTU9MU2xvTFZHQzQwRXE4VVROZ3liQXhtUzNrRTRPV21ibExERmY3ZkswIn0.eyJleHAiOjE2MTc4OTAyNjMsImlhdCI6MTYxNzg4ODQ2MywiYXV0aF90aW1lIjoxNjE3ODg3NTkyLCJqdGkiOiI0MWJkNjI0ZS00MmU4LTQwNTItYjAzNy0yY2JjMTY3NjZlN2IiLCJpc3MiOiJodHRwczovL2F1dGguZHZjbGFiLmNvbS9hdXRoL3JlYWxtcy9EdmNMQUIiLCJhdWQiOlsiYWNjb3VudCIsImR2Y2xhYiJdLCJzdWIiOiI4ZjVmY2IxOS04NTUwLTRhZDgtYTZlNC0xMzYwZmM1NjU4ZDciLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJkdmNsYWIiLCJzZXNzaW9uX3N0YXRlIjoiN2YzOTBkNzctOTZhOC00OGFjLTk2OWMtMGM1ZGY4M2E3NjJhIiwiYWNyIjoiMSIsInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJET0NLSFVCX1VTRVIiLCJET0NLSFVCX0FETUlOIl19LCJyZXNvdXJjZV9hY2Nlc3MiOnsiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYWNjb3VudCIsIm1hbmFnZS1hY2NvdW50LWxpbmtzIiwidmlldy1wcm9maWxlIl19fSwic2NvcGUiOiJwcm9maWxlIGVtYWlsIHVzZXJpbmZvIiwiZW1haWxfdmVyaWZpZWQiOmZhbHNlLCJhdmF0YXJfdXJsIjoiaHR0cHM6Ly90aGlyZHd4LnFsb2dvLmNuL21tb3Blbi92aV8zMi9NZFdjckl1VUlWT1lzckZCNFhGYmZVdlJPbGVhZnhESjhpYmUyVTlCM204MUp1dTY3c1ROUTV2VWlhODNubWlhazczeVhZYmRza05ybWdDTTZSSm1ZTGtTdy8xMzIiLCJuYW1lIjoiTGFuaHVpIExpIiwicHJlZmVycmVkX3VzZXJuYW1lIjoiZ29vZG5pZ2h0IiwiZ2l2ZW5fbmFtZSI6Ikxhbmh1aSIsImZhbWlseV9uYW1lIjoiTGkiLCJlbWFpbCI6Imdvb2RuaWdodGxpbGFuaHVpQGdtYWlsLmNvbSIsImVuYWJsZWQiOnRydWV9.i_SgWgMrL53R51Z8fancQcrA0rP9Z-mtMX-Nr4X__TrfeGgCCMvm9IwsO9dHCvDp8kbif_AsFTeOWp9udTTfzhbC4iveycdtbTqijaBzk0PgxTj9sgecrSyhCrwIbZGhc544ULM8W3E4PlZMgeFAxd7_QdIehdK4qKNxNfsERUUIeqRj_itYQI9KYnExOTQ-SCBf6QEex3Uu-TIBvGw1Q0KliT59kChs4rw4o4zNX0QzxizUrS7vZY7GDHXp4X2ShG4Ti_RgzoF_P36ZdG2NBhJISzUsZA0kzlYQEjSQWelh95EXANFXfYnfjWvaFlGrIII7rPweIkF3ats8WNOk4w";
        String resource_id = new StringBuilder("3101277246277cd6d3d70b435eb9f35e").insert(8, "-")
                .insert(13, "-")
                .insert(18, "-")
                .insert(23, "-").toString();
        ContainerService.ApplyResourcePolicyBody arp_body = new ContainerService.ApplyResourcePolicyBody()
                .withDesc("1"+ "_access_" + "3101277246277cd6d3d70b435eb9f35e")
                .withName("1" + "_access_" + "3101277246277cd6d3d70b435eb9f35e")
                .withScopes(List.of("view"))
                .withUsers(List.of("goodnight"));
        KeycloakAdapter.getInstance().applyResourcePolicy(token, resource_id, arp_body);
    }
}