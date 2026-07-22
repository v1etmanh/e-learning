package com.jpd.web.service;

import com.jpd.web.dto.CreatorDto;
import com.jpd.web.dto.CreatorProfileDto;
import com.jpd.web.dto.LearningListDto;
import com.jpd.web.exception.ApiException;
import com.jpd.web.exception.CreatorAlreadyExistsException;
import com.jpd.web.model.Creator;
import com.jpd.web.model.Status;
import com.jpd.web.model.TypeOfFile;
import com.jpd.web.repository.CreatorRepository;
import com.jpd.web.service.utils.SendNoticeService;
import com.jpd.web.testutil.CreatorTestDataBuilder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CreatorRepository creatorRepository;
    @Mock
    private FireBaseService fireBaseService;
    @Mock
    private CourseInfService courseInfService;
    @Mock
    private WishlistService wishlistService;
    @Mock
    private SendNoticeService sendNoticeService;
    @Mock
    private KeycloakAdminService keycloakAdminService;

    @InjectMocks
    private CustomerService customerService;

    private CreatorProfileDto aProfile(org.springframework.web.multipart.MultipartFile image) {
        CreatorProfileDto dto = new CreatorProfileDto();
        dto.setFullName("Jane Doe");
        dto.setPhone("0123456789");
        dto.setBio("bio");
        dto.setAgreedToTerms(true);
        dto.setProfileImage(image);
        return dto;
    }

    @Nested
    class UploadProfile {

        @Test
        void sendsWelcomeEmail_beforeCheckingDuplicateProfile_evenWhenAlreadyExists() {
            // Documents current (likely unintended) ordering: the notice email fires
            // unconditionally before the already-exists check, so a duplicate signup
            // attempt still triggers the "congratulations" email.
            when(creatorRepository.findById("customer-1"))
                    .thenReturn(Optional.of(CreatorTestDataBuilder.aCreatorWithId("customer-1")));

            assertThrows(CreatorAlreadyExistsException.class,
                    () -> customerService.uploadProfile("jane@x.com", "customer-1", aProfile(null)));

            verify(sendNoticeService).sendNotice(any(), eq("jane@x.com"));
        }

        @Test
        void skipsImageUpload_whenNoProfileImageProvided() throws Exception {
            when(creatorRepository.findById("customer-1")).thenReturn(Optional.empty());
            when(creatorRepository.save(any(Creator.class))).thenAnswer(inv -> inv.getArgument(0));

            customerService.uploadProfile("jane@x.com", "customer-1", aProfile(null));

            verify(fireBaseService, never()).uploadFile(any(), any());
        }

        @Test
        void savesCreatorWithPendingStatus_thenAssignsKeycloakRole_inOrder() {
            when(creatorRepository.findById("customer-1")).thenReturn(Optional.empty());
            when(creatorRepository.save(any(Creator.class))).thenAnswer(inv -> inv.getArgument(0));

            CreatorDto result = customerService.uploadProfile("jane@x.com", "customer-1", aProfile(null));

            assertThat(result.getFullName()).isEqualTo("Jane Doe");
            InOrder order = inOrder(creatorRepository, keycloakAdminService);
            order.verify(creatorRepository).save(any(Creator.class));
            order.verify(keycloakAdminService).assignClientRoleToUser("customer-1");
        }

        @Test
        void uploadsImage_whenProfileImageProvided() throws Exception {
            MockMultipartFile image = new MockMultipartFile("img", "p.png", "image/png", new byte[]{1});
            when(creatorRepository.findById("customer-1")).thenReturn(Optional.empty());
            when(fireBaseService.uploadFile(image, TypeOfFile.IMG)).thenReturn("https://firebase/p.png");
            when(creatorRepository.save(any(Creator.class))).thenAnswer(inv -> inv.getArgument(0));

            customerService.uploadProfile("jane@x.com", "customer-1", aProfile(image));

            verify(fireBaseService).uploadFile(image, TypeOfFile.IMG);
        }

        @Test
        void wrapsIOException_fromImageUpload_asApiException() throws Exception {
            MockMultipartFile image = new MockMultipartFile("img", "p.png", "image/png", new byte[]{1});
            when(creatorRepository.findById("customer-1")).thenReturn(Optional.empty());
            when(fireBaseService.uploadFile(image, TypeOfFile.IMG)).thenThrow(new IOException("upload failed"));

            assertThrows(ApiException.class,
                    () -> customerService.uploadProfile("jane@x.com", "customer-1", aProfile(image)));
        }

        @Test
        void wrapsBlankUploadUrl_asApiException() throws Exception {
            MockMultipartFile image = new MockMultipartFile("img", "p.png", "image/png", new byte[]{1});
            when(creatorRepository.findById("customer-1")).thenReturn(Optional.empty());
            when(fireBaseService.uploadFile(image, TypeOfFile.IMG)).thenReturn("   ");

            assertThrows(ApiException.class,
                    () -> customerService.uploadProfile("jane@x.com", "customer-1", aProfile(image)));
        }
    }

    @Test
    void retrieveLearningList_combinesCoursesAndWishlist() {
        when(courseInfService.retrieveYourCourse("customer-1")).thenReturn(java.util.List.of());
        when(wishlistService.retrieveYourWishlist("customer-1")).thenReturn(java.util.List.of());

        LearningListDto result = customerService.retrieveLearningList("customer-1");

        assertThat(result.getCardDtos()).isEmpty();
        assertThat(result.getWishlistDtos()).isEmpty();
        verify(courseInfService).retrieveYourCourse("customer-1");
        verify(wishlistService).retrieveYourWishlist("customer-1");
    }
}
