package com.liteware.controller.api;

import com.liteware.model.dto.CommentDto;
import com.liteware.service.board.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/comment")
@RequiredArgsConstructor
public class CommentController {
    
    private final CommentService commentService;
    
    @PutMapping("/{commentId}")
    public ResponseEntity<?> updateComment(@PathVariable Long commentId,
                                          @RequestBody Map<String, String> body,
                                          @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String content = body.get("content");
            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("댓글 내용을 입력해주세요");
            }
            
            Long userId = 1L; // TODO: Get from userDetails
            commentService.updateComment(commentId, content, userId);
            
            return ResponseEntity.ok().body("success");
        } catch (Exception e) {
            log.error("Comment update error", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable Long commentId,
                                          @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = 1L; // TODO: Get from userDetails
            commentService.deleteComment(commentId, userId);
            
            return ResponseEntity.ok().body("success");
        } catch (Exception e) {
            log.error("Comment delete error", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}