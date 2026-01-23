package com.uni.research.module.note.controller;

import com.uni.research.common.result.Result;
import com.uni.research.module.note.entity.Note;
import com.uni.research.module.note.service.NoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Note Controller
 */
@RestController
@RequestMapping("/note")
@RequiredArgsConstructor
@Tag(name = "Note Management")
public class NoteController {

    private final NoteService noteService;

    @Operation(summary = "Create Note")
    @PostMapping
    public Result<Note> createNote(@RequestBody Note note) {
        return Result.success(noteService.createNote(note));
    }

    @Operation(summary = "Update Note")
    @PutMapping("/{id}")
    public Result<Note> updateNote(@PathVariable Long id, @RequestBody Note note) {
        return Result.success(noteService.updateNote(id, note));
    }

    @Operation(summary = "Delete Note")
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteNote(@PathVariable Long id) {
        noteService.deleteNote(id);
        return Result.success(true);
    }

    @Operation(summary = "Get Document Notes")
    @GetMapping("/doc/{docId}")
    public Result<List<Note>> getDocumentNotes(@PathVariable Long docId) {
        return Result.success(noteService.getDocumentNotes(docId));
    }

    @Operation(summary = "Search Notes")
    @GetMapping("/search")
    public Result<List<Note>> searchNotes(@RequestParam String keyword) {
        return Result.success(noteService.searchNotes(keyword));
    }
}
