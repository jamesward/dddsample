package se.citerus.dddsample.domain;

import junit.framework.TestCase;
import static se.citerus.dddsample.domain.SampleLocations.*;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

public class CargoTest extends TestCase {

  // TODO:
  // it seems that events are not added to the system by cargo.deliveryHistory().addEvent(),
  // but rather new HandlingEvents are stored and associated with its cargo. This test should
  // work against the repositories or the service layer. The delivery history of a cargo should be
  // read-only from the cargo end. // PeBa


  public void testlastKnownLocationUnknownWhenNoEvents() throws Exception {
    Cargo cargo = new Cargo(new TrackingId("XYZ"), STOCKHOLM, MELBOURNE);

    assertEquals(Location.UNKNOWN, cargo.lastKnownLocation());
  }

  public void testlastKnownLocationReceived() throws Exception {
    Cargo cargo = populateCargoReceivedStockholm();

    assertEquals(STOCKHOLM, cargo.lastKnownLocation());
  }

  public void testlastKnownLocationClaimed() throws Exception {
    Cargo cargo = populateCargoClaimedMelbourne();

    assertEquals(MELBOURNE, cargo.lastKnownLocation());
  }

  public void testlastKnownLocationUnloaded() throws Exception {
    Cargo cargo = populateCargoOffHongKong();

    assertEquals(HONGKONG, cargo.lastKnownLocation());
  }

  public void testlastKnownLocationloaded() throws Exception {
    Cargo cargo = populateCargoOnHamburg();

    assertEquals(HAMBURG, cargo.lastKnownLocation());
  }

  public void testAtFinalLocation() throws Exception {
    Cargo cargo = populateCargoOffMelbourne();

    assertTrue(cargo.hasArrived());
  }

  public void testNotAtFinalLocationWhenNotUnloaded() throws Exception {
    Cargo cargo = populateCargoOnHongKong();

    assertFalse(cargo.hasArrived());
  }

  public void testEquality() throws Exception {
    Cargo c1 = new Cargo(new TrackingId("ABC"), STOCKHOLM, HONGKONG);
    Cargo c2 = new Cargo(new TrackingId("CBA"), STOCKHOLM, HONGKONG);
    Cargo c3 = new Cargo(new TrackingId("ABC"), STOCKHOLM, MELBOURNE);
    Cargo c4 = new Cargo(new TrackingId("ABC"), STOCKHOLM, HONGKONG);

    assertTrue("Cargos should be equal when TrackingIDs are equal", c1.equals(c4));
    assertTrue("Cargos should be equal when TrackingIDs are equal", c1.equals(c3));
    assertTrue("Cargos should be equal when TrackingIDs are equal", c3.equals(c4));
    assertFalse("Cargos are not equal when TrackingID differ", c1.equals(c2));
  }

  public void testIsUnloadedAtFinalDestination() throws Exception {
    assertFalse(new Cargo().isUnloadedAtDestination());

    Cargo cargo = setUpCargoWithItinerary(HANGZOU, TOKYO, NEWYORK);
    assertFalse(cargo.isUnloadedAtDestination());

    // Adding an event unrelated to unloading at final destination
    cargo.deliveryHistory().addEvent(
      new HandlingEvent(cargo, new Date(), new Date(), HandlingEvent.Type.RECEIVE, HANGZOU, null));
    assertFalse(cargo.isUnloadedAtDestination());

    CarrierMovement cm1 = new CarrierMovement(new CarrierMovementId("CM1"), HANGZOU, NEWYORK);

    // Adding an unload event, but not at the final destination
    cargo.deliveryHistory().addEvent(
      new HandlingEvent(cargo, new Date(), new Date(), HandlingEvent.Type.UNLOAD, TOKYO, cm1));
    assertFalse(cargo.isUnloadedAtDestination());

    // Adding an event in the final destination, but not unload
    cargo.deliveryHistory().addEvent(
      new HandlingEvent(cargo, new Date(), new Date(), HandlingEvent.Type.CUSTOMS, NEWYORK, null));
    assertFalse(cargo.isUnloadedAtDestination());

    // Finally, cargo is unloaded at final destination
    cargo.deliveryHistory().addEvent(
      new HandlingEvent(cargo, new Date(), new Date(), HandlingEvent.Type.UNLOAD, NEWYORK, cm1));
    assertTrue(cargo.isUnloadedAtDestination());
  }

