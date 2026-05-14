package com.jpd.web.service;

import java.io.IOException;
import java.util.Optional;

import com.jpd.web.model.*;
import com.jpd.web.repository.CreatorMediaCapacityRepository;
import com.jpd.web.repository.CreatorRepository;
import com.jpd.web.service.utils.FileCategory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.jpd.web.dto.ModerationResponse;
import com.jpd.web.exception.ApiException;
import com.jpd.web.exception.FileUploadException;
import com.jpd.web.exception.ModerateException;
import com.jpd.web.repository.PendingImgRepository;
import com.jpd.web.service.utils.ValidationResources;

import jakarta.transaction.Transactional;

@Service
public class FileUploadService {

	@Autowired
	private PendingImgRepository pendingImgRepository;

	@Autowired
	private CreatorRepository creatorRepository;

	@Autowired
	private FireBaseService fireBaseService;

	@Autowired
	private CreatorMediaCapacityRepository creatorMediaCapacityRepository;

	private static final long MAX_FOLDER_SIZE_MB = 500;
	private static final long MAX_FOLDER_SIZE_BYTES = MAX_FOLDER_SIZE_MB * 1024 * 1024;
	private static final long MAX_FILE_SIZE_BYTES = 50 * 1024 * 1024; // 50MB cho 1 file

	/**
	 * Lưu file (IMG hoặc PDF) vào Firebase với kiểm tra dung lượng
	 * @param creatorId ID của creator
	 * @param file file cần upload
	 * @param type loại file (IMG hoặc PDF)
	 * @return URL của file đã upload
	 * @throws IllegalAccessException
	 * @throws IOException
	 */
	@Transactional
	public String saveImgIntoFirebase(String creatorId, MultipartFile file, TypeOfFile type)
			throws IllegalAccessException, IOException {

		// 1. Validate file input
		validateFileInput(file);

		// 2. Xác định category
		FileCategory category = determineFileCategory(type);

		// 3. Moderate image nếu là IMG


		// 4. Kiểm tra dung lượng và upload
		try {
			// Kiểm tra kích thước file đơn lẻ
			validateSingleFileSize(file);

			// Kiểm tra và cập nhật capacity
			validateAndUpdateCapacity(creatorId, file.getSize(), category);

			// Upload file
			String url = fireBaseService.uploadFileByCustomer(creatorId, category, file);

			return url;

		} catch (IllegalArgumentException e) {
			throw e;
		} catch (Exception e) {
			throw new ApiException("Lỗi khi lưu file: " + e.getMessage());
		}
	}

	/**
	 * Xóa file theo URL
	 * @param url URL của file cần xóa
	 * @param creatorId ID của creator
	 */
	@Transactional
	public void deleteFileByUrl(String url, String creatorId) {
		try {
			// 1. Kiểm tra creator tồn tại
			Creator creator = creatorRepository.findById(creatorId)
					.orElseThrow(() -> new FileUploadException("Creator không tồn tại"));

			// 2. Lấy thông tin file từ pending image (nếu có)
			Optional<PendingImage> pendingImage = pendingImgRepository.findByCreatorIdAndUrl(creatorId, url);

			// 3. Lấy kích thước file trước khi xóa (để trừ capacity)
			long fileSize = 0;
			if (pendingImage.isPresent()) {
				// Nếu có lưu size trong PendingImage thì lấy ra
				// fileSize = pendingImage.get().getFileSize();

				// Xóa khỏi pending
				pendingImgRepository.delete(pendingImage.get());
			} else {
				// Nếu không có trong pending, có thể lấy từ Firebase

			}

			// 4. Xóa file khỏi Firebase
			String result = fireBaseService.deleteImgByUrl(url);

			// 5. Cập nhật capacity (trừ đi dung lượng đã xóa)
			if (fileSize > 0) {
				updateCapacityAfterDelete(creatorId, fileSize);
			}

		} catch (FileUploadException e) {
			throw e;
		} catch (Exception e) {
			throw new FileUploadException("Lỗi khi xóa file: " + e.getMessage());
		}
	}

