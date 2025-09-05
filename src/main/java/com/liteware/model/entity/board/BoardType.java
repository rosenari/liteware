package com.liteware.model.entity.board;

public enum BoardType {
    NOTICE("공지사항"),
    GENERAL("일반게시판"),
    QNA("질문답변"),
    FAQ("FAQ"),
    DATA("자료실");
    
    private final String description;
    
    BoardType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}