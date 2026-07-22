package com.jpd.web.testutil;

import com.jpd.web.model.AccessMode;
import com.jpd.web.model.Chapter;
import com.jpd.web.model.Course;
import com.jpd.web.model.CourseMetrics;
import com.jpd.web.model.Creator;
import com.jpd.web.model.Language;

import java.util.ArrayList;

public final class CourseTestDataBuilder {

    private CourseTestDataBuilder() {
    }

    public static Course.CourseBuilder aCourse() {
        return Course.builder()
                .courseId(1L)
                .name("Test Course")
                .language(Language.ENGLISH)
                .teachingLanguage(Language.ENGLISH)
                .isBan(false)
                .isPublic(true)
                .accessMode(AccessMode.PUBLIC)
                .creator(CreatorTestDataBuilder.aCreator().build())
                .chapters(new ArrayList<>())
                .enrollments(new ArrayList<>())
                .reports(new ArrayList<>())
                .comments(new ArrayList<>())
                .wishlists(new ArrayList<>());
    }

    /** A course owned by the given creator id, linked back-and-forth with its creator. */
    public static Course aCourseOwnedBy(String creatorId) {
        Creator creator = CreatorTestDataBuilder.aCreatorWithId(creatorId);
        Course course = aCourse().creator(creator).build();
        creator.setCourses(new ArrayList<>(java.util.List.of(course)));
        return course;
    }

    public static CourseMetrics.CourseMetricsBuilder aMetricsFor(Course course) {
        return CourseMetrics.builder()
                .courseDescriptionId(1L)
                .course(course)
                .totalStudents(0)
                .totalFeedbacks(0)
                .averageRating(0.0)
                .totalRating(0)
                .totalPeopleLike(0)
                .totalReport(0);
    }

    public static Chapter.ChapterBuilder aChapterOf(Course course) {
        return Chapter.builder()
                .chapterId(1L)
                .ChapterName("Test Chapter")
                .orderInCourse(1)
                .course(course)
                .modules(new ArrayList<>());
    }
}
