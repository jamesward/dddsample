package se.citerus.dddsample.service.dto.assembler;

import junit.framework.TestCase;
import se.citerus.dddsample.domain.Location;
import static se.citerus.dddsample.domain.SampleLocations.HAMBURG;
import static se.citerus.dddsample.domain.SampleLocations.STOCKHOLM;
import se.citerus.dddsample.service.dto.LocationDTO;

import java.util.Arrays;
import java.util.List;

public class LocationDTOAssemblerTest extends TestCase {

  public void testToDTOList() {
    final LocationDTOAssembler assembler = new LocationDTOAssembler();
    final List<Location> locationList = Arrays.asList(STOCKHOLM, HAMBURG);

    final List<LocationDTO> dtos = assembler.toDTOList(locationList);

    assertEquals(2, dtos.size());

    LocationDTO dto = dtos.get(0);
    assertEquals("SESTO", dto.getUnLocode());
    assertEquals("Stockholm", dto.getName());

    dto = dtos.get(1);
    assertEquals("DEHAM", dto.getUnLocode());
    assertEquals("Hamburg", dto.getName());
  }

}
