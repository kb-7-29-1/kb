package com.salgosipo.safety.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salgosipo.safety.domain.PedestrianRoute;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SafetyRouteClientTest {

    @Test
    void parsesLineStringAndSummaryProperties() throws Exception {
        String response = """
                {
                  "type": "FeatureCollection",
                  "features": [
                    {
                      "type": "Feature",
                      "properties": {
                        "totalDistance": 620,
                        "totalTime": 480
                      },
                      "geometry": {
                        "type": "Point",
                        "coordinates": [127.0700, 37.5500]
                      }
                    },
                    {
                      "type": "Feature",
                      "properties": {},
                      "geometry": {
                        "type": "LineString",
                        "coordinates": [
                          [127.0700, 37.5500],
                          [127.0710, 37.5510],
                          [127.0720, 37.5520]
                        ]
                      }
                    }
                  ]
                }
                """;

        SafetyRouteClient client = new SafetyRouteClient(
                "test-key",
                new RestTemplate(),
                new ObjectMapper()
        );

        PedestrianRoute route = client.parseRoute(
                response,
                new SafetyRouteClient.RouteOption("0", "추천")
        );

        assertNotNull(route);
        assertEquals(620, route.getDistanceMeters());
        assertEquals(480, route.getTotalTimeSeconds());
        assertEquals(3, route.getRoutePoints().size());
        assertEquals(37.5500, route.getRoutePoints().get(0).getLatitude(), 1e-9);
        assertEquals(127.0720, route.getRoutePoints().get(2).getLongitude(), 1e-9);
    }
}
