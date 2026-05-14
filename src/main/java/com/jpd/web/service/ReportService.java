package com.jpd.web.service;

import com.jpd.web.service.utils.CourseMetricsHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jpd.web.dto.ReportForm;
import com.jpd.web.model.Course;
import com.jpd.web.model.Report;
import com.jpd.web.repository.ReportRepository;
import com.jpd.web.service.utils.ValidationResources;
import com.jpd.web.transform.ReportTransform;

@Service
public class ReportService {
@Autowired
private ReportRepository reportRepository;
@Autowired
private ValidationResources validationResources;
@Autowired
private CourseMetricsHelper courseMetricsHelper;
public void saveReport(String customerId,ReportForm reportForm) {
	

Course c=	this.validationResources.validateCustomerWithCourse(customerId, reportForm.getCourseId());
	Report rep=ReportTransform.transToReport(reportForm, c, customerId);
	this.courseMetricsHelper.incrementReport(reportForm.getCourseId());
	this.reportRepository.save(rep);
}
}
