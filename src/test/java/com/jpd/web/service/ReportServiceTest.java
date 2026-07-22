package com.jpd.web.service;

import com.jpd.web.dto.ReportForm;
import com.jpd.web.model.Course;
import com.jpd.web.model.Report;
import com.jpd.web.model.ReportType;
import com.jpd.web.repository.ReportRepository;
import com.jpd.web.service.utils.CourseMetricsHelper;
import com.jpd.web.service.utils.ValidationResources;
import com.jpd.web.testutil.CourseTestDataBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;
    @Mock
    private ValidationResources validationResources;
    @Mock
    private CourseMetricsHelper courseMetricsHelper;

    @InjectMocks
    private ReportService reportService;

    @Test
    void saveReport_delegatesAccessCheck_incrementsMetrics_thenSaves() {
        Course course = CourseTestDataBuilder.aCourse().courseId(1L).build();
        when(validationResources.validateCustomerWithCourse("customer-1", 1L)).thenReturn(course);
        ReportForm form = ReportForm.builder().courseId(1L).type(ReportType.OTHER).detail("bad content").build();

        reportService.saveReport("customer-1", form);

        verify(courseMetricsHelper).incrementReport(1L);
        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).save(captor.capture());
        assertThat(captor.getValue().getCustomerId()).isEqualTo("customer-1");
        assertThat(captor.getValue().getCourse()).isSameAs(course);
    }
}
