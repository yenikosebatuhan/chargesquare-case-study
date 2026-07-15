package com.chargesquare.station.domain;

/**
 * Connector lifecycle status.
 * <ul>
 *   <li>AVAILABLE — free to use.</li>
 *   <li>RESERVED — a short-lived hold for one user before a session starts (stretch goal).</li>
 *   <li>OCCUPIED — in use by an active session.</li>
 * </ul>
 */
public enum ConnectorStatus {
    AVAILABLE,
    RESERVED,
    OCCUPIED
}