	/**
	 * Moderate image để kiểm tra nội dung không phù hợp
	 * @param file file cần kiểm tra
	 * @throws IOException
	 */

	/**
	 * Lấy thông tin dung lượng storage của creator
	 * @param creatorId ID creator
	 * @return thông tin capacity
	 */
	public CreatorMediaCapacity getCreatorStorageInfo(String creatorId) {
		return creatorMediaCapacityRepository.findByCreatorId(creatorId);
	}

	/**
	 * Kiểm tra xem creator có thể upload file với size cụ thể không
	 * @param creatorId ID creator
	 * @param fileSize kích thước file
	 * @return true nếu có thể upload
	 */
	public boolean canUploadFile(String creatorId, long fileSize) {
		CreatorMediaCapacity capacity = creatorMediaCapacityRepository.findByCreatorId(creatorId);

		if (capacity == null) {
			return false;
		}

		long newTotal = capacity.getCapacity() + fileSize;
		return newTotal <= MAX_FOLDER_SIZE_BYTES;
	}

	// ==================== PRIVATE HELPER METHODS ====================

	/**
	 * Validate file input
	 */
	private void validateFileInput(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("File không được để trống");
		}
	}

	/**
	 * Xác định FileCategory từ TypeOfFile
	 */
	private FileCategory determineFileCategory(TypeOfFile type) {
		if (TypeOfFile.IMG.equals(type)) {
			return FileCategory.IMG;
		} else if (TypeOfFile.PDF.equals(type)) {
			return FileCategory.PDF;
		} else {
			throw new IllegalArgumentException("Loại file không hợp lệ: " + type);
		}
	}

	/**
	 * Kiểm tra kích thước file đơn lẻ
	 */
	private void validateSingleFileSize(MultipartFile file) {
		if (file.getSize() > MAX_FILE_SIZE_BYTES) {
			double fileSizeMB = file.getSize() / (1024.0 * 1024.0);
			throw new IllegalArgumentException(
					String.format("File quá lớn (%.2fMB). Kích thước tối đa cho 1 file: %dMB",
							fileSizeMB, MAX_FILE_SIZE_BYTES / (1024 * 1024))
			);
		}
	}

	/**
	 * Kiểm tra và cập nhật capacity
	 */

	private void validateAndUpdateCapacity(String creatorId, long fileSize, FileCategory category) {
		// Lấy capacity hiện tại
		CreatorMediaCapacity capacity = creatorMediaCapacityRepository.findByCreatorId(creatorId);

		if (capacity == null) {
			// Tạo mới nếu chưa có
			capacity = new CreatorMediaCapacity();
			capacity.setCreatorId(creatorId);
			capacity.setCapacity(0L);
		}

		long currentCapacity = capacity.getCapacity();
		long newTotalCapacity = currentCapacity + fileSize;

		// Kiểm tra vượt quá giới hạn
		if (newTotalCapacity > MAX_FOLDER_SIZE_BYTES) {
			double currentMB = currentCapacity / (1024.0 * 1024.0);
			double fileSizeMB = fileSize / (1024.0 * 1024.0);
			double newTotalMB = newTotalCapacity / (1024.0 * 1024.0);

			throw new IllegalArgumentException(
					String.format(
							"Vượt quá giới hạn dung lượng! " +
									"Đã dùng: %.2fMB, File mới: %.2fMB, Tổng: %.2fMB, Giới hạn: %dMB",
							currentMB, fileSizeMB, newTotalMB, MAX_FOLDER_SIZE_MB
					)
			);
		}

		// Cập nhật capacity
		capacity.setCapacity(newTotalCapacity);
		creatorMediaCapacityRepository.save(capacity);
	}

	/**
	 * Cập nhật capacity sau khi xóa file
	 */

	private void updateCapacityAfterDelete(String creatorId, long fileSize) {
		CreatorMediaCapacity capacity = creatorMediaCapacityRepository.findByCreatorId(creatorId);

		if (capacity != null) {
			long newCapacity = Math.max(0, capacity.getCapacity() - fileSize);
			capacity.setCapacity(newCapacity);
			creatorMediaCapacityRepository.save(capacity);
		}
	}
}