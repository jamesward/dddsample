package se.citerus.dddsample.interfaces.booking.web;

import org.springframework.web.servlet.mvc.multiaction.MultiActionController;
import se.citerus.dddsample.interfaces.booking.facade.BookingServiceFacade;
import se.citerus.dddsample.interfaces.booking.facade.dto.CargoRoutingDTO;
import se.citerus.dddsample.interfaces.booking.facade.dto.ItineraryCandidateDTO;
import se.citerus.dddsample.interfaces.booking.facade.dto.LegDTO;
import se.citerus.dddsample.interfaces.booking.facade.dto.LocationDTO;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles cargo booking and routing. Operates against a dedicated remoting service facade,
 * and could easily be rewritten as a thick Swing client. Completely separated from the domain layer,
 * unlike the tracking user interface.
 * <p/>
 * In order to successfully keep the domain model shielded from user interface considerations,
 * this approach is generally preferred to the one taken in the tracking controller. However,
 * there is never any one perfect solution for all situations, so we've chosen to demonstrate
 * two polarized ways to build user interfaces.   
 *
 * @see se.citerus.dddsample.interfaces.tracking.CargoTrackingController
 */
public final class CargoAdminController extends MultiActionController {

  private BookingServiceFacade bookingServiceFacade;

  public Map registrationForm(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
    final Map<String, Object> map = new HashMap<String, Object>();
    final List<LocationDTO> dtoList = bookingServiceFacade.listShippingLocations();

    final List<String> unLocodeStrings = new ArrayList<String>();

    for (LocationDTO dto : dtoList) {
      unLocodeStrings.add(dto.getUnLocode());
    }

    map.put("unlocodes", unLocodeStrings);
    return map;
  }

  public void register(final HttpServletRequest request, final HttpServletResponse response,
                       final RegistrationCommand command) throws Exception {

    final String trackingId = bookingServiceFacade.bookNewCargo(
      command.getOriginUnlocode(), command.getDestinationUnlocode()
    );
    response.sendRedirect("show.html?trackingId=" + trackingId);
  }

  public Map list(HttpServletRequest request, HttpServletResponse response) throws Exception {
    final Map<String, Object> map = new HashMap<String, Object>();
    final List<CargoRoutingDTO> cargoList = bookingServiceFacade.listAllCargos();

    map.put("cargoList", cargoList);
    return map;
  }

  public Map show(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
    final Map<String, Object> map = new HashMap<String, Object>();
    final String trackingId = request.getParameter("trackingId");
    final CargoRoutingDTO dto = bookingServiceFacade.loadCargoForRouting(trackingId);
    map.put("cargo", dto);
    return map;
  }

  public Map selectItinerary(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
    final Map<String, Object> map = new HashMap<String, Object>();
    final String trackingId = request.getParameter("trackingId");
    final List<ItineraryCandidateDTO> itineraryCandidates = bookingServiceFacade.requestPossibleRoutesForCargo(trackingId);

    if (request.getParameter("spec") != null) {
      map.put("itineraryCandidates", itineraryCandidates);
    }

    final CargoRoutingDTO cargoDTO = bookingServiceFacade.loadCargoForRouting(trackingId);
    map.put("origin", cargoDTO.getOrigin());
    map.put("destination", cargoDTO.getFinalDestination());
    map.put("trackingId", trackingId);
    return map;
  }

  public void assignItinerary(final HttpServletRequest request, final HttpServletResponse response, RouteAssignmentCommand command) throws Exception {
    final List<LegDTO> legDTOs = new ArrayList<LegDTO>(command.getLegs().size());
    for (RouteAssignmentCommand.LegCommand leg : command.getLegs()) {
      legDTOs.add(new LegDTO(leg.getCarrierMovementId(), leg.getFromUnLocode(), leg.getToUnLocode()));
    }

    final ItineraryCandidateDTO selectedItinerary = new ItineraryCandidateDTO(legDTOs);

    bookingServiceFacade.assignCargoToRoute(command.getTrackingId(), selectedItinerary);

    response.sendRedirect("list.html");
  }

  public void setBookingServiceFacade(BookingServiceFacade bookingServiceFacade) {
    this.bookingServiceFacade = bookingServiceFacade;
  }
}
