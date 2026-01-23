package com.uni.research.module.note.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.uni.research.module.note.entity.Note;

import java.util.List;

/**
 * Note Service
 */
public interface NoteService extends IService<Note> {

    /**
     * Create note
     */
    Note createNote(Note note);

    /**
     * Update note
     */
    Note updateNote(Long id, Note note);

    /**
     * Delete note
     */
    void deleteNote(Long id);

    /**
     * Get notes by document ID
     */
    List<Note> getDocumentNotes(Long docId);

    /**
     * Search notes by keyword
     */
    List<Note> searchNotes(String keyword);
}
