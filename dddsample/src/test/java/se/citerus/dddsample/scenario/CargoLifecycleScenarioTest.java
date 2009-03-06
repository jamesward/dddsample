package se.citerus.dddsample.scenario;

import junit.framework.TestCase;
import se.citerus.dddsample.application.ApplicationEvents;
import se.citerus.dddsample.application.BookingService;
import se.citerus.dddsample.application.CargoInspectionService;
import se.citerus.dddsample.application.HandlingEventService;
import se.citerus.dddsample.application.impl.BookingServiceImpl;
import se.citerus.dddsample.application.impl.CargoInspectionServiceImpl;
import se.citerus.dddsample.application.impl.HandlingEventServiceImpl;
import se.citerus.dddsample.domain.model.cargo.*;
import static se.citerus.dddsample.domain.model.cargo.TransportStatus.*;
import se.citerus.dddsample.domain.model.handling.HandlingEvent;
import static se.citerus.dddsample.domain.model.handling.HandlingEvent.Type.*;
import se.citerus.dddsample.domain.model.handling.HandlingEventFactory;
import se.citerus.dddsample.domain.model.handling.HandlingEventRepository;
import se.citerus.dddsample.domain.model.location.Location;
import se.citerus.dddsample.domain.model.location.LocationRepository;
import static se.citerus.dddsample.domain.model.location.SampleLocations.*;
import se.citerus.dddsample.domain.model.location.UnLocode;
import static se.citerus.dddsample.domain.model.voyage.SampleVoyages.*;
import static se.citerus.dddsample.domain.model.voyage.Voyage.NONE;
import se.citerus.dddsample.domain.model.voyage.VoyageNumber;
import se.citerus.dddsample.domain.model.voyage.VoyageRepository;
import se.citerus.dddsample.domain.service.RoutingService;
import se.citerus.dddsample.infrastructure.messaging.stub.SynchronousApplicationEventsStub;
import se.citerus.dddsample.infrastructure.persistence.inmemory.CargoRepositoryInMem;
import se.citerus.dddsample.infrastructure.persistence.inmemory.LocationRepositoryInMem;
import se.citerus.dddsample.infrastructure.persistence.inmemory.VoyageRepositoryInMem;

import java.util.*;

public class CargoLifecycleScenarioTest extends TestCase {

  /**
   * Repository implementations are part of the infrastructure layer,
   * which in this test is stubbed out by in-memory replacements.
   */
  HandlingEventRepository handlingEventRepository;
  CargoRepository cargoRepository;
  LocationRepository locationRepository;
  VoyageRepository voyageRepository;

  /**
   * This interface is part of the application layer,
   * and defines a number of events that occur during
   * aplication execution. It is used for message-driving
   * and is implemented using JMS.
   *
   * In this test it is stubbed with synchronous calls.
   */
  ApplicationEvents applicationEvents;

  /**
   * These three components all belong to the application layer,
   * and map against use cases of the application. The "real"
   * implementations are used in this lifecycle test,
   * but wired with stubbed infrastructure.
   */
  BookingService bookingService;
  HandlingEventService handlingEventService;
  CargoInspectionService cargoInspectionService;

  /**
   * This factory is part of the handling aggregate and belongs to
   * the domain layer. Similar to the application layer components,
   * the "real" implementation is used here too,
   * wired with stubbed infrastructure.
   */
  HandlingEventFactory handlingEventFactory;

  /**
   * This is a domain service interface, whose implementation
   * is part of the infrastructure layer (remote call to external system).
   *
   * It is stubbed in this test.
   */
  RoutingService routingService;


