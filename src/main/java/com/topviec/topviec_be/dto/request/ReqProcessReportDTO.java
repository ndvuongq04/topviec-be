package com.topviec.topviec_be.dto.request;

import com.topviec.topviec_be.enums.complaints.ComplaintType;
import com.topviec.topviec_be.enums.complaints.ViolationGroup;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReqProcessReportDTO {

    @NotBlank(message = "decision không được để trống")
    private String decision;

    private String action;

    private ComplaintType complaintType;

    private ViolationGroup violationGroup;

    private Integer points;

    private String resolutionNote;
}
