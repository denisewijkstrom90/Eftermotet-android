package se.denise.eftermotet;

import org.junit.Test;
import static org.junit.Assert.*;

public class ReminderSchedulerTest {
    @Test public void overdueUnchangedAlarmIsPreserved() {
        // The sync decision must not depend on whether Android delivered a past alarm yet.
        assertTrue(ReminderScheduler.unchanged(1L, "Skola", "19:29", 1L, "Skola", "19:29"));
    }
    @Test public void editedTimeReplacesAlarm() {
        assertFalse(ReminderScheduler.unchanged(1L, "Skola", "19:29", 2L, "Skola", "19:30"));
    }
    @Test public void editedTitleUpdatesNotification() {
        assertFalse(ReminderScheduler.unchanged(1L, "Skola", "19:29", 1L, "Vård", "19:29"));
    }
    @Test public void editedDateUpdatesNotification() {
        assertFalse(ReminderScheduler.unchanged(1L, "Skola", "2026-10-04", 1L, "Skola", "2026-10-05"));
    }
}
