package org.zerock.board.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@ToString
@Builder
@NoArgsConstructor
public class BoardDTO {
    private Long bno;
    private String title, content, writerEmail, writerName;
    private LocalDateTime regDate, modDate;
    private int replyCount;

}
