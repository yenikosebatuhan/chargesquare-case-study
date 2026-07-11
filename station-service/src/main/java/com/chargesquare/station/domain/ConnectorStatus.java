package com.chargesquare.station.domain;

/** A connector is either free to use or in use by an active session. */
public enum ConnectorStatus {
    AVAILABLE,
    OCCUPIED
}
