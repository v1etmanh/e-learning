package com.jpd.web.dto;

import java.util.List;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;

import com.jpd.web.model.Status;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
@AllArgsConstructor
@Data
@RequiredArgsConstructor
@Schema(name = "CreatorDto", description = "Creator profile as stored after a successful application.")
public class CreatorDto {
	@NotBlank(message = "Họ tên không được để trống")
    @Size(max = 100, message = "Họ tên không được vượt quá 100 ký tự")
    @Schema(description = "Full legal name shown on the creator's public profile.", example = "Nguyễn Thị Thuỷ", maxLength = 100)
    private String fullName;

    @Pattern(regexp = "^[0-9]{10,11}$", message = "Số điện thoại không hợp lệ")
    @Schema(description = "Contact phone number, digits only.", example = "0912345678", pattern = "^[0-9]{10,11}$")
    private String phone;

    @Size(max = 1000, message = "Tiểu sử không được vượt quá 1000 ký tự")
    @Schema(description = "Short biography describing teaching background and expertise.",
            example = "Giáo viên tiếng Nhật 5 năm kinh nghiệm, chuyên luyện thi JLPT N3-N1.", maxLength = 1000)
    private String bio;

    // Profile image - single file
    @NotBlank
    @Schema(description = "Public Firebase Storage URL of the uploaded avatar. Null when the application was submitted without a `profileImage`.",
            example = "https://firebasestorage.googleapis.com/v0/b/jaen-elearning.appspot.com/o/img%2F7f3c1a90-avatar.jpg?alt=media")
    private String imgUrl;

    @Schema(description = "Public URLs of uploaded qualification certificates. Null on a freshly submitted application — certificates are attached later.",
            example = "null")
    private List<String>certificateUrl;

    @Schema(description = "PayPal account used to pay out creator earnings. Null until the creator configures payouts.",
            example = "null")
    private String paypalEmail;

    @Schema(description = "Review state of the creator profile. A new application is always `PENDING`.", example = "PENDING")
    private Status status;
    
    
    
}
