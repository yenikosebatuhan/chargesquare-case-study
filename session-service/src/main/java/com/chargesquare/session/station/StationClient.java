package com.chargesquare.session.station;

import com.chargesquare.session.error.ConflictException;
import com.chargesquare.session.error.NotFoundException;
import com.chargesquare.session.error.UpstreamException;
import com.chargesquare.session.security.ServiceTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * The real synchronous REST boundary from Session Service to Station Service.
 * Every call goes over the network (RestClient), never an in-process shortcut.
 * If Station Service is unreachable we fail fast with 502 (see UpstreamException).
 */
@Component
public class StationClient {

    private static final Logger log = LoggerFactory.getLogger(StationClient.class);

    private final RestClient restClient;
    private final ServiceTokenProvider tokenProvider;

    public StationClient(@Value("${station.service.url}") String baseUrl,
                         ServiceTokenProvider tokenProvider) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.tokenProvider = tokenProvider;
        log.info("Station Service base URL = {}", baseUrl);
    }

    /** Read a connector's status + tariff. Maps a 404 to CONNECTOR_NOT_FOUND. */
    public ConnectorView getConnector(Long connectorId) {
        try {
            return restClient.get()
                    .uri("/connectors/{id}", connectorId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.bearerToken())
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (req, res) -> {
                        throw new NotFoundException("CONNECTOR_NOT_FOUND",
                                "Connector " + connectorId + " does not exist");
                    })
                    .body(ConnectorView.class);
        } catch (ResourceAccessException ex) {
            throw unreachable(ex);
        }
    }

    /**
     * Flip the connector to OCCUPIED for a given user. Passing the user lets Station honour a
     * reservation the same user holds. Maps a 409 to CONNECTOR_OCCUPIED / CONNECTOR_RESERVED.
     */
    public void occupy(Long connectorId, Long userId) {
        try {
            restClient.post()
                    .uri("/connectors/{id}/occupy", connectorId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.bearerToken())
                    .body(new OccupyRequest(userId))
                    .retrieve()
                    .onStatus(status -> status.value() == 409, (req, res) -> {
                        throw new ConflictException("CONNECTOR_OCCUPIED",
                                "Connector " + connectorId + " is not AVAILABLE");
                    })
                    .onStatus(status -> status.value() == 404, (req, res) -> {
                        throw new NotFoundException("CONNECTOR_NOT_FOUND",
                                "Connector " + connectorId + " does not exist");
                    })
                    .toBodilessEntity();
        } catch (ResourceAccessException ex) {
            throw unreachable(ex);
        }
    }

    /** Return the connector to AVAILABLE. */
    public void release(Long connectorId) {
        try {
            restClient.post()
                    .uri("/connectors/{id}/release", connectorId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.bearerToken())
                    .retrieve()
                    .toBodilessEntity();
        } catch (ResourceAccessException ex) {
            throw unreachable(ex);
        }
    }

    /** Reserve a connector for a user (stretch goal); maps 404/409 to clear errors. */
    public ConnectorView reserve(Long connectorId, Long userId, Integer ttlSeconds) {
        try {
            return restClient.post()
                    .uri("/connectors/{id}/reserve", connectorId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.bearerToken())
                    .body(new ReserveRequest(userId, ttlSeconds))
                    .retrieve()
                    .onStatus(status -> status.value() == 409, (req, res) -> {
                        throw new ConflictException("CONNECTOR_UNAVAILABLE",
                                "Connector " + connectorId + " cannot be reserved");
                    })
                    .onStatus(status -> status.value() == 404, (req, res) -> {
                        throw new NotFoundException("CONNECTOR_NOT_FOUND",
                                "Connector " + connectorId + " does not exist");
                    })
                    .body(ConnectorView.class);
        } catch (ResourceAccessException ex) {
            throw unreachable(ex);
        }
    }

    /** List a station's connectors (used by the stuck-connector reaper). */
    public List<ConnectorView> listConnectors(Long stationId) {
        try {
            return restClient.get()
                    .uri("/stations/{id}/connectors", stationId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.bearerToken())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<ConnectorView>>() {});
        } catch (ResourceAccessException ex) {
            throw unreachable(ex);
        }
    }

    private UpstreamException unreachable(Exception cause) {
        log.error("Station Service unreachable: {}", cause.getMessage());
        return new UpstreamException("Station Service is unreachable");
    }

    public record OccupyRequest(Long userId) {
    }

    public record ReserveRequest(Long userId, Integer ttlSeconds) {
    }
}

