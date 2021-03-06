package se.citerus.dddsample.tracking.core.domain.model.handling;

import se.citerus.dddsample.tracking.core.domain.model.cargo.TrackingId;

/**
 * Thrown when trying to register an event with an unknown tracking id.
 */
public final class UnknownCargoException extends CannotCreateHandlingEventException {

  private final TrackingId trackingId;

  /**
   * @param trackingId cargo tracking id
   */
  public UnknownCargoException(final TrackingId trackingId) {
    this.trackingId = trackingId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getMessage() {
    return "No cargo with tracking id " + trackingId.stringValue() + " exists in the system";
  }
}
