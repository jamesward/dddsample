package se.citerus.dddsample.tracking.core.interfaces.handling.file;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import se.citerus.dddsample.tracking.core.application.handling.HandlingEventService;
import se.citerus.dddsample.tracking.core.domain.model.cargo.TrackingId;
import se.citerus.dddsample.tracking.core.domain.model.handling.OperatorCode;
import se.citerus.dddsample.tracking.core.domain.model.location.UnLocode;
import se.citerus.dddsample.tracking.core.domain.model.shared.HandlingActivityType;
import se.citerus.dddsample.tracking.core.domain.model.voyage.VoyageNumber;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimerTask;

import static se.citerus.dddsample.tracking.core.interfaces.handling.HandlingReportParser.*;

/**
 * Periodically scans a certain directory for files and attempts
 * to parse handling event registrations from the contents.
 * <p/>
 * Files that fail to parse are moved into a separate directory,
 * succesful files are deleted.
 */
public class UploadDirectoryScanner extends TimerTask implements InitializingBean {

  private File uploadDirectory;
  private File parseFailureDirectory;
  private HandlingEventService handlingEventService;

  private final static Log logger = LogFactory.getLog(UploadDirectoryScanner.class);

  @Override
  public void run() {
    for (File file : uploadDirectory.listFiles()) {
      try {
        parse(file);
        delete(file);
        logger.info("Import of " + file.getName() + " complete");
      } catch (Exception e) {
        logger.error(e, e);
        move(file);
      }
    }
  }

  private void parse(final File file) throws IOException {
    @SuppressWarnings("unchecked")
    final List<String> lines = FileUtils.readLines(file);
    final List<String> rejectedLines = new ArrayList<String>();
    for (String line : lines) {
      try {
        parseLine(line);
      } catch (Exception e) {
        logger.error("Rejected line \n" + line + "\nReason is: " + e);
        rejectedLines.add(line);
      }
    }
    if (!rejectedLines.isEmpty()) {
      writeRejectedLinesToFile(toRejectedFilename(file), rejectedLines);
    }
  }

  private String toRejectedFilename(final File file) {
    return file.getName() + ".reject";
  }

  private void writeRejectedLinesToFile(final String filename, final List<String> rejectedLines) throws IOException {
    FileUtils.writeLines(
      new File(parseFailureDirectory, filename), rejectedLines
    );
  }

  private void parseLine(final String line) throws Exception {
    final String[] columns = line.split("\t");
    if (columns.length == 5) {
      queueAttempt(columns[0], columns[1], columns[2], columns[3], columns[4]);
    } else if (columns.length == 4) {
      queueAttempt(columns[0], columns[1], "", columns[2], columns[3]);
    } else {
      throw new IllegalArgumentException("Wrong number of columns on line: " + line + ", must be 4 or 5");
    }
  }

  private void queueAttempt(String completionTimeStr, String trackingIdStr, String voyageNumberStr, String unLocodeStr, String eventTypeStr) throws Exception {
    final List<String> errors = new ArrayList<String>();

    final Date date = parseDate(completionTimeStr, errors);
    final TrackingId trackingId = parseTrackingId(trackingIdStr, errors);
    final VoyageNumber voyageNumber = parseVoyageNumber(voyageNumberStr, errors);
    final HandlingActivityType eventType = parseEventType(eventTypeStr, errors);
    final UnLocode unLocode = parseUnLocode(unLocodeStr, errors);
    final OperatorCode operatorCode = parseOperatorCode();

    if (errors.isEmpty()) {
      handlingEventService.registerHandlingEvent(date, trackingId, voyageNumber, unLocode, eventType, operatorCode);
    } else {
      throw new Exception(errors.toString());
    }
  }

  private void delete(final File file) {
    if (!file.delete()) {
      logger.error("Could not delete " + file.getName());
    }
  }

  private void move(final File file) {
    final File destination = new File(parseFailureDirectory, file.getName());
    final boolean result = file.renameTo(destination);
    if (!result) {
      logger.error("Could not move " + file.getName() + " to " + destination.getAbsolutePath());
    }
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    if (uploadDirectory.equals(parseFailureDirectory)) {
      throw new IOException("Upload and parse failed directories must not be the same directory: " + uploadDirectory);
    }
    if (!uploadDirectory.exists() && !uploadDirectory.mkdirs()) {
      throw new IOException("Failed to create " + uploadDirectory.getAbsolutePath());
    }
    if (!parseFailureDirectory.exists() && !parseFailureDirectory.mkdirs()) {
      throw new IOException("Failed to create " + parseFailureDirectory.getAbsolutePath());
    }
  }

  public void setUploadDirectory(File uploadDirectory) {
    this.uploadDirectory = uploadDirectory;
  }

  public void setParseFailureDirectory(File parseFailureDirectory) {
    this.parseFailureDirectory = parseFailureDirectory;
  }

  public void setHandlingEventService(HandlingEventService handlingEventService) {
    this.handlingEventService = handlingEventService;
  }
}
