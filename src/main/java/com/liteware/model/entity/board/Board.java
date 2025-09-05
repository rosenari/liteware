package com.liteware.model.entity.board;

import com.liteware.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "boards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(exclude = {"posts"})
@EqualsAndHashCode(of = "boardId", callSuper = false)
public class Board extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_id")
    private Long boardId;
    
    @Column(name = "board_code", unique = true, nullable = false, length = 50)
    private String boardCode;
    
    @Column(name = "board_name", nullable = false, length = 100)
    private String boardName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "board_type", nullable = false, length = 20)
    private BoardType boardType;
    
    @Column(name = "description", length = 500)
    private String description;
    
    @Column(name = "use_yn", nullable = false)
    @Builder.Default
    private Boolean useYn = true;
    
    @Column(name = "write_auth_level")
    @Builder.Default
    private Integer writeAuthLevel = 1;
    
    @Column(name = "read_auth_level")
    @Builder.Default
    private Integer readAuthLevel = 1;
    
    @Column(name = "comment_auth_level")
    @Builder.Default
    private Integer commentAuthLevel = 1;
    
    @Column(name = "attachment_yn")
    @Builder.Default
    private Boolean attachmentYn = true;
    
    @Column(name = "secret_yn")
    @Builder.Default
    private Boolean secretYn = false;
    
    @Column(name = "notice_yn")
    @Builder.Default
    private Boolean noticeYn = true;
    
    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;
    
    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Post> posts = new ArrayList<>();
}