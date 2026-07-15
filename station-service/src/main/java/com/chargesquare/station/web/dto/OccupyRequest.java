package com.chargesquare.station.web.dto;

/** Optional body for occupy: the user starting the session, used to honour a reservation. */
public record OccupyRequest(Long userId) {
}
