package com.jpd.web.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.jpd.web.service.utils.FileCategory;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.firebase.cloud.StorageClient;
import com.jpd.web.model.ModuleContent;
import com.jpd.web.model.PendingImage;
import com.jpd.web.model.Status;
import com.jpd.web.model.TypeOfFile;
import com.jpd.web.repository.PendingImgRepository;
//service to handle storage img or pdf 
@Service
public class FireBaseService {

	@Autowired
	    private StorageClient storageClient;
	    
	    public String uploadFile(MultipartFile file,TypeOfFile moduleContent) throws IOException {
	        try {
	      
	            // Lấy bucket từ StorageClient
	            Bucket bucket = storageClient.bucket();
	            
	            // Tạo tên file unique
				//tao folder customerId neu ch dc tao

	            String fileName = moduleContent.toString()+"/" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
	            
	            // Upload file
	            Blob blob = bucket.create(fileName, file.getBytes(), file.getContentType());
	            
	            // Tạo public URL
	            String downloadUrl = String.format(
	                "https://firebasestorage.googleapis.com/v0/b/%s/o/%s?alt=media",
	                bucket.getName(),
	                fileName.replace("/", "%2F")
	            );
	           
	            return downloadUrl;
	            
	        } catch (Exception e) {
	            throw new FileUploadException("Failed to upload file: " + e.getMessage(), e);
	        }
	    }
	    public String deleteImgByUrl(String url) {
	        try {
	            // 1️⃣ Lấy bucket hiện tại
	            Bucket bucket = storageClient.bucket();

	            // 2️⃣ Từ URL public -> tách ra đường dẫn file trong bucket
	            // URL dạng: https://firebasestorage.googleapis.com/v0/b/your-bucket-name/o/FOLDER%2FfileName.png?alt=media
	            // Ta cần lấy "FOLDER/fileName.png"
	            String prefix = "https://firebasestorage.googleapis.com/v0/b/" + bucket.getName() + "/o/";
	            String suffix = "?alt=media";
	            String objectName = url.substring(prefix.length(), url.indexOf(suffix));
	            objectName = objectName.replace("%2F", "/"); // decode lại

	            // 3️⃣ Lấy đối tượng Blob (file) từ bucket
	            Blob blob = bucket.get(objectName);
	            if (blob == null) {
	                return "❌ File not found in Firebase Storage.";
	            }

	            // 4️⃣ Xóa file
	            boolean deleted = blob.delete();
	            if (deleted) {
	                return "✅ File deleted successfully.";
	            } else {
	                return "⚠️ File could not be deleted (might not exist).";
	            }

	        } catch (Exception e) {
	            e.printStackTrace();
	            return "❌ Error deleting file: " + e.getMessage();
	        }
	    }
	    @Autowired
	    private PendingImgRepository pendingImageRepository;
	    @Scheduled(cron = "0 0 3 * * ?")
	    public void cleanPendingImages() {
	        List<PendingImage> oldImages = pendingImageRepository.findByStatus(Status.PENDING);
	        for (PendingImage img : oldImages) {
	            deleteImgByUrl(img.getUrl());
	            pendingImageRepository.delete(img);
	        }
	         }
	    
