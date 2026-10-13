package com.radiantlogic.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.radiantlogic.exception.WorkdayException;
import com.radiantlogic.model.JobLocation;
import com.radiantlogic.model.JobRow;
import com.radiantlogic.model.JobType;
import com.radiantlogic.model.JobWorker;
import com.radiantlogic.model.SupervisoryOrganization;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for the {@link Job} enum.
 *
 * <p>Uses Mockito and JUnit 5 to verify attribute lookup, value extraction,
 * and error handling behavior of the enum.</p>
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD.AtLeastOneConstructor", "PMD.UnitTestContainsTooManyAsserts"})
class JobTest {

  /** Mocked job row used for unit testing. */
  @SuppressWarnings("PMD.AccessorMethodGeneration")
  @Mock
  private JobRow jobRow;

  /**
   * Tests for the {@code fromName} functionality.
   *
   * <p>This nested test class groups all test cases related to the
   * {@code fromName} behavior.
   */
  @Nested
  @DisplayName("fromName")
  class FromName {

    @Test
    @DisplayName("Should resolve valid attribute names to matching enum constants")
    void shouldResolveValidAttributeNames() throws WorkdayException {
      assertThat(Job.fromName("JobID")).isEqualTo(Job.JOBID);
      assertThat(Job.fromName("Descriptor")).isEqualTo(Job.DESCRIPTOR);
      assertThat(Job.fromName("JobWorker")).isEqualTo(Job.JOBWORKER);
      assertThat(Job.fromName("SupervisoryOrganization"))
          .isEqualTo(Job.SUPERVISORYORGANIZATION);
      assertThat(Job.fromName("Location")).isEqualTo(Job.LOCATION);
      assertThat(Job.fromName("JobType")).isEqualTo(Job.JOBTYPE);
    }

    @Test
    @DisplayName("Should resolve attribute names irrespective of case")
    void shouldResolveAttributeNamesCaseInsensitively() throws WorkdayException {
      assertThat(Job.fromName("jobid")).isEqualTo(Job.JOBID);
      assertThat(Job.fromName("JOBID")).isEqualTo(Job.JOBID);
      assertThat(Job.fromName("jobtype")).isEqualTo(Job.JOBTYPE);
      assertThat(Job.fromName("LOCATION")).isEqualTo(Job.LOCATION);
    }

    @Test
    @DisplayName("Should throw WorkdayException for an unknown attribute name")
    void shouldThrowWorkdayExceptionForUnknownAttributeName() {
      assertThatThrownBy(() -> Job.fromName("InvalidAttribute"))
          .isInstanceOf(WorkdayException.class)
          .hasMessage("Invalid attribute name: InvalidAttribute");
    }
  }

  /**
   * Tests for the {@code extractValue} functionality.
   *
   * <p>This nested test class groups all test cases related to the
   * {@code extractValue} behavior.
   */
  @Nested
  @DisplayName("extractValue")
  class ExtractValue {

    @Test
    @DisplayName("Should extract job identifier")
    void shouldExtractJobId() {
      when(jobRow.getJobId()).thenReturn("JOB-123");

      assertThat(Job.JOBID.extractValue(jobRow))
          .isEqualTo("JOB-123");
    }

    @Test
    @DisplayName("Should extract descriptor")
    void shouldExtractDescriptor() {
      when(jobRow.getDescriptor()).thenReturn("Software Engineer");

      assertThat(Job.DESCRIPTOR.extractValue(jobRow))
          .isEqualTo("Software Engineer");
    }

    @Test
    @DisplayName("Should extract job worker")
    void shouldExtractJobWorker() {
      final JobWorker worker = new JobWorker();
      when(jobRow.getJobWorker()).thenReturn(worker);

      assertThat(Job.JOBWORKER.extractValue(jobRow))
          .isEqualTo(worker);
    }

    @Test
    @DisplayName("Should extract supervisory organization")
    void shouldExtractSupervisoryOrganization() {
      final SupervisoryOrganization organization = new SupervisoryOrganization();
      when(jobRow.getSupervisoryOrganization()).thenReturn(organization);

      assertThat(Job.SUPERVISORYORGANIZATION.extractValue(jobRow))
          .isEqualTo(organization);
    }

    @Test
    @DisplayName("Should extract location")
    void shouldExtractLocation() {
      final JobLocation location = new JobLocation();
      when(jobRow.getLocation()).thenReturn(location);

      assertThat(Job.LOCATION.extractValue(jobRow))
          .isEqualTo(location);
    }

    @Test
    @DisplayName("Should extract job type")
    void shouldExtractJobType() {
      final JobType jobType = new JobType();
      when(jobRow.getJobType()).thenReturn(jobType);

      assertThat(Job.JOBTYPE.extractValue(jobRow))
          .isEqualTo(jobType);
    }
  }

  /**
   * Tests for the {@code attributeName} functionality.
   *
   * <p>This nested test class groups all test cases related to the
   * {@code attributeName} behavior.
   */
  @Nested
  @DisplayName("attributeName")
  class AttributeName {

    @Test
    @DisplayName("Should expose configured attribute names")
    void shouldReturnConfiguredAttributeNames() {
      assertThat(Job.JOBID.getAttributeName()).isEqualTo("JobID");
      assertThat(Job.DESCRIPTOR.getAttributeName()).isEqualTo("Descriptor");
      assertThat(Job.JOBWORKER.getAttributeName()).isEqualTo("JobWorker");
      assertThat(Job.SUPERVISORYORGANIZATION.getAttributeName())
          .isEqualTo("SupervisoryOrganization");
      assertThat(Job.LOCATION.getAttributeName()).isEqualTo("Location");
      assertThat(Job.JOBTYPE.getAttributeName()).isEqualTo("JobType");
    }
  }
}
