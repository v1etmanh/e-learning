package com.jpd.web.testutil;

import com.jpd.web.model.Course;
import com.jpd.web.model.Enrollment;

import java.util.ArrayList;

public final class EnrollmentTestDataBuilder {

    private EnrollmentTestDataBuilder() {
    }

    public static Enrollment.EnrollmentBuilder anEnrollment() {
        return Enrollment.builder()
                .enrollId(1L)
                .customerId("customer-1")
                .isFinish(false)
                .customerModuleContents(new ArrayList<>());
    }

    public static Enrollment anEnrollmentOf(String customerId, Course course) {
        return anEnrollment().customerId(customerId).course(course).build();
    }
}