	    public byte[] getFileFromUrl(String url) throws IOException {
	        try {
	            if (!StringUtils.hasText(url)) {
	                throw new IllegalArgumentException("URL không hợp lệ");
	            }
	            
	            Bucket bucket = storageClient.bucket();
	            
	            // Tách path file từ URL
	            String prefix = "https://firebasestorage.googleapis.com/v0/b/" + bucket.getName() + "/o/";
	            
	            if (!url.contains(prefix)) {
	                throw new IllegalArgumentException("URL không phải từ Firebase Storage");
	            }
	            
	            String objectName = url.substring(prefix.length());
	            
	            // Xóa query parameter
	            if (objectName.contains("?")) {
	                objectName = objectName.substring(0, objectName.indexOf("?"));
	            }
	            
	            // Decode URL encoding
	            objectName = objectName.replace("%2F", "/");
	            
	            // Lấy file từ Firebase
	            Blob blob = bucket.get(objectName);
	            
	            if (blob == null) {
	                throw new IOException("File không tồn tại: " + objectName);
	            }
	            
	            return blob.getContent();
	            
	        } catch (Exception e) {
	            throw new IOException("Lỗi tải file: " + e.getMessage(), e);
	        }
	    }
	public String createFolder(String folderPath) throws IOException {
		try {
			Bucket bucket = storageClient.bucket();

			// Normalize path
			String normalizedPath = folderPath.trim().replaceAll("^/+|/+$", "");

			// Tạo file .placeholder để đánh dấu folder
			String placeholderFileName = normalizedPath + "/.placeholder";

			// Upload file rỗng
			Blob blob = bucket.create(placeholderFileName, new byte[0], "text/plain");

			return "Folder created: " + normalizedPath;

		} catch (Exception e) {
			throw new IOException("Failed to create folder: " + e.getMessage(), e);
		}
	}
	/**
	 * Upload file vào cấu trúc folder theo customerId
	 * @param customerId ID của customer
	 * @param category loại file (CERTIFICATE, IMG, PDF)
	 * @param file file cần upload
	 * @return URL của file đã upload
	 */
	public String uploadFileByCustomer(String customerId, FileCategory category, MultipartFile file) throws IOException {
		try {
			// Validate input
			if (!StringUtils.hasText(customerId)) {
				throw new IllegalArgumentException("Customer ID không được để trống");
			}

			if (file == null || file.isEmpty()) {
				throw new IllegalArgumentException("File không hợp lệ");
			}

			// Lấy bucket từ StorageClient
			Bucket bucket = storageClient.bucket();

			// Xây dựng đường dẫn: customerFile/{customerId}/{category}/{timestamp_filename}
			String filePath = String.format("customerFile/%s/%s/%d_%s",
					customerId,
					category.getPath(),
					System.currentTimeMillis(),
					file.getOriginalFilename()
			);

			// Upload file (folder sẽ tự động được tạo)
			Blob blob = bucket.create(filePath, file.getBytes(), file.getContentType());

			// Tạo public URL
			String downloadUrl = String.format(
					"https://firebasestorage.googleapis.com/v0/b/%s/o/%s?alt=media",
					bucket.getName(),
					filePath.replace("/", "%2F")
			);

			return downloadUrl;

		} catch (Exception e) {
			throw new FileUploadException("Failed to upload file: " + e.getMessage(), e);
		}
	}

	/**
	 * Upload file certificate ở root level (ngoài customerFile)
	 * @param file file certificate
	 * @return URL của file
	 */
	public String uploadGlobalCertificate(MultipartFile file,String customerId) throws IOException {
		try {
			if (file == null || file.isEmpty()) {
				throw new IllegalArgumentException("File không hợp lệ");
			}

			Bucket bucket = storageClient.bucket();

			// Đường dẫn: Certificate/{timestamp_filename}
			String filePath = String.format("certificates/%s/%s", customerId, file.getOriginalFilename());

			Blob blob = bucket.create(filePath, file.getBytes(), file.getContentType());

			String downloadUrl = String.format(
					"https://firebasestorage.googleapis.com/v0/b/%s/o/%s?alt=media",
					bucket.getName(),
					filePath.replace("/", "%2F")
			);

			return downloadUrl;

		} catch (Exception e) {
			throw new FileUploadException("Failed to upload certificate: " + e.getMessage(), e);
		}
	}

	/**
	 * Lấy tất cả file của một customer theo category
	 * @param customerId ID customer
	 * @param category loại file
	 * @return List URL của các file
	 */
	public List<String> getCustomerFiles(String customerId, FileCategory category) {
		List<String> fileUrls = new ArrayList<>();

		try {
			Bucket bucket = storageClient.bucket();

			// Đường dẫn folder cần tìm
			String prefix = String.format("customerFile/%s/%s/", customerId, category.getPath());

			// Lấy tất cả file trong folder
			Iterable<Blob> blobs = bucket.list(com.google.cloud.storage.Storage.BlobListOption.prefix(prefix)).iterateAll();

			for (Blob blob : blobs) {
				// Bỏ qua placeholder và folder
				if (!blob.getName().endsWith("/") && !blob.getName().endsWith(".placeholder")) {
					String url = String.format(
							"https://firebasestorage.googleapis.com/v0/b/%s/o/%s?alt=media",
							bucket.getName(),
							blob.getName().replace("/", "%2F")
					);
					fileUrls.add(url);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return fileUrls;
	}

	/**
	 * Xóa tất cả file của một customer
	 * @param customerId ID customer
	 * @return thông báo kết quả
	 */
	public String deleteAllCustomerFiles(String customerId) {
		try {
			Bucket bucket = storageClient.bucket();
			String prefix = "customerFile/" + customerId + "/";

			Iterable<Blob> blobs = bucket.list(com.google.cloud.storage.Storage.BlobListOption.prefix(prefix)).iterateAll();

			int deletedCount = 0;
			for (Blob blob : blobs) {
				if (blob.delete()) {
					deletedCount++;
				}
			}

			return String.format("✅ Đã xóa %d file của customer %s", deletedCount, customerId);

		} catch (Exception e) {
			return "❌ Error: " + e.getMessage();
		}
	}
}