  public void testCargoFromHongkongToStockholm() throws Exception {
    /* Test setup: A cargo should be shipped from Hongkong to Stockholm,
       and it should arrive in no more than two weeks. */
    Location origin = HONGKONG;
    Location destination = STOCKHOLM;
    Date arrivalDeadline = inTwoWeeks();

    /* Use case 1: booking

       A new cargo is booked, and the unique tracking id is assigned to the cargo. */
    TrackingId trackingId = bookingService.bookNewCargo(
      origin.unLocode(), destination.unLocode(), arrivalDeadline
    );

    /* The tracking id can be used to lookup the cargo in the repository.

       Important: The cargo, and thus the domain model, is responsible for determining
       the status of the cargo, whether it is on the right track or not and so on.
       This is core domain logic.

       Tracking the cargo basically amounts to presenting information extracted from
       the cargo aggregate in a suitable way. */
    Cargo cargo = cargoRepository.find(trackingId);
    assertNotNull(cargo);
    assertEquals(NOT_RECEIVED, cargo.delivery().transportStatus());
    assertEquals(RoutingStatus.NOT_ROUTED, cargo.routingStatus());
    assertFalse(cargo.isMisdirected());
    assertNull(cargo.estimatedTimeOfArrival());
    assertEquals(HandlingActivity.NONE, cargo.nextExpectedActivity());

    /* Use case 2: routing

       A number of possible routes for this cargo is requested and may be
       presented to the customer in some way for him/her to choose from.
       Selection could be affected by things like price and time of delivery,
       but this test simply uses an arbitrary selection to mimic that process.

       The cargo is then assigned to the selected route, described by an itinerary. */
    List<Itinerary> itineraries = bookingService.requestPossibleRoutesForCargo(trackingId);
    Itinerary itinerary = selectPreferedItinerary(itineraries);
    cargo.assignToRoute(itinerary);

    assertEquals(RoutingStatus.ROUTED, cargo.routingStatus());
    assertNotNull(cargo.estimatedTimeOfArrival());
    assertEquals(new HandlingActivity(RECEIVE, HONGKONG), cargo.nextExpectedActivity());

    /*
      Use case 3: handling

      A handling event registration attempt will be formed from parsing
      the data coming in as a handling report either via
      the web service interface or as an uploaded CSV file.

      The handling event factory tries to create a HandlingEvent from the attempt,
      and if the factory decides that this is a plausible handling event, it is stored.
      If the attempt is invalid, for example if no cargo exists for the specfied tracking id,
      the attempt is rejected.

      Handling begins: cargo is received in Hongkong.
      */
    handlingEventService.registerHandlingEvent(
      new Date(100), trackingId, null, HONGKONG.unLocode(), RECEIVE
    );

    // Next event: Load onto voyage CM003 in Hongkong
    handlingEventService.registerHandlingEvent(
      new Date(200), trackingId, CM003.voyageNumber(), HONGKONG.unLocode(), LOAD
    );

    // Check current state - should be ok
    assertEquals(CM003, cargo.delivery().currentVoyage());
    assertEquals(HONGKONG, cargo.delivery().lastKnownLocation());
    assertEquals(ONBOARD_CARRIER, cargo.delivery().transportStatus());
    assertFalse(cargo.isMisdirected());
    assertEquals(new HandlingActivity(UNLOAD, NEWYORK), cargo.nextExpectedActivity());


    /*
      Here's an attempt to register a handling event that's not valid
      because there is no voyage with the specified voyage number,
      and there's no location with the specified UN Locode either.

      This attempt will be rejected and will not affet the cargo delivery in any way.
     */
    final VoyageNumber noSuchVoyageNumber = new VoyageNumber("XX000");
    final UnLocode noSuchUnLocode = new UnLocode("ZZZZZ");
    handlingEventService.registerHandlingEvent(
      new Date(300), trackingId, noSuchVoyageNumber, noSuchUnLocode, LOAD
    );


    // Cargo is now (incorrectly) unloaded in Tokyo
    handlingEventService.registerHandlingEvent(
      new Date(400), trackingId, CM003.voyageNumber(), TOKYO.unLocode(), UNLOAD
    );

    // Check current state - cargo is misdirected!
    assertEquals(NONE, cargo.delivery().currentVoyage());
    assertEquals(TOKYO, cargo.delivery().lastKnownLocation());
    assertEquals(IN_PORT, cargo.delivery().transportStatus());
    assertTrue(cargo.isMisdirected());
    assertEquals(HandlingActivity.NONE, cargo.nextExpectedActivity());


    // -- Cargo needs to be rerouted --


    // Specify a new route, this time from Tokyo (where it was incorrectly unloaded) to Stockholm
    RouteSpecification fromTokyo = new RouteSpecification(TOKYO, STOCKHOLM, arrivalDeadline);
    cargo.specifyNewRoute(fromTokyo);

    // The old itinerary does not satisfy the new specification
    assertEquals(RoutingStatus.MISROUTED, cargo.routingStatus());
    assertEquals(HandlingActivity.NONE, cargo.nextExpectedActivity());

    // Repeat procedure of selecting one out of a number of possible routes satisfying the route spec
    List<Itinerary> newItineraries = bookingService.requestPossibleRoutesForCargo(cargo.trackingId());
    Itinerary newItinerary = selectPreferedItinerary(newItineraries);
    cargo.assignToRoute(newItinerary);

    // New itinerary should satisfy new route
    assertEquals(RoutingStatus.ROUTED, cargo.routingStatus());

    // TODO we can't handle the face that after a reroute, the cargo isn't misdirected anymore
    //assertFalse(cargo.isMisdirected());
    //assertEquals(new HandlingActivity(LOAD, TOKYO), cargo.nextExpectedActivity());


    // -- Cargo has been rerouted, shipping continues --


    // Load in Tokyo
    handlingEventService.registerHandlingEvent(
      new Date(500), trackingId, CM003.voyageNumber(), TOKYO.unLocode(), LOAD
    );

    // Check current state - should be ok
    assertEquals(CM003, cargo.delivery().currentVoyage());
    assertEquals(TOKYO, cargo.delivery().lastKnownLocation());
    assertEquals(ONBOARD_CARRIER, cargo.delivery().transportStatus());
    assertFalse(cargo.isMisdirected());
    assertEquals(new HandlingActivity(UNLOAD, HAMBURG), cargo.nextExpectedActivity());

    // Unload in Hamburg
    handlingEventService.registerHandlingEvent(
      new Date(600), trackingId, CM003.voyageNumber(), HAMBURG.unLocode(), UNLOAD
    );

    // Check current state - should be ok
    assertEquals(NONE, cargo.delivery().currentVoyage());
    assertEquals(HAMBURG, cargo.delivery().lastKnownLocation());
    assertEquals(IN_PORT, cargo.delivery().transportStatus());
    assertFalse(cargo.isMisdirected());
    assertEquals(new HandlingActivity(LOAD, HAMBURG), cargo.nextExpectedActivity());


    // Load in Hamburg
    handlingEventService.registerHandlingEvent(
      new Date(700), trackingId, CM005.voyageNumber(), HAMBURG.unLocode(), LOAD
    );

    // Check current state - should be ok
    assertEquals(CM005, cargo.delivery().currentVoyage());
    assertEquals(HAMBURG, cargo.delivery().lastKnownLocation());
    assertEquals(ONBOARD_CARRIER, cargo.delivery().transportStatus());
    assertFalse(cargo.isMisdirected());
    assertEquals(new HandlingActivity(UNLOAD, STOCKHOLM), cargo.nextExpectedActivity());


    // Unload in Stockholm
    handlingEventService.registerHandlingEvent(
      new Date(800), trackingId, CM005.voyageNumber(), STOCKHOLM.unLocode(), UNLOAD
    );

    // Check current state - should be ok
    assertEquals(NONE, cargo.delivery().currentVoyage());
    assertEquals(STOCKHOLM, cargo.delivery().lastKnownLocation());
    assertEquals(IN_PORT, cargo.delivery().transportStatus());
    assertFalse(cargo.isMisdirected());
    assertEquals(new HandlingActivity(CLAIM, STOCKHOLM), cargo.nextExpectedActivity());

    // Finally, cargo is claimed in Stockholm. This ends the cargo lifecycle from our perspective.
    handlingEventService.registerHandlingEvent(
      new Date(900), trackingId, null, STOCKHOLM.unLocode(), CLAIM
    );

    // Check current state - should be ok
    assertEquals(NONE, cargo.delivery().currentVoyage());
    assertEquals(STOCKHOLM, cargo.delivery().lastKnownLocation());
    assertEquals(CLAIMED, cargo.delivery().transportStatus());
    assertFalse(cargo.isMisdirected());
    assertEquals(HandlingActivity.NONE, cargo.nextExpectedActivity());
  }






