package com.jpd.web.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.jpd.web.dto.CourseLearningCardDto;
import com.jpd.web.dto.CreatorDto;
import com.jpd.web.dto.CreatorProfileDto;
import com.jpd.web.dto.LearningListDto;
import com.jpd.web.dto.NoticeForm;

import com.jpd.web.dto.WishlistDto;
import com.jpd.web.exception.ApiException;
import com.jpd.web.exception.CreatorAlreadyExistsException;

import com.jpd.web.model.Creator;
import com.jpd.web.model.Status;
import com.jpd.web.model.TypeOfFile;
import com.jpd.web.repository.CreatorRepository;
import com.jpd.web.service.utils.SendNoticeService;
import com.jpd.web.transform.CreatorTransform;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CustomerService {

	@Autowired
	private CreatorRepository creatorRepository;
	@Autowired
	private FireBaseService fireBaseService;
   @Autowired
  private CourseInfService courseInfService;
   @Autowired
   private WishlistService wishlistService;
   @Autowired
   private SendNoticeService sendNoticeService;
@Autowired
private KeycloakAdminService keycloakAdminService;



	private String uploadProfileImage(CreatorProfileDto profileDto, String customerId) {
		try {
			log.debug("Uploading profile image for email: {}", customerId);

			String imageUrl = fireBaseService.uploadFile(profileDto.getProfileImage(), TypeOfFile.IMG);

			if (imageUrl == null || imageUrl.trim().isEmpty()) {
				throw new FileUploadException("Failed to upload profile image");
			}

			log.debug("Profile image uploaded successfully for email: {}", customerId);

			return imageUrl;

		} catch (IOException e) {
			log.error("Error uploading profile image for email: {}", customerId, e);
			throw new ApiException("Failed to upload profile image: " + e.getMessage());
		}
	}

	public CreatorDto uploadProfile(String email,String customerId, CreatorProfileDto profileDto) {


		Optional<Creator> existingCreator = this.creatorRepository.findById(customerId);
		NoticeForm n=NoticeForm.builder()
				.createdAt(LocalDateTime.now())
				.message("customer with given name "+customerId+"vs email " +email+" đăng kí để thành creator => status thành công")
				.build();
		this.sendNoticeService.sendNotice(n, email);
		if (existingCreator.isPresent()) {
			log.warn("Creator profile already exists for email: {}", email);
			throw new CreatorAlreadyExistsException("You already have a creator profile");
		}

		// 3. Tạo mới Creator
		Creator creator = CreatorTransform.transformFromCreatorDto(profileDto);
		creator.setCreatorId(customerId);
		creator.setStatus(Status.PENDING);
		// 4. Upload ảnh nếu có
		if (profileDto.getProfileImage() != null && !profileDto.getProfileImage().isEmpty()) {
			String imageUrl = uploadProfileImage(profileDto, email);
			creator.setImageUrl(imageUrl);
		}

		// 5. Gán customer cho creator

		// 6. Lưu vào database
		Creator cr1= this.creatorRepository.save(creator);
     this.keycloakAdminService.assignClientRoleToUser(customerId);
		return CreatorTransform.transToCreatorDto(cr1);
	}
public LearningListDto retrieveLearningList(String customerId) {
	List<CourseLearningCardDto> clr=this.courseInfService.retrieveYourCourse(customerId);
	List<WishlistDto>wld=this.wishlistService.retrieveYourWishlist(customerId);
	LearningListDto l=LearningListDto.builder()
			.cardDtos(clr)
			.wishlistDtos(wld)
			.build();
	return l;
}
 
}