  /* TODO implement nextExpectedEvent
  public void testNextExpectedEvent() {
    Cargo cargo = setUpCargoWithItinerary(HANGZOU, TOKYO, NEWYORK);
    CarrierMovementId cmid = new CarrierMovementId("CM1");
    CarrierMovement cm1 = new CarrierMovement(cmid, HANGZOU, TOKYO);
    CarrierMovement cm2 = new CarrierMovement(cmid, TOKYO, NEWYORK);

    HandlingEvent event1 = new HandlingEvent(cargo, new Date(), new Date(), HandlingEvent.Type.RECEIVE, HANGZOU, null);

    assertEquals(event1, cargo.nextExpectedEvent());

    cargo.deliveryHistory().addEvent(event1);

    HandlingEvent event2 = new HandlingEvent(cargo, new Date(), new Date(), HandlingEvent.Type.LOAD, HANGZOU, cm1);

    assertEquals(event2, cargo.nextExpectedEvent());

    cargo.deliveryHistory().addEvent(event2);
  }
  */


  // TODO: Generate test data some better way
  private Cargo populateCargoReceivedStockholm() throws Exception {
    final Cargo cargo = new Cargo(new TrackingId("XYZ"), STOCKHOLM, MELBOURNE);

    HandlingEvent he = new HandlingEvent(cargo, getDate("2007-12-01"), new Date(), HandlingEvent.Type.RECEIVE, STOCKHOLM, null);
    cargo.deliveryHistory().addEvent(he);

    return cargo;
  }

  private Cargo populateCargoClaimedMelbourne() throws Exception {
    final Cargo cargo = populateCargoOffMelbourne();

    cargo.deliveryHistory().addEvent(new HandlingEvent(cargo, getDate("2007-12-09"), new Date(), HandlingEvent.Type.CLAIM, MELBOURNE, null));

    return cargo;
  }

  private Cargo populateCargoOffHongKong() throws Exception {
    final Cargo cargo = new Cargo(new TrackingId("XYZ"), STOCKHOLM, MELBOURNE);

    final CarrierMovement stockholmToHamburg = new CarrierMovement(
       new CarrierMovementId("CAR_001"), STOCKHOLM, HAMBURG);

    cargo.deliveryHistory().addEvent(new HandlingEvent(cargo, getDate("2007-12-01"), new Date(), HandlingEvent.Type.LOAD, STOCKHOLM, stockholmToHamburg));
    cargo.deliveryHistory().addEvent(new HandlingEvent(cargo, getDate("2007-12-02"), new Date(), HandlingEvent.Type.UNLOAD, HAMBURG, stockholmToHamburg));

    final CarrierMovement hamburgToHongKong = new CarrierMovement(
       new CarrierMovementId("CAR_001"), HAMBURG, HONGKONG);

    cargo.deliveryHistory().addEvent(new HandlingEvent(cargo, getDate("2007-12-03"), new Date(), HandlingEvent.Type.LOAD, HAMBURG, hamburgToHongKong));
    cargo.deliveryHistory().addEvent(new HandlingEvent(cargo, getDate("2007-12-04"), new Date(), HandlingEvent.Type.UNLOAD, HONGKONG, hamburgToHongKong));

    return cargo;
  }

  private Cargo populateCargoOnHamburg() throws Exception {
    final Cargo cargo = new Cargo(new TrackingId("XYZ"), STOCKHOLM, MELBOURNE);

    final CarrierMovement stockholmToHamburg = new CarrierMovement(
       new CarrierMovementId("CAR_001"), STOCKHOLM, HAMBURG);

    cargo.deliveryHistory().addEvent(new HandlingEvent(cargo, getDate("2007-12-01"), new Date(), HandlingEvent.Type.LOAD, STOCKHOLM, stockholmToHamburg));
    cargo.deliveryHistory().addEvent(new HandlingEvent(cargo, getDate("2007-12-02"), new Date(), HandlingEvent.Type.UNLOAD, HAMBURG, stockholmToHamburg));

    final CarrierMovement hamburgToHongKong = new CarrierMovement(
       new CarrierMovementId("CAR_001"), HAMBURG, HONGKONG);

    cargo.deliveryHistory().addEvent(new HandlingEvent(cargo, getDate("2007-12-03"), new Date(), HandlingEvent.Type.LOAD, HAMBURG, hamburgToHongKong));

    return cargo;
  }

