package org.zerock.board.entity;

import com.fasterxml.jackson.databind.ser.Serializers;
import lombok.*;
import org.springframework.jmx.export.annotation.ManagedNotification;

import javax.persistence.*;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@ToString
@Builder
public class Reply extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long rno;

    private String text;

    private String replyer;

    @ManyToOne(fetch = FetchType.LAZY)
    private Board board;    //연관관계 지정
}
