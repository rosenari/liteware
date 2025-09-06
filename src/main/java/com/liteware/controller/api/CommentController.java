package com.liteware.controller.api;

import com.liteware.model.dto.CommentDto;
import com.liteware.model.entity.User;
import com.liteware.repository.UserRepository;
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
    private final UserRepository userRepository;
    
    private User getCurrentUser(UserDetails userDetails) {
        if (userDetails == null) {
            return null;
        }
        return userRepository.findByLoginId(userDetails.getUsername()).orElse(null);
    }
    
    @PutMapping("/{commentId}")
    public ResponseEntity<?> updateComment(@PathVariable Long commentId,
                                          @RequestBody Map<String, String> body,
                                          @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String content = body.get("content");
            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("댓글 내용을 입력해주세요");
            }
            
            User currentUser = getCurrentUser(userDetails);
            if (currentUser == null) {
                return ResponseEntity.status(401).body("로그인이 필요합니다");
            }
            
            commentService.updateComment(commentId, content, currentUser.getUserId());
            
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
            User currentUser = getCurrentUser(userDetails);
            if (currentUser == null) {
                return ResponseEntity.status(401).body("로그인이 필요합니다");
            }
            
            commentService.deleteComment(commentId, currentUser.getUserId());
            
            return ResponseEntity.ok().body("success");
        } catch (Exception e) {
            log.error("Comment delete error", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}