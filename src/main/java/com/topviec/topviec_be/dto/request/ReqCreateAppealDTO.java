package com.topviec.topviec_be.dto.request;

import com.topviec.topviec_be.enums.complaints.EvidenceFileType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReqCreateAppealDTO {

    @NotNull(message = "ID báo cáo không được để trống")
    private Long complaintId;

    @NotBlank(message = "Nội dung kháng cáo không được để trống")
    @Size(max = 2000, message = "Nội dung kháng cáo không được vượt quá 2000 ký tự")
    private String content;

    @Valid
    @Builder.Default
    private List<EvidenceItem> evidences = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvidenceItem {

        @NotBlank(message = "fileUrl không được để trống")
        private String fileUrl;

        @NotNull(message = "fileType không được để trống")
        private EvidenceFileType fileType;
    }
}
