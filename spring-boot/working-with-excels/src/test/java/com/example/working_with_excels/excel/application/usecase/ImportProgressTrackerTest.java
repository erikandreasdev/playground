package com.example.working_with_excels.excel.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the ImportProgressTracker service.
 */
class ImportProgressTrackerTest {

    private ImportProgressTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new ImportProgressTracker();
    }

    @Nested
    @DisplayName("trackProgress")
    class TrackProgress {

        @Test
        @DisplayName("should track progress at 10% intervals")
        void shouldTrackAt10PercentIntervals() {
            // Act & Assert - simulate processing 100 rows
            tracker.trackProgress(10, 100, "TestSheet");
            assertThat(tracker.getLastLoggedPercentage()).isEqualTo(10);

            tracker.trackProgress(15, 100, "TestSheet");
            assertThat(tracker.getLastLoggedPercentage()).isEqualTo(10); // No change yet

            tracker.trackProgress(20, 100, "TestSheet");
            assertThat(tracker.getLastLoggedPercentage()).isEqualTo(20);
        }

        @Test
        @DisplayName("should handle 100% progress")
        void shouldHandle100Percent() {
            // Act
            tracker.trackProgress(100, 100, "TestSheet");

            // Assert
            assertThat(tracker.getLastLoggedPercentage()).isEqualTo(100);
        }

        @Test
        @DisplayName("should handle small datasets")
        void shouldHandleSmallDatasets() {
            // Act - 5 rows total
            tracker.trackProgress(1, 5, "SmallSheet");
            assertThat(tracker.getLastLoggedPercentage()).isEqualTo(20);

            tracker.trackProgress(3, 5, "SmallSheet");
            assertThat(tracker.getLastLoggedPercentage()).isEqualTo(60);
        }

        @Test
        @DisplayName("should handle zero total rows")
        void shouldHandleZeroTotalRows() {
            // Act - should not throw
            tracker.trackProgress(0, 0, "EmptySheet");

            // Assert
            assertThat(tracker.getLastLoggedPercentage()).isZero();
        }

        @Test
        @DisplayName("should handle negative total rows")
        void shouldHandleNegativeTotalRows() {
            // Act - edge case, should not throw
            tracker.trackProgress(1, -5, "InvalidSheet");

            // Assert
            assertThat(tracker.getLastLoggedPercentage()).isZero();
        }
    }

    @Nested
    @DisplayName("reset")
    class Reset {

        @Test
        @DisplayName("should reset progress to zero")
        void shouldResetToZero() {
            // Arrange
            tracker.trackProgress(50, 100, "Sheet1");
            assertThat(tracker.getLastLoggedPercentage()).isEqualTo(50);

            // Act
            tracker.reset();

            // Assert
            assertThat(tracker.getLastLoggedPercentage()).isZero();
        }

        @Test
        @DisplayName("should allow tracking new sheet after reset")
        void shouldAllowTrackingAfterReset() {
            // Arrange
            tracker.trackProgress(100, 100, "Sheet1");
            tracker.reset();

            // Act
            tracker.trackProgress(10, 100, "Sheet2");

            // Assert
            assertThat(tracker.getLastLoggedPercentage()).isEqualTo(10);
        }
    }
}
