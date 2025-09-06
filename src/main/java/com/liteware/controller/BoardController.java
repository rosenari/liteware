package com.liteware.controller;

import com.liteware.model.dto.CommentDto;
import com.liteware.model.dto.PostDto;
import com.liteware.model.entity.User;
import com.liteware.model.entity.board.Board;
import com.liteware.model.entity.board.Post;
import com.liteware.repository.UserRepository;
import com.liteware.service.board.BoardService;
import com.liteware.service.board.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/board")
@RequiredArgsConstructor
public class BoardController {
    
    private final BoardService boardService;
    private final CommentService commentService;
    private final UserRepository userRepository;
    
    private User getCurrentUser(UserDetails userDetails) {
        if (userDetails == null) {
            return null;
        }
        return userRepository.findByLoginId(userDetails.getUsername()).orElse(null);
    }
    
    @GetMapping
    public String boardList(Model model) {
        model.addAttribute("boards", boardService.getActiveBoards());
        return "board/list";
    }
    
    @GetMapping("/{boardId}")
    public String postList(@PathVariable Long boardId,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "20") int size,
                          @RequestParam(required = false) String keyword,
                          Model model) {
        
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        Page<Post> posts;
        if (keyword != null && !keyword.isEmpty()) {
            posts = boardService.searchPosts(keyword, pageable);
            model.addAttribute("keyword", keyword);
        } else {
            posts = boardService.getPostsByBoard(boardId, pageable);
        }
        model.addAttribute("posts", posts);
        
        try {
            model.addAttribute("board", boardService.getActiveBoards().stream()
                    .filter(b -> b.getBoardId().equals(boardId))
                    .findFirst()
                    .orElse(null));
        } catch (Exception e) {
            log.error("Board not found", e);
        }
        
        model.addAttribute("notices", boardService.getNoticePosts(boardId));
        model.addAttribute("currentPage", page);
        
        return "board/posts";
    }
    
    @GetMapping("/{boardId}/write")
    public String writeForm(@PathVariable Long boardId,
                           @AuthenticationPrincipal UserDetails userDetails,
                           Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        
        model.addAttribute("boardId", boardId);
        model.addAttribute("post", new PostDto());
        
        return "board/write";
    }
    
    @PostMapping("/{boardId}/write")
    public String write(@PathVariable Long boardId,
                       @ModelAttribute PostDto postDto,
                       @AuthenticationPrincipal UserDetails userDetails,
                       RedirectAttributes redirectAttributes) {
        try {
            User currentUser = getCurrentUser(userDetails);
            if (currentUser == null) {
                return "redirect:/login";
            }
            
            postDto.setBoardId(boardId);
            postDto.setWriterId(currentUser.getUserId());
            
            Post post = boardService.createPost(postDto);
            redirectAttributes.addFlashAttribute("success", "게시글이 등록되었습니다.");
            
            return "redirect:/board/" + boardId + "/post/" + post.getPostId();
        } catch (Exception e) {
            log.error("Post write error", e);
            redirectAttributes.addFlashAttribute("error", "게시글 등록 중 오류가 발생했습니다.");
            return "redirect:/board/" + boardId + "/write";
        }
    }
    
    @GetMapping("/{boardId}/post/{postId}")
    public String viewPost(@PathVariable Long boardId,
                          @PathVariable Long postId,
                          @AuthenticationPrincipal UserDetails userDetails,
                          Model model) {
        try {
            Post post;
            User currentUser = getCurrentUser(userDetails);
            if (currentUser != null) {
                post = boardService.getPost(postId, currentUser.getUserId());
            } else {
                post = boardService.getPost(postId);
            }
            
            model.addAttribute("post", post);
            model.addAttribute("comments", commentService.getCommentsByPost(postId));
            model.addAttribute("boardId", boardId);
            
            // 수정/삭제 권한 체크
            if (currentUser != null) {
                model.addAttribute("canEdit", post.getWriter().getUserId().equals(currentUser.getUserId()));
            }
            
            return "board/view";
        } catch (Exception e) {
            log.error("Post view error", e);
            return "redirect:/board/" + boardId;
        }
    }
    
    @GetMapping("/{boardId}/post/{postId}/edit")
    public String editForm(@PathVariable Long boardId,
                          @PathVariable Long postId,
                          @AuthenticationPrincipal UserDetails userDetails,
                          Model model) {
        try {
            Post post = boardService.getPost(postId);
            
            // 권한 체크
            User currentUser = getCurrentUser(userDetails);
            if (currentUser == null || !post.getWriter().getUserId().equals(currentUser.getUserId())) {
                return "redirect:/board/" + boardId + "/post/" + postId;
            }
            
            model.addAttribute("post", post);
            model.addAttribute("boardId", boardId);
            
            return "board/edit";
        } catch (Exception e) {
            log.error("Post edit form error", e);
            return "redirect:/board/" + boardId;
        }
    }
    
    @PostMapping("/{boardId}/post/{postId}/edit")
    public String edit(@PathVariable Long boardId,
                      @PathVariable Long postId,
                      @ModelAttribute PostDto postDto,
                      @AuthenticationPrincipal UserDetails userDetails,
                      RedirectAttributes redirectAttributes) {
        try {
            User currentUser = getCurrentUser(userDetails);
            if (currentUser == null) {
                return "redirect:/login";
            }
            boardService.updatePost(postId, postDto, currentUser.getUserId());
            
            redirectAttributes.addFlashAttribute("success", "게시글이 수정되었습니다.");
            return "redirect:/board/" + boardId + "/post/" + postId;
        } catch (Exception e) {
            log.error("Post edit error", e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/board/" + boardId + "/post/" + postId + "/edit";
        }
    }
    
    @PostMapping("/{boardId}/post/{postId}/delete")
    public String delete(@PathVariable Long boardId,
                        @PathVariable Long postId,
                        @AuthenticationPrincipal UserDetails userDetails,
                        RedirectAttributes redirectAttributes) {
        try {
            User currentUser = getCurrentUser(userDetails);
            if (currentUser == null) {
                return "redirect:/login";
            }
            boardService.deletePost(postId, currentUser.getUserId());
            
            redirectAttributes.addFlashAttribute("success", "게시글이 삭제되었습니다.");
            return "redirect:/board/" + boardId;
        } catch (Exception e) {
            log.error("Post delete error", e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/board/" + boardId + "/post/" + postId;
        }
    }
    
    @PostMapping("/{boardId}/post/{postId}/comment")
    @ResponseBody
    public String addComment(@PathVariable Long boardId,
                            @PathVariable Long postId,
                            @RequestBody CommentDto commentDto,
                            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User currentUser = getCurrentUser(userDetails);
            if (currentUser == null) {
                return "error";
            }
            
            commentDto.setPostId(postId);
            commentDto.setWriterId(currentUser.getUserId());
            
            commentService.createComment(commentDto);
            return "success";
        } catch (Exception e) {
            log.error("Comment add error", e);
            return "error";
        }
    }
}