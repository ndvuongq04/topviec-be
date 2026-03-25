package com.topviec.topviec_be.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReqUpdateApplicationStatusDTO {

    @NotBlank(message = "Status cannot be blank")
    private String status;

    private String note;
}
