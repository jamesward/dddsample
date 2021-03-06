package se.citerus.dddsample.tracking.core.domain.model.voyage;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static se.citerus.dddsample.tracking.core.application.util.DateTestUtil.toDate;
import static se.citerus.dddsample.tracking.core.domain.model.location.SampleLocations.*;

/**
 * Sample carrier movements, for test purposes.
 */
public class SampleVoyages {

  public final static Voyage pacific1 = new Voyage.Builder(new VoyageNumber("PAC1"), HONGKONG).
    addMovement(TOKYO, toDate("2009-03-03"), toDate("2009-03-05")).
    addMovement(LONGBEACH, toDate("2009-03-06"), toDate("2009-03-09")).
    addMovement(SEATTLE, toDate("2009-03-10"), toDate("2009-03-12")).
    addMovement(HONGKONG, toDate("2009-03-15"), toDate("2009-03-19")).
    build();

  public final static Voyage pacific2 = new Voyage.Builder(new VoyageNumber("PAC2"), SHANGHAI).
    addMovement(TOKYO, toDate("2009-03-04"), toDate("2009-03-05")).
    addMovement(LONGBEACH, toDate("2009-03-06"), toDate("2009-03-08")).
    addMovement(SEATTLE, toDate("2009-03-10"), toDate("2009-03-14")).
    addMovement(HANGZOU, toDate("2009-03-14"), toDate("2009-03-16")).
    addMovement(SHANGHAI, toDate("2009-03-17"), toDate("2009-03-19")).
    build();

  public final static Voyage continental1 = new Voyage.Builder(new VoyageNumber("CNT1"), LONGBEACH).
    addMovement(DALLAS, toDate("2009-03-06"), toDate("2009-03-08")).
    addMovement(CHICAGO, toDate("2009-03-09"), toDate("2009-03-10")).
    addMovement(NEWYORK, toDate("2009-03-11"), toDate("2009-03-14")).
    build();

  public final static Voyage continental2 = new Voyage.Builder(new VoyageNumber("CNT2"), LONGBEACH).
    addMovement(DALLAS, toDate("2009-03-06"), toDate("2009-03-08")).
    addMovement(NEWYORK, toDate("2009-03-10"), toDate("2009-03-14")).
    build();

  public final static Voyage continental3 = new Voyage.Builder(new VoyageNumber("CNT3"), SEATTLE).
    addMovement(CHICAGO, toDate("2009-03-06"), toDate("2009-03-08")).
    addMovement(NEWYORK, toDate("2009-03-10"), toDate("2009-03-13")).
    build();

  public final static Voyage atlantic1 = new Voyage.Builder(new VoyageNumber("ATC1"), NEWYORK).
      addMovement(ROTTERDAM, toDate("2009-03-13"), toDate("2009-03-18")).
      addMovement(HAMBURG, toDate("2009-03-19"), toDate("2009-03-20")).
      addMovement(HELSINKI, toDate("2009-03-21"), toDate("2009-03-22")).
      addMovement(NEWYORK, toDate("2009-03-24"), toDate("2009-03-30")).
      build();

  public final static Voyage atlantic2 = new Voyage.Builder(new VoyageNumber("ATC2"), NEWYORK).
      addMovement(HAMBURG, toDate("2009-03-17"), toDate("2009-03-20")).
      addMovement(GOTHENBURG, toDate("2009-03-22"), toDate("2009-03-24")).
      addMovement(STOCKHOLM, toDate("2009-03-25"), toDate("2009-03-26")).
      addMovement(HELSINKI, toDate("2009-03-27"), toDate("2009-03-28")).
      addMovement(NEWYORK, toDate("2009-03-31"), toDate("2009-04-04")).
      build();

  /**
   * Voyage number 0100S (by ship)
   * <p/>
   * Hongkong - Hangzou - Tokyo - Melbourne - New York
   */
  public static final Voyage HONGKONG_TO_NEW_YORK =
    new Voyage.Builder(new VoyageNumber("0100S"), HONGKONG).
      addMovement(HANGZOU, toDate("2008-10-01", "12:00"), toDate("2008-10-03", "14:30")).
      addMovement(TOKYO, toDate("2008-10-03", "21:00"), toDate("2008-10-06", "06:15")).
      addMovement(MELBOURNE, toDate("2008-10-06", "11:00"), toDate("2008-10-12", "11:30")).
      addMovement(NEWYORK, toDate("2008-10-14", "12:00"), toDate("2008-10-23", "23:10")).
      build();

  /**
   * Voyage number 0200T (by train)
   * <p/>
   * New York - Chicago - Dallas
   */
  public static final Voyage NEW_YORK_TO_DALLAS =
    new Voyage.Builder(new VoyageNumber("0200T"), NEWYORK).
      addMovement(CHICAGO, toDate("2008-10-24", "07:00"), toDate("2008-10-24", "17:45")).
      addMovement(DALLAS, toDate("2008-10-24", "21:25"), toDate("2008-10-25", "19:30")).
      build();

  /**
   * Voyage number 0300A (by airplane)
   * <p/>
   * Dallas - Hamburg - Stockholm - Helsinki
   */
  public static final Voyage DALLAS_TO_HELSINKI =
    new Voyage.Builder(new VoyageNumber("0300A"), DALLAS).
      addMovement(HAMBURG, toDate("2008-10-29", "03:30"), toDate("2008-10-31", "14:00")).
      addMovement(STOCKHOLM, toDate("2008-11-01", "15:20"), toDate("2008-11-01", "18:40")).
      addMovement(HELSINKI, toDate("2008-11-02", "09:00"), toDate("2008-11-02", "11:15")).
      build();

  /**
   * Voyage number 0301S (by ship)
   * <p/>
   * Dallas - Hamburg - Stockholm - Helsinki, alternate route
   */
  public static final Voyage DALLAS_TO_HELSINKI_ALT =
    new Voyage.Builder(new VoyageNumber("0301S"), DALLAS).
      addMovement(HELSINKI, toDate("2008-10-29", "03:30"), toDate("2008-11-05", "15:45")).
      build();

  /**
   * Voyage number 0400S (by ship)
   * <p/>
   * Helsinki - Rotterdam - Shanghai - Hongkong
   */
  public static final Voyage HELSINKI_TO_HONGKONG =
    new Voyage.Builder(new VoyageNumber("0400S"), HELSINKI).
      addMovement(ROTTERDAM, toDate("2008-11-04", "05:50"), toDate("2008-11-06", "14:10")).
      addMovement(SHANGHAI, toDate("2008-11-10", "21:45"), toDate("2008-11-22", "16:40")).
      addMovement(HONGKONG, toDate("2008-11-24", "07:00"), toDate("2008-11-28", "13:37")).
      build();

  private static final Map<VoyageNumber, Voyage> ALL = new HashMap<VoyageNumber, Voyage>();

    static {
    for (Field field : SampleVoyages.class.getDeclaredFields()) {
      if (field.getType().equals(Voyage.class)) {
        try {
          Voyage voyage = (Voyage) field.get(null);
          ALL.put(voyage.voyageNumber(), voyage);
        } catch (IllegalAccessException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

  public static List<Voyage> getAll() {
    return new ArrayList<Voyage>(ALL.values());
  }

  public static Voyage lookup(VoyageNumber voyageNumber) {
    return ALL.get(voyageNumber);
  }

}
