package com.jpd.web.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.jpd.web.exception.ModuleNotFoundException;
import com.jpd.web.repository.ModuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jpd.web.dto.ModerationBatchResult;
import com.jpd.web.dto.ModuleContentDto;
import com.jpd.web.dto.ModuleContentUpdateResult;
import com.jpd.web.dto.RejectedContent;
import com.jpd.web.dto.RejectedContentInfo;
import com.jpd.web.dto.StandardizedContentDto;
import com.jpd.web.exception.ModerationException;
import com.jpd.web.exception.ModuleContentNotFoundException;
import com.jpd.web.exception.UnauthorizedException;
import com.jpd.web.model.Course;
import com.jpd.web.model.Module;
import com.jpd.web.model.ModuleContent;
import com.jpd.web.model.TypeOfContent;
import com.jpd.web.repository.ModuleContentRepository;
import com.jpd.web.repository.PassageRepository;
import com.jpd.web.repository.ReadingQuestionRepository;
import com.jpd.web.service.utils.ValidationResources;
import com.jpd.web.transform.ModuleContentTransform;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
/**
 * Service for managing module content operations
 * Handles CRUD operations with ownership validation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModuleContentService {

	private final ModuleContentRepository moduleContentRepository;
	private final ModuleRepository moduleRepository;
	private final ValidationResources validationResources;
	private final ReadingQuestionRepository readingQuestionRepository;
	private final PassageRepository passageRepository;
	private final EntityManager entityManager;

	/**
	 * Update course material with optimized batch processing
	 * Handles both new and existing content efficiently
	 */
	@Transactional
	public List<ModuleContent> updateCourseMaterial(
			ModuleContentDto moduleContentDto,
			String creatorId) {

		log.info("Updating course material for module {} by creator {}",
				moduleContentDto.getModuleId(), creatorId);

		// Validate module ownership
		validateModuleOwnership(moduleContentDto.getModuleId(), creatorId);

		List<ModuleContent> dtoContents = moduleContentDto.getModuleContent();
		if (dtoContents == null || dtoContents.isEmpty()) {
			log.warn("No content provided for update");
			return new ArrayList<>();
		}

		// Separate new and existing content
		List<ModuleContent> newContents = new ArrayList<>();
		List<Long> existingIds = new ArrayList<>();

		for (ModuleContent content : dtoContents) {
			content.setModuleId(moduleContentDto.getModuleId());

			if (isNewContent(content)) {
				content.setMcId(null);
				newContents.add(content);
			} else {
				existingIds.add(content.getMcId());
				content.setMcId(null); // Reset ID for re-insertion
				newContents.add(content);
			}
		}

		// Delete existing content in batch
		deleteExistingContent(existingIds);

		// Insert all content in batch
		List<ModuleContent> savedContents = moduleContentRepository.saveAll(newContents);

		log.info("Successfully updated {} contents for module {}",
				savedContents.size(), moduleContentDto.getModuleId());

		return savedContents;
	}

	/**
	 * Delete a single module content with validation
	 */
	@Transactional
	public void deleteModuleContent(
			Long contentId,
			Long moduleId,
			String creatorId) {

		log.info("Deleting content {} from module {} by creator {}",
				contentId, moduleId, creatorId);

		// Validate ownership
		validateModuleOwnership(moduleId, creatorId);

		// Validate content exists and belongs to module
		ModuleContent content = moduleContentRepository.findById(contentId)
				.orElseThrow(() -> new ModuleContentNotFoundException(contentId));

		if (content.getModuleId()!=(moduleId)) {
			log.warn("Content {} does not belong to module {}", contentId, moduleId);
			throw new UnauthorizedException(
					"This content does not belong to the specified module");
		}

		moduleContentRepository.deleteById(contentId);

		log.info("Successfully deleted content {} from module {}", contentId, moduleId);
	}

	/**
	 * Delete all module contents by type
	 */
	@Transactional
	public void deleteModuleContentsByType(
			TypeOfContent type,
			Long moduleId,
			String creatorId) {

		log.info("Deleting all {} contents from module {} by creator {}",
				type, moduleId, creatorId);

		// Validate ownership
		validateModuleOwnership(moduleId, creatorId);

		// Delete by type and module
		int deletedCount = moduleContentRepository
				.deleteByTypeOfContentAndModuleId(type, moduleId);

		log.info("Successfully deleted {} contents of type {} from module {}",
				deletedCount, type, moduleId);
	}

	/**
	 * Get module contents by type with ownership validation
	 * Fixed: Removed redundant database query loop
	 */
	@Transactional()
	public List<ModuleContent> getModuleContentsByType(
			TypeOfContent type,
			Long moduleId,
			String creatorId) {

		log.info("Retrieving {} contents from module {} by creator {}",
				type, moduleId, creatorId);

		// Validate ownership
		validateModuleOwnership(moduleId, creatorId);

		// Direct query - no need for redundant loop
		List<ModuleContent> contents = moduleContentRepository
				.findByTypeOfContentAndModuleId(type, moduleId);

		log.info("Found {} contents of type {} in module {}",
				contents.size(), type, moduleId);

		return contents;
	}

	/**
	 * Get all module contents with ownership validation
	 */
	@Transactional()
	public List<ModuleContent> getAllModuleContents(
			Long moduleId,
			String creatorId) {

		log.info("Retrieving all contents from module {} by creator {}",
				moduleId, creatorId);

		validateModuleOwnership(moduleId, creatorId);

		return moduleContentRepository.findByModuleId(moduleId);
	}

	// ==================== Private Helper Methods ====================

	/**
	 * Validate that the creator owns the module
	 */
	private void validateModuleOwnership(Long moduleId, String creatorId) {
		Module module = moduleRepository.findById(moduleId)
				.orElseThrow(() -> new ModuleNotFoundException(moduleId));

		String moduleCreatorId = module.getChapter()
				.getCourse()
				.getCreator()
				.getCreatorId();

		if (!moduleCreatorId.equals(creatorId)) {
			log.warn("Unauthorized access attempt: creator {} for module {}",
					creatorId, moduleId);
			throw new UnauthorizedException(
					"You are not the creator of this course");
		}
	}

	/**
	 * Check if content is new (not yet persisted)
	 */
	private boolean isNewContent(ModuleContent content) {
		return content.getMcId() == null || content.getMcId() < 0;
	}

	/**
	 * Delete existing content in batch with flush and clear
	 */
	private void deleteExistingContent(List<Long> ids) {
		if (ids.isEmpty()) {
			return;
		}

		log.debug("Deleting {} existing contents", ids.size());

		moduleContentRepository.deleteAllById(ids);
		moduleContentRepository.flush();
		entityManager.clear();

		log.debug("Successfully deleted and cleared existing contents");
	}

	/**
	 * Validate content ownership before operations
	 */
	private void validateContentOwnership(
			Long contentId,
			Long moduleId,
			String creatorId) {

		validateModuleOwnership(moduleId, creatorId);

		ModuleContent content = moduleContentRepository.findById(contentId)
				.orElseThrow(() -> new ModuleContentNotFoundException(contentId));

		if (content.getModuleId()!=moduleId) {
			throw new UnauthorizedException(
					"Content does not belong to the specified module");
		}
	}
}
//		@Transactional
//		public ModuleContentUpdateResult updateCourseMaterial1(
//		    ModuleContentDto moduleContentDto, long creatorId) {
//
//		    Module module = validationResources.validateCompleteOwnership(
//		        moduleContentDto.getModuleId(),
//		        moduleContentDto.getChapterId(),
//		        moduleContentDto.getCourseId(),
//		        creatorId
//		    );
//
//		    Course c = module.getChapter().getCourse();
//		    List<ModuleContent> dtoContents = moduleContentDto.getModuleContent();
//
//		    // ✅ TẠO MAP ĐỂ LƯU ID NHẤT QUÁN
//		    Map<ModuleContent, Long> contentIdMap = new HashMap<>();
//
//		    // ✅ GÁN ID CHO TẤT CẢ TRƯỚC KHI TRANSFORM
//		    for (ModuleContent mc : dtoContents) {
//		        long contentId = mc.getMcId() != null && mc.getMcId() > 0
//		            ? mc.getMcId()
//		            : -Math.abs(System.nanoTime() + contentIdMap.size()); // Đảm bảo unique
//		        contentIdMap.put(mc, contentId);
//		    }
//
//		    // 1️⃣ Transform với ID từ Map
//		    List<StandardizedContentDto> toModerate = new ArrayList<>();
//		    for (ModuleContent mc : dtoContents) {
//		        StandardizedContentDto dto = ModuleContentTransform.transform(
//		            mc,
//		            c.getLanguage(),
//		            c.getTeachingLanguage()
//		        );
//		        // ✅ GHI ĐÈ CONTENT_ID TỪ MAP
//		        dto.setContent_id(contentIdMap.get(mc));
//		        toModerate.add(dto);
//		    }
//
//		    if (toModerate.isEmpty()) {
//		        throw new ModerationException("Failed to transform any content");
//		    }
//
//		    // 2️⃣ Call moderation API
//		    ModerationBatchResult moderationResult;
//		    try {
//		        moderationResult = moderationService.moderateBatchWithDetails(toModerate);
//		    } catch (ModerationException e) {
//		        log.error("Moderation service failed", e);
//		        throw new ModerationException("Content moderation service unavailable: " + e.getMessage());
//		    }
//
//		    // 3️⃣ Create map of approved content IDs
//		    Set<Long> approvedIds = moderationResult.getApprovedContents().stream()
//		        .map(StandardizedContentDto::getContent_id)
//		        .collect(Collectors.toSet());
//
//		    // 4️⃣ Create rejection reason map
//		    Map<Long, String> rejectionReasons = moderationResult.getRejectedContents().stream()
//		        .collect(Collectors.toMap(
//		            RejectedContentInfo::getContentId,
//		            RejectedContentInfo::getReason
//		        ));
//
//		    log.info("Moderation results: {} submitted, {} approved, {} rejected",
//		        toModerate.size(), approvedIds.size(), rejectionReasons.size());
//
//		    // 5️⃣ Filter using consistent IDs from Map
//		    List<ModuleContent> approvedModuleContents = dtoContents.stream()
//		        .filter(mc -> approvedIds.contains(contentIdMap.get(mc))) // ✅ DÙNG ID TỪ MAP
//		        .collect(Collectors.toList());
//
//		    // 6️⃣ Collect rejected contents
//		    List<RejectedContent> rejectedContents = dtoContents.stream()
//		        .filter(mc -> !approvedIds.contains(contentIdMap.get(mc))) // ✅ DÙNG ID TỪ MAP
//		        .map(mc -> {
//		            RejectedContent rejected = new RejectedContent();
//		            rejected.setMcId(mc.getMcId());
//		            rejected.setContent(ModuleContentTransform.extractRawContent(mc));
//		            rejected.setType(mc.getTypeOfContent());
//		            rejected.setReason(rejectionReasons.getOrDefault(
//		                contentIdMap.get(mc), // ✅ DÙNG ID TỪ MAP
//		                "Unknown reason"
//		            ));
//		            return rejected;
//		        })
//		        .collect(Collectors.toList());
//
//		    if (approvedModuleContents.isEmpty()) {
//		        log.warn("All {} contents were rejected by moderation", dtoContents.size());
//		        return new ModuleContentUpdateResult(Collections.emptyList(), rejectedContents);
//		    }
//
//		    // 7️⃣ - 9️⃣ Phần còn lại giữ nguyên
//		    List<ModuleContent> toInsert = new ArrayList<>();
//		    List<Long> idsToDelete = new ArrayList<>();
//
//		    for (ModuleContent mc : approvedModuleContents) {
//		        mc.setModule(module);
//
//		        if (mc.getMcId() == null || mc.getMcId() < 0) {
//		            mc.setMcId(null);
//		            toInsert.add(mc);
//		        } else {
//		            idsToDelete.add(mc.getMcId());
//		            mc.setMcId(null);
//		            toInsert.add(mc);
//		        }
//		    }
//
//		    if (!idsToDelete.isEmpty()) {
//		        List<ModuleContent> toDeleteEntities =
//		            moduleContentRepository.findAllById(idsToDelete);
//		        this.moduleContentRepository.deleteAllById(idsToDelete);
//		        this.moduleContentRepository.flush();
//		    }
//
//		    List<ModuleContent> savedContents = toInsert.stream()
//		    	    .map(mc -> moduleContentRepository.save(mc)) // save() sẽ tự động merge nếu cần
//		    	    .collect(Collectors.toList());
//
//		    log.info("Update complete: {} saved, {} rejected",
//		        savedContents.size(), rejectedContents.size());
//
//		    return new ModuleContentUpdateResult(savedContents, rejectedContents);
//		}
	

