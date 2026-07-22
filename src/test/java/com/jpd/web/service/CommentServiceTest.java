package com.jpd.web.service;

import com.jpd.web.dto.CommentDto;
import com.jpd.web.exception.CommentNotFoundException;
import com.jpd.web.exception.FeedBackIligalException;
import com.jpd.web.exception.UnauthorizedException;
import com.jpd.web.model.Comment;
import com.jpd.web.model.Course;
import com.jpd.web.repository.CommentRepository;
import com.jpd.web.service.utils.CommentFilterService;
import com.jpd.web.service.utils.ValidationResources;
import com.jpd.web.testutil.CourseTestDataBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentaRepository;
    @Mock
    private CommentFilterService commentFilterService;
    @Mock
    private ValidationResources validationResources;

    @InjectMocks
    private CommentService commentService;

    @Test
    void createComment_delegatesAccessCheckToValidationResources() {
        Course course = CourseTestDataBuilder.aCourse().build();
        when(validationResources.validateCustomerWithCourse("student-1", 1L)).thenReturn(course);
        when(commentFilterService.isToxic("hello")).thenReturn(false);
        when(commentaRepository.save(org.mockito.ArgumentMatchers.any(Comment.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CommentDto result = commentService.createComment("student-1", "hello", 1L);

        assertThat(result.getComment()).isEqualTo("hello");
        assertThat(result.getCreateBy()).isEqualTo("student-1");
    }

    @Test
    void createComment_throwsFeedBackIllegal_whenToxic() {
        Course course = CourseTestDataBuilder.aCourse().build();
        when(validationResources.validateCustomerWithCourse("student-1", 1L)).thenReturn(course);
        when(commentFilterService.isToxic("bad")).thenReturn(true);

        assertThrows(FeedBackIligalException.class,
                () -> commentService.createComment("student-1", "bad", 1L));
    }

    @Test
    void deleteCommentById_throwsCommentNotFound_whenMissing() {
        when(commentaRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(CommentNotFoundException.class,
                () -> commentService.deleteCommentById(1L, "student-1"));
    }

    @Test
    void deleteCommentById_throwsUnauthorized_whenCallerIsNotCourseCreatorAndNotOwner() {
        Course course = CourseTestDataBuilder.aCourseOwnedBy("creator-1");
        Comment comment = Comment.builder().commentId(1L).course(course).customerId("student-1").build();
        when(commentaRepository.findById(1L)).thenReturn(Optional.of(comment));

        // Current behavior: no "customer owns their own comment" branch exists -
        // only the course creator may delete, so even the comment's own author is rejected.
        assertThrows(UnauthorizedException.class,
                () -> commentService.deleteCommentById(1L, "student-1"));
    }

    @Test
    void deleteCommentById_succeeds_whenCallerIsCourseCreator() {
        Course course = CourseTestDataBuilder.aCourseOwnedBy("creator-1");
        Comment comment = Comment.builder().commentId(1L).course(course).build();
        when(commentaRepository.findById(1L)).thenReturn(Optional.of(comment));

        commentService.deleteCommentById(1L, "creator-1");

        org.mockito.Mockito.verify(commentaRepository).delete(comment);
    }

    @Test
    void updateComment_hasNoOwnershipCheck_anyCallerCanUpdate() {
        Comment comment = Comment.builder().commentId(1L).content("old").build();
        when(commentaRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(commentaRepository.save(org.mockito.ArgumentMatchers.any(Comment.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Documents current (arguably incomplete) behavior: no ownership check at all,
        // a completely unrelated customerId can still update the comment.
        CommentDto result = commentService.updateComment(1L, "totally-unrelated-customer", "new content");

        assertThat(result.getComment()).isEqualTo("new content");
        assertThat(comment.getContent()).isEqualTo("new content");
    }

    @Test
    void retrieveAllCommentOfCourse_delegatesAccessCheck_andHardcodesAnonymousAuthor() {
        Course course = CourseTestDataBuilder.aCourse().build();
        when(validationResources.validateCustomerWithCourse("student-1", 1L)).thenReturn(course);
        Comment comment = Comment.builder().commentId(1L).content("hi").customerId("student-1").build();
        when(commentaRepository.findByCourse_CourseId(1L)).thenReturn(List.of(comment));

        List<CommentDto> result = commentService.retrieveAllCommentOfCourse(1L, "student-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getComment()).isEqualTo("hi");
        assertThat(result.get(0).getCreateBy()).isEqualTo("Anonymous");
    }
}
