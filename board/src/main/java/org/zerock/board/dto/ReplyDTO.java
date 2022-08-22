package org.zerock.board.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReplyDTO {
    private Long rno;
    private String text;
    private String replyer;
    private Long bno;   //게시글 번호
    private LocalDateTime regDate, modDate;
}
