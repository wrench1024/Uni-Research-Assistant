package com.uni.research.module.note.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.uni.research.module.auth.entity.User;
import com.uni.research.module.auth.mapper.UserMapper;
import com.uni.research.module.note.entity.Note;
import com.uni.research.module.note.mapper.NoteMapper;
import com.uni.research.module.note.service.NoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Note Service Implementation
 */
@Service
@RequiredArgsConstructor
public class NoteServiceImpl extends ServiceImpl<NoteMapper, Note> implements NoteService {

    private final UserMapper userMapper;

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("用户未认证");
        }

        String username;
        Object principal = authentication.getPrincipal();
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            username = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }

        // Use LambdaQueryWrapper to query user by username
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        return user.getId();
    }

    @Override
    public Note createNote(Note note) {
        // Set user ID from context
        note.setUserId(getCurrentUserId());
        save(note);
        return note;
    }

    @Override
    public Note updateNote(Long id, Note note) {
        Note existingNote = getById(id);
        if (existingNote == null) {
            throw new RuntimeException("笔记不存在");
        }

        // Verify ownership
        if (!existingNote.getUserId().equals(getCurrentUserId())) {
            throw new RuntimeException("无权限修改此笔记");
        }

        // Update fields
        existingNote.setContent(note.getContent());
        existingNote.setTags(note.getTags());
        existingNote.setNoteType(note.getNoteType());
        existingNote.setPosition(note.getPosition());

        updateById(existingNote);
        return existingNote;
    }

    @Override
    public void deleteNote(Long id) {
        Note note = getById(id);
        if (note == null) {
            throw new RuntimeException("笔记不存在");
        }

        // Verify ownership
        if (!note.getUserId().equals(getCurrentUserId())) {
            throw new RuntimeException("无权限删除此笔记");
        }

        removeById(id);
    }

    @Override
    public List<Note> getDocumentNotes(Long docId) {
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Note::getUserId, getCurrentUserId())
                .eq(Note::getDocId, docId)
                .orderByDesc(Note::getCreateTime);
        return list(wrapper);
    }

    @Override
    public List<Note> searchNotes(String keyword) {
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Note::getUserId, getCurrentUserId())
                .and(w -> w.like(Note::getContent, keyword)
                        .or()
                        .like(Note::getTags, keyword))
                .orderByDesc(Note::getCreateTime);
        return list(wrapper);
    }
}