  private Cargo populateCargoOffMelbourne() throws Exception {
    final Cargo cargo = new Cargo(new TrackingId("XYZ"), STOCKHOLM, MELBOURNE);

    final CarrierMovement stockholmToHamburg = new CarrierMovement(
       new CarrierMovementId("CAR_001"), STOCKHOLM, HAMBURG);

    cargo.deliveryHistory().addEvent(new HandlingEvent(cargo, getDate("2007-12-01"), new Date(), HandlingEvent.Type.LOAD, STOCKHOLM, stockholmToHamburg));
    cargo.deliveryHistory().addEvent(new HandlingEvent(cargo, getDate("2007-12-02"), new Date(), HandlingEvent.Type.UNLOAD, HAMBURG, stockholmToHamburg));

    final CarrierMovement hamburgToHongKong = new CarrierMovement(
       new CarrierMovementId("CAR_001"), HAMBURG, HONGKONG);

    cargo.deliveryHistory().addEvent(new HandlingEvent(cargo, getDate("2007-12-03"), new Date(), HandlingEvent.Type.LOAD, HAMBURG, hamburgToHongKong));
    cargo.deliveryHistory().addEvent(new HandlingEvent(cargo, getDate("2007-12-04"), new Date(), HandlingEvent.Type.UNLOAD, HONGKONG, hamburgToHongKong));

    final CarrierMovement hongKongToMelbourne = new CarrierMovement(
       new CarrierMovementId("CAR_001"), HONGKONG, MELBOURNE);

    cargo.deliveryHistory().addEvent(new HandlingEvent(cargo, getDate("2007-12-05"), new Date(), HandlingEvent.Type.LOAD, HONGKONG, hongKongToMelbourne));
    cargo.deliveryHistory().addEvent(new HandlingEvent(cargo, getDate("2007-12-07"), new Date(), HandlingEvent.Type.UNLOAD, MELBOURNE, hongKongToMelbourne));

    return cargo;
  }

  private Cargo populateCargoOnHongKong() throws Exception {
    final Cargo cargo = new Cargo(new TrackingId("XYZ"), STOCKHOLM, MELBOURNE);

    final CarrierMovement stockholmToHamburg = new CarrierMovement(
       new CarrierMovementId("CAR_001"), STOCKHOLM, HAMBURG);

    cargo.deliveryHistory().addEvent(new HandlingEvent(cargo, getDate("2007-12-01"), new Date(), HandlingEvent.Type.LOAD, STOCKHOLM, stockholmToHamburg));
    cargo.deliveryHistory().addEvent(new HandlingEvent(cargo, getDate("2007-12-02"), new Date(), HandlingEvent.Type.UNLOAD, HAMBURG, stockholmToHamburg));

    final CarrierMovement hamburgToHongKong = new CarrierMovement(
       new CarrierMovementId("CAR_001"), HAMBURG, HONGKONG);

    cargo.deliveryHistory().addEvent(new HandlingEvent(cargo, getDate("2007-12-03"), new Date(), HandlingEvent.Type.LOAD, HAMBURG, hamburgToHongKong));
    cargo.deliveryHistory().addEvent(new HandlingEvent(cargo, getDate("2007-12-04"), new Date(), HandlingEvent.Type.UNLOAD, HONGKONG, hamburgToHongKong));

    final CarrierMovement hongKongToMelbourne = new CarrierMovement(
       new CarrierMovementId("CAR_001"), HONGKONG, MELBOURNE);

    cargo.deliveryHistory().addEvent(new HandlingEvent(cargo, getDate("2007-12-05"), new Date(), HandlingEvent.Type.LOAD, HONGKONG, hongKongToMelbourne));

    return cargo;
  }

