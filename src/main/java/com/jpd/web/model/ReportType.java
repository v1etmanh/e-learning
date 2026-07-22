package com.jpd.web.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ReportType", description = """
        Reason a learner is reporting a course.

        * `INAPPROPRIATE_CONTENT` — offensive, obscene or otherwise unsuitable material
        * `MISLEADING_INFORMATION` — false or misleading claims
        * `COPYRIGHT_VIOLATION` — infringes someone else's copyright
        * `DISCRIMINATION_OR_HATE` — hate speech or discriminatory language
        * `POOR_QUALITY` — the course quality is unacceptably low
        * `SCAM_OR_FRAUD` — fraud, or demands for payment outside the platform
        * `RELIGIOUS_OR_POLITICAL_CONTENT` — unsuitable religious or political material
        * `OTHER` — anything not covered above; explain in `detail`
        """)
public enum ReportType {
	INAPPROPRIATE_CONTENT,//Nội dung phản cảm, tục tĩu, không phù hợp
	MISLEADING_INFORMATION,//Thông tin sai lệch hoặc gây hiểu nhầm
	COPYRIGHT_VIOLATION,//Vi phạm bản quyền
	DISCRIMINATION_OR_HATE,//Ngôn từ thù ghét hoặc phân biệt đối xử
	POOR_QUALITY,//Chất lượng khóa học kém
	SCAM_OR_FRAUD,//Lừa đảo hoặc yêu cầu thanh toán bất hợp pháp
	RELIGIOUS_OR_POLITICAL_CONTENT,//Nội dung tôn giáo hoặc chính trị không phù hợp
	OTHER//Khác
}
