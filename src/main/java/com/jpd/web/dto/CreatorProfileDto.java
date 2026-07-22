package com.jpd.web.dto;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@AllArgsConstructor
@RequiredArgsConstructor
@Data
@Schema(name = "CreatorProfileDto",
        description = "Multipart form submitted by a customer applying to become a creator. "
                + "The creator id and email are taken from the bearer token, not from this form.")
public class CreatorProfileDto {



    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 100, message = "Họ tên không được vượt quá 100 ký tự")
    @Schema(description = "Full legal name shown on the creator's public profile.",
            example = "Nguyễn Thị Thuỷ", maxLength = 100, requiredMode = Schema.RequiredMode.REQUIRED)
    private String fullName;

    @Pattern(regexp = "^[0-9]{10,11}$", message = "Số điện thoại không hợp lệ")
    @Schema(description = "Contact phone number: digits only, 10 or 11 characters. Optional, but rejected if present and malformed.",
            example = "0912345678", pattern = "^[0-9]{10,11}$", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String phone;

    @Size(max = 1000, message = "Tiểu sử không được vượt quá 1000 ký tự")
    @Schema(description = "Short biography describing teaching background and expertise.",
            example = "Giáo viên tiếng Nhật 5 năm kinh nghiệm, chuyên luyện thi JLPT N3-N1.",
            maxLength = 1000, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String bio;

    // Profile image - single file
    @Schema(description = "Avatar image file. Optional — when supplied it is uploaded to Firebase Storage "
            + "and the resulting public URL is returned as `imgUrl` in the response.",
            type = "string", format = "binary", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private MultipartFile profileImage;

    @NotNull(message = "Bạn phải đồng ý với điều khoản")
    @Schema(description = "Acceptance of the creator terms and conditions. Must be present.",
            example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean agreedToTerms;
    
  
    
 
    
  
}