  public void testIsMisdirected() throws Exception {
    //A cargo with no itinerary is not misdirected
    Cargo cargo = new Cargo(new TrackingId("TRKID"), SHANGHAI, GOTHENBURG);
    assertFalse(cargo.isMisdirected());

    cargo = setUpCargoWithItinerary(SHANGHAI, ROTTERDAM, GOTHENBURG);

    //A cargo with no handling events is not misdirected
    assertFalse(cargo.isMisdirected());

    Collection<HandlingEvent> handlingEvents = new ArrayList<HandlingEvent>();

    CarrierMovement abc = new CarrierMovement(new CarrierMovementId("ABC"), SHANGHAI, ROTTERDAM);
    CarrierMovement def = new CarrierMovement(new CarrierMovementId("DEF"), ROTTERDAM, GOTHENBURG);
    CarrierMovement ghi = new CarrierMovement(new CarrierMovementId("GHI"), ROTTERDAM, NEWYORK);

    //Happy path
    handlingEvents.add(new HandlingEvent(cargo, new Date(10), new Date(20), HandlingEvent.Type.RECEIVE, SHANGHAI, null));
    handlingEvents.add(new HandlingEvent(cargo, new Date(30), new Date(40), HandlingEvent.Type.LOAD, SHANGHAI, abc));
    handlingEvents.add(new HandlingEvent(cargo, new Date(50), new Date(60), HandlingEvent.Type.UNLOAD, ROTTERDAM, abc));
    handlingEvents.add(new HandlingEvent(cargo, new Date(70), new Date(80), HandlingEvent.Type.LOAD, ROTTERDAM, def));
    handlingEvents.add(new HandlingEvent(cargo, new Date(90), new Date(100), HandlingEvent.Type.UNLOAD, GOTHENBURG, def));
    handlingEvents.add(new HandlingEvent(cargo, new Date(110), new Date(120), HandlingEvent.Type.CLAIM, GOTHENBURG, null));
    handlingEvents.add(new HandlingEvent(cargo, new Date(130), new Date(140), HandlingEvent.Type.CUSTOMS, GOTHENBURG, null));

    cargo.deliveryHistory().addAllEvents(handlingEvents);
    assertFalse(cargo.isMisdirected());

    //Try a couple of failing ones

    cargo = setUpCargoWithItinerary(SHANGHAI, ROTTERDAM, GOTHENBURG);
    handlingEvents = new ArrayList<HandlingEvent>();

    handlingEvents.add(new HandlingEvent(cargo, new Date(), new Date(), HandlingEvent.Type.RECEIVE, HANGZOU, null));
    cargo.deliveryHistory().addAllEvents(handlingEvents);
    assertTrue(cargo.isMisdirected());


    cargo = setUpCargoWithItinerary(SHANGHAI, ROTTERDAM, GOTHENBURG);
    handlingEvents = new ArrayList<HandlingEvent>();

    handlingEvents.add(new HandlingEvent(cargo, new Date(10), new Date(20), HandlingEvent.Type.RECEIVE, SHANGHAI, null));
    handlingEvents.add(new HandlingEvent(cargo, new Date(30), new Date(40), HandlingEvent.Type.LOAD, SHANGHAI, abc));
    handlingEvents.add(new HandlingEvent(cargo, new Date(50), new Date(60), HandlingEvent.Type.UNLOAD, ROTTERDAM, abc));
    handlingEvents.add(new HandlingEvent(cargo, new Date(70), new Date(80), HandlingEvent.Type.LOAD, ROTTERDAM, ghi));

    cargo.deliveryHistory().addAllEvents(handlingEvents);
    assertTrue(cargo.isMisdirected());


    cargo = setUpCargoWithItinerary(SHANGHAI, ROTTERDAM, GOTHENBURG);
    handlingEvents = new ArrayList<HandlingEvent>();

    handlingEvents.add(new HandlingEvent(cargo, new Date(10), new Date(20), HandlingEvent.Type.RECEIVE, SHANGHAI, null));
    handlingEvents.add(new HandlingEvent(cargo, new Date(30), new Date(40), HandlingEvent.Type.LOAD, SHANGHAI, abc));
    handlingEvents.add(new HandlingEvent(cargo, new Date(50), new Date(60), HandlingEvent.Type.UNLOAD, ROTTERDAM, abc));
    handlingEvents.add(new HandlingEvent(cargo, new Date(), new Date(), HandlingEvent.Type.CLAIM, ROTTERDAM, null));

    cargo.deliveryHistory().addAllEvents(handlingEvents);
    assertTrue(cargo.isMisdirected());
  }

  private Cargo setUpCargoWithItinerary(Location origin, Location midpoint, Location destination) {
    Cargo cargo = new Cargo(new TrackingId("CARGO1"), origin, destination);

    Itinerary itinerary = new Itinerary(
       new Leg(new CarrierMovementId("ABC"), origin, midpoint),
       new Leg(new CarrierMovementId("DEF"), midpoint, destination)
    );

    cargo.setItinerary(itinerary);
    return cargo;
  }

  /**
   * Parse an ISO 8601 (YYYY-MM-DD) String to Date
   *
   * @param isoFormat String to parse.
   * @return Created date instance.
   * @throws ParseException Thrown if parsing fails.
   */
  private Date getDate(String isoFormat) throws ParseException {
    final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    return dateFormat.parse(isoFormat);
  }
}