  /*
  * Utility stubs below.
  */

  private Date inTwoWeeks() {
    return new Date(new Date().getTime() + 1000 * 60 * 60 * 24 * 14);
  }

  private Itinerary selectPreferedItinerary(List<Itinerary> itineraries) {
    return itineraries.get(0);
  }

  protected void setUp() throws Exception {
    // Stub
    // TODO new voyages
    routingService = new RoutingService() {
      public List<Itinerary> fetchRoutesForSpecification(RouteSpecification routeSpecification) {
        if (routeSpecification.origin().equals(HONGKONG)) {
          // Hongkong - NYC - Chicago - Stockholm, initial routing
          return Arrays.asList(
            new Itinerary(Arrays.asList(
              new Leg(CM003, HONGKONG, NEWYORK, new Date(), new Date()),
              new Leg(CM004, NEWYORK, CHICAGO, new Date(), new Date()),
              new Leg(CM005, CHICAGO, STOCKHOLM, new Date(), new Date())
            ))
          );
        } else {
          // Tokyo - Hamburg - Stockholm, rerouting
          return Arrays.asList(
            new Itinerary(Arrays.asList(
              new Leg(CM003, TOKYO, HAMBURG, new Date(), new Date()),
              new Leg(CM005, HAMBURG, STOCKHOLM, new Date(), new Date())
            ))
          );
        }
      }
    };


    applicationEvents = new SynchronousApplicationEventsStub();

    // Stub
    // TODO move functionality to in-mem impl
    handlingEventRepository = new HandlingEventRepository() {
      Map<TrackingId, List<HandlingEvent>> eventMap = new HashMap<TrackingId, List<HandlingEvent>>();

      public void store(HandlingEvent event) {
        final TrackingId trackingId = event.cargo().trackingId();
        List<HandlingEvent> list = eventMap.get(trackingId);
        if (list == null) {
          list = new ArrayList<HandlingEvent>();
          eventMap.put(trackingId, list);
        }
        list.add(event);
      }

      public List<HandlingEvent> findEventsForCargo(TrackingId trackingId) {
        return eventMap.get(trackingId);
      }
    };

    // In-memory implementations
    cargoRepository = new CargoRepositoryInMem();
    locationRepository = new LocationRepositoryInMem();
    voyageRepository = new VoyageRepositoryInMem();

    // Actual factories and application services, wired with stubbed or in-memory infrastructure
    cargoInspectionService = new CargoInspectionServiceImpl(applicationEvents, cargoRepository, handlingEventRepository);
    handlingEventFactory = new HandlingEventFactory(cargoRepository, voyageRepository, locationRepository);
    handlingEventService = new HandlingEventServiceImpl(handlingEventRepository, applicationEvents, handlingEventFactory);
    bookingService = new BookingServiceImpl(cargoRepository, locationRepository, routingService);

    // Stub
    ((SynchronousApplicationEventsStub) applicationEvents).setCargoInspectionService(cargoInspectionService);
  }

}
