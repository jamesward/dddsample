package se.citerus.dddsample.tracking.core.domain.model.handling;

import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import se.citerus.dddsample.tracking.core.domain.model.cargo.Cargo;
import se.citerus.dddsample.tracking.core.domain.model.location.Location;
import se.citerus.dddsample.tracking.core.domain.model.shared.HandlingActivity;
import se.citerus.dddsample.tracking.core.domain.model.shared.HandlingActivityType;
import se.citerus.dddsample.tracking.core.domain.model.voyage.Voyage;
import se.citerus.dddsample.tracking.core.domain.patterns.domainevent.DomainEvent;

import java.util.Date;

/**
 * A HandlingEvent is used to register the event when, for instance,
 * a cargo is unloaded from a carrier at a some loacation at a given time.
 * <p/>
 * The HandlingEvent's are sent from different Incident Logging Applications
 * some time after the event occured and contain information about the
 * {@link se.citerus.dddsample.tracking.core.domain.model.cargo.TrackingId}, {@link se.citerus.dddsample.tracking.core.domain.model.location.Location}, timestamp of the completion of the event,
 * and possibly, if applicable a {@link se.citerus.dddsample.tracking.core.domain.model.voyage.Voyage}.
 * <p/>
 * This class is the only member, and consequently the root, of the HandlingEvent aggregate.
 * <p/>
 * HandlingEvent's could contain information about a {@link Voyage} and if so,
 * the event type must be either {@link se.citerus.dddsample.tracking.core.domain.model.shared.HandlingActivityType#LOAD} or {@link se.citerus.dddsample.tracking.core.domain.model.shared.HandlingActivityType#UNLOAD}.
 * <p/>
 * All other events must be of {@link se.citerus.dddsample.tracking.core.domain.model.shared.HandlingActivityType#RECEIVE}, {@link se.citerus.dddsample.tracking.core.domain.model.shared.HandlingActivityType#CLAIM} or {@link se.citerus.dddsample.tracking.core.domain.model.shared.HandlingActivityType#CUSTOMS}.
 */
public final class HandlingEvent implements DomainEvent<HandlingEvent> {

  private EventSequenceNumber sequenceNumber;
  private HandlingActivity activity;
  private Date completionTime;
  private Date registrationTime;
  private Cargo cargo;
  private OperatorCode operatorCode;

  /**
   * @param cargo            cargo
   * @param completionTime   completion time, the reported time that the event actually happened (e.g. the receive took place).
   * @param registrationTime registration time, the time the message is received
   * @param type             type of event
   * @param location         where the event took place
   * @param voyage           the voyage
   * @param operatorCode     operator code for port operator
   */
  HandlingEvent(final Cargo cargo,
                final Date completionTime,
                final Date registrationTime,
                final HandlingActivityType type,
                final Location location,
                final Voyage voyage,
                final OperatorCode operatorCode) {
    Validate.notNull(cargo, "Cargo is required");
    Validate.notNull(completionTime, "Completion time is required");
    Validate.notNull(registrationTime, "Registration time is required");
    Validate.notNull(type, "Handling event type is required");
    Validate.notNull(location, "Location is required");
    Validate.notNull(voyage, "Voyage is required");
    Validate.notNull(operatorCode, "Operator code is required");

    if (!type.isVoyageRelated()) {
      throw new IllegalArgumentException("Voyage is not allowed with event type " + type);
    }

    this.sequenceNumber = EventSequenceNumber.next();
    this.cargo = cargo;
    this.completionTime = new Date(completionTime.getTime());
    this.registrationTime = new Date(registrationTime.getTime());
    this.activity = new HandlingActivity(type, location, voyage);
    this.operatorCode = operatorCode;
  }

  /**
   * @param cargo            cargo
   * @param completionTime   completion time, the reported time that the event actually happened (e.g. the receive took place).
   * @param registrationTime registration time, the time the message is received
   * @param type             type of event
   * @param location         where the event took place
   */
  // TODO make package local
  public HandlingEvent(final Cargo cargo,
                       final Date completionTime,
                       final Date registrationTime,
                       final HandlingActivityType type,
                       final Location location) {
    Validate.notNull(cargo, "Cargo is required");
    Validate.notNull(completionTime, "Completion time is required");
    Validate.notNull(registrationTime, "Registration time is required");
    Validate.notNull(type, "Handling event type is required");
    Validate.notNull(location, "Location is required");

    if (type.isVoyageRelated()) {
      throw new IllegalArgumentException("Voyage is required for event type " + type);
    }

    this.sequenceNumber = EventSequenceNumber.next();
    this.completionTime = new Date(completionTime.getTime());
    this.registrationTime = new Date(registrationTime.getTime());
    this.cargo = cargo;
    this.activity = new HandlingActivity(type, location);
  }

  public EventSequenceNumber sequenceNumber() {
    return sequenceNumber;
  }

  public HandlingActivity activity() {
    return activity;
  }

  public HandlingActivityType type() {
    return activity.type();
  }

  public Voyage voyage() {
    return activity.voyage() != null ? activity.voyage() : Voyage.NONE;
  }

  public OperatorCode operatorCode() {
    return operatorCode;
  }

  public Date completionTime() {
    return new Date(this.completionTime.getTime());
  }

  public Date registrationTime() {
    return new Date(this.registrationTime.getTime());
  }

  public Location location() {
    return activity.location();
  }

  public Cargo cargo() {
    return this.cargo;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final HandlingEvent event = (HandlingEvent) o;

    return sameEventAs(event);
  }

  @Override
  public boolean sameEventAs(final HandlingEvent other) {
    return other != null && new EqualsBuilder().
      append(this.cargo, other.cargo).
      append(this.completionTime, other.completionTime).
      append(this.activity, other.activity).
      isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().
      append(cargo).
      append(completionTime).
      append(activity).
      toHashCode();
  }

  @Override
  public String toString() {
    return "Cargo: " + cargo +
      "\nActivity: " + activity +
      "\nCompleted on: " + completionTime +
      "\nRegistered on: " + registrationTime;
  }

  HandlingEvent() {
    // Needed by Hibernate
  }


  // Auto-generated surrogate key
  @SuppressWarnings("UnusedDeclaration")
  private Long id;

}
