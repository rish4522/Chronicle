package com.example.chronicle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class NoteDetailsActivity extends AppCompatActivity {

    private EditText titleEditText, contentEditText;
    private ImageButton saveNoteBtn;
    private ImageView noteImageView;
    private String title, content, docId;
    private boolean isEditMode = false;

    private ImageButton deleteNoteIcon;
    private static final int PICK_IMAGE_REQUEST = 1;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_details);

        titleEditText = findViewById(R.id.notes_title_text);
        contentEditText = findViewById(R.id.notes_content_text);
        saveNoteBtn = findViewById(R.id.save_note_btn);
        deleteNoteIcon = findViewById(R.id.delete_note_icon);
        noteImageView = findViewById(R.id.note_image_view);

        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        noteImageView.setImageURI(imageUri);
                        selectedImageUri = imageUri;
                    }
                });

        title = getIntent().getStringExtra("title");
        content = getIntent().getStringExtra("content");
        docId = getIntent().getStringExtra("docId");

        isEditMode = docId != null && !docId.isEmpty();

        titleEditText.setText(title);
        contentEditText.setText(content);

        if (isEditMode) {
            // Retrieve the imageUrl from Firestore and set it to selectedImageUri
            DocumentReference noteRef = utility.getCollectionReferenceForNotes().document(docId);
            noteRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Note note = document.toObject(Note.class);
                        if (note != null && note.getImageUrl() != null) {
                            selectedImageUri = Uri.parse(note.getImageUrl());

                            // Log the retrieved imageUrl
                            Log.d("ImageUrl", "Retrieved Image URL: " + selectedImageUri.toString());

                            // Load image using Glide if imageUrl is available
                            loadImageIntoImageView(selectedImageUri);
                        }
                    }
                } else {
                    Log.d("ImageUrl", "Failed to retrieve Image URL");
                }
            });
        }

        noteImageView.setOnClickListener(v -> openImagePicker());
        noteImageView.setOnLongClickListener(v -> {
            showImageDeleteConfirmationDialog();
            return true;
        });

        saveNoteBtn.setOnClickListener(v -> saveNote());
        deleteNoteIcon.setOnClickListener(v -> showDeleteConfirmationDialog());
        ImageButton insertImageBtn = findViewById(R.id.insert_image_btn);
        insertImageBtn.setOnClickListener(v -> openImagePicker());
    }

    private void loadImageIntoImageView(Uri imageUri) {
        Glide.with(this).load(imageUri).into(noteImageView);
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

private void showImageDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Image");
        builder.setMessage("Are you sure you want to delete this image?");
        builder.setPositiveButton("Delete", (dialog, which) -> deleteImage());
        builder.setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void deleteImage() {
        // Clear the image from the ImageView
        noteImageView.setImageDrawable(null);
    }

    private void saveNote() {
        String noteTitle = titleEditText.getText().toString();
        String noteContent = contentEditText.getText().toString();
        if (noteTitle.isEmpty()) {
            titleEditText.setError("Title is required");
            return;
        }
        Note note = new Note();
        note.setTitle(noteTitle);
        note.setContent(noteContent);
        note.setTimestamp(Timestamp.now());

        saveNoteToFirebase(note);
    }

    private void saveNoteToFirebase(Note note) {
        DocumentReference documentReference;
        if (isEditMode) {
            documentReference = utility.getCollectionReferenceForNotes().document(docId);
        } else {
            documentReference = utility.getCollectionReferenceForNotes().document();
        }
        note.setFavorite(false);
        note.setTimestamp(Timestamp.now());
        if (selectedImageUri != null) {
            uploadImageAndSaveNote(documentReference, note);
        } else {
            saveNoteWithNoImage(documentReference, note);
        }
    }

    private void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Note");
        builder.setMessage("Are you sure you want to delete this note?");
        builder.setPositiveButton("Delete", (dialogInterface, i) -> performDeleteNote());
        builder.setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void performDeleteNote() {
        DocumentReference documentReference = utility.getCollectionReferenceForNotes().document(docId);
        documentReference.delete().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                utility.showToast(NoteDetailsActivity.this, "Note deleted successfully");
                finish();
            } else {
                utility.showToast(NoteDetailsActivity.this, "Failed to delete Note");
            }
        });
    }

    private void uploadImageAndSaveNote(DocumentReference documentReference, Note note) {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference imageRef = storageRef.child("images/" + selectedImageUri.getLastPathSegment());

        imageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        note.setImageUrl(uri.toString());
                        saveNoteWithImage(documentReference, note);
                    });
                })
                .addOnFailureListener(exception -> {
                    // Handle image upload failure.
                });
    }

    private void saveNoteWithImage(DocumentReference documentReference, Note note) {
        documentReference.set(note).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                utility.showToast(NoteDetailsActivity.this, "Note added successfully");
                finish();
            } else {
                utility.showToast(NoteDetailsActivity.this, "Failed to add Note");
            }
        });
    }

    private void saveNoteWithNoImage(DocumentReference documentReference, Note note) {
        documentReference.set(note).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                utility.showToast(NoteDetailsActivity.this, "Note added successfully");
                finish();
            } else {
                utility.showToast(NoteDetailsActivity.this, "Failed to add Note");
            }
        });
    }
}
