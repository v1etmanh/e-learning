package com.jpd.web.service;

import com.jpd.web.dto.WishlistDto;
import com.jpd.web.exception.CourseNotFoundException;
import com.jpd.web.exception.WishlistExistException;
import com.jpd.web.model.Course;
import com.jpd.web.model.Wishlist;
import com.jpd.web.repository.CourseRepository;
import com.jpd.web.repository.WishlistRepository;
import com.jpd.web.service.utils.CourseMetricsHelper;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WishlistServiceTest {

    @Mock
    private WishlistRepository wishlistRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private CourseMetricsHelper courseMetricsHelper;

    @InjectMocks
    private WishlistService wishlistService;

    @Test
    void addWishlist_throwsCourseNotFound_whenCourseMissing() {
        when(courseRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(CourseNotFoundException.class,
                () -> wishlistService.addWishlist("student-1", 1L));
    }

    @Test
    void addWishlist_throwsWishlistExist_whenAlreadyWishlisted() {
        Course course = CourseTestDataBuilder.aCourse().build();
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(wishlistRepository.findByCourse_CourseIdAndCustomerId(1L, "student-1"))
                .thenReturn(Optional.of(Wishlist.builder().id(1L).build()));

        assertThrows(WishlistExistException.class,
                () -> wishlistService.addWishlist("student-1", 1L));
    }

    @Test
    void addWishlist_incrementsMetricsAndSaves_onHappyPath() {
        Course course = CourseTestDataBuilder.aCourse().build();
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(wishlistRepository.findByCourse_CourseIdAndCustomerId(1L, "student-1"))
                .thenReturn(Optional.empty());

        wishlistService.addWishlist("student-1", 1L);

        verify(courseMetricsHelper).incrementWishlist(1L);
        ArgumentCaptor<Wishlist> captor = ArgumentCaptor.forClass(Wishlist.class);
        verify(wishlistRepository).save(captor.capture());
        assertThat(captor.getValue().getCustomerId()).isEqualTo("student-1");
        assertThat(captor.getValue().getCourse()).isSameAs(course);
    }

    @Test
    void retrieveYourWishlist_mapsEachEntryToDto() {
        Course course = CourseTestDataBuilder.aCourse().build();
        Wishlist wishlist = Wishlist.builder().id(1L).customerId("student-1").course(course).build();
        when(wishlistRepository.findByCustomerId("student-1")).thenReturn(List.of(wishlist));

        List<WishlistDto> result = wishlistService.retrieveYourWishlist("student-1");

        assertThat(result).hasSize(1);
    }
}
