package com.example.chronicle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;

import com.bumptech.glide.Glide;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Calendar;

public class NoteDetailsActivity extends AppCompatActivity {

    private EditText titleEditText, contentEditText;
    private ImageButton saveNoteBtn;
    private ImageView noteImageView;
    private String title, content, docId;
    private boolean isEditMode = false;

    private ImageButton deleteNoteIcon, editModeBtn;
    private static final int PICK_IMAGE_REQUEST = 1;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private Uri selectedImageUri;
    private String selectedTime = "";
    private Note note;


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_details);

        titleEditText = findViewById(R.id.notes_title_text);
        contentEditText = findViewById(R.id.notes_content_text);
        saveNoteBtn = findViewById(R.id.save_note_btn);
        deleteNoteIcon = findViewById(R.id.delete_note_icon);
        noteImageView = findViewById(R.id.note_image_view);
        editModeBtn = findViewById(R.id.edit_mode_btn);
        editModeBtn.setOnClickListener(v -> toggleEditMode());

        note = new Note();

        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        noteImageView.setImageURI(imageUri);
                        selectedImageUri = imageUri;
                    }
                });

        ImageButton moodIconImageView = findViewById(R.id.mood_icon);
        moodIconImageView.setOnClickListener(v -> showMoodSelectionDialog());

        int selectedMoodIcon = getSelectedMoodFromSharedPreferences();
        moodIconImageView.setImageResource(selectedMoodIcon);

        // Set the selected mood in your Note object
        note.setSelectedMood(selectedMoodIcon);

        title = getIntent().getStringExtra("title");
        content = getIntent().getStringExtra("content");
        docId = getIntent().getStringExtra("docId");

        isEditMode = docId != null && !docId.isEmpty();

        titleEditText.setText(title);
        contentEditText.setText(content);

        selectedTime = getSelectedTimeFromSharedPreferences();
        if (!selectedTime.isEmpty()) {
            TextView timeTextView = findViewById(R.id.time_text_view);
            timeTextView.setText(selectedTime);
        }

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
                            selectedTime = note.getSelectedTime();
                            updateSelectedTime(selectedTime);
                            // Log the retrieved imageUrl
//                            Log.d("ImageUrl", "Retrieved Image URL: " + selectedImageUri.toString());
                            TextView timeTextView = findViewById(R.id.time_text_view); // Replace with your TextView
                            timeTextView.setText(selectedTime);
                            timeTextView.invalidate();

                            // Load image using Glide if imageUrl is available
                            loadImageIntoImageView(selectedImageUri);
                            showTimePickerDialog(null);
                        }
                    }
                } else {
                    Log.d("ImageUrl", "Failed to retrieve Image URL");
                }
            });
            setReadOnlyMode();
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

    private void toggleEditMode(){
        isEditMode = !isEditMode;
        if (isEditMode){
            setEditMode();
        }
        else {
            setReadOnlyMode();
        }
    }

    private void setReadOnlyMode(){
        titleEditText.setEnabled(false);
        contentEditText.setEnabled(false);
        saveNoteBtn.setVisibility(View.GONE);
        editModeBtn.setImageResource(R.drawable.baseline_edit_note_24);

        ImageButton timeIcon = findViewById(R.id.time_picker);
        timeIcon.setEnabled(false);
        ImageButton imageIcon = findViewById(R.id.insert_image_btn);
        imageIcon.setEnabled(false);
        ImageButton moodIcon = findViewById(R.id.mood_icon);
        moodIcon.setEnabled(false);
    }

    private void setEditMode() {
        titleEditText.setEnabled(true);
        contentEditText.setEnabled(true);
        saveNoteBtn.setVisibility(View.VISIBLE);
//        editModeBtn.setImageResource(R.drawable.baseline_done_24);
        ImageButton timeIcon = findViewById(R.id.time_picker);
        timeIcon.setEnabled(true);
        ImageButton imageIcon = findViewById(R.id.insert_image_btn);
        imageIcon.setEnabled(true);
        ImageButton moodIcon = findViewById(R.id.mood_icon);
        moodIcon.setEnabled(true);
    }

    private void saveSelectedTimeToSharedPreferences(String time) {
        SharedPreferences sharedPreferences = getSharedPreferences("NoteDetailsPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("selectedTime", time);
        editor.apply();
    }

    private String getSelectedTimeFromSharedPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences("NoteDetailsPrefs", MODE_PRIVATE);
        return sharedPreferences.getString("selectedTime", "");
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
        Note note = new Note();
        note.setTitle(noteTitle);
        note.setContent(noteContent);
        note.setTimestamp(Timestamp.now());

        if (noteTitle.isEmpty()) {
            titleEditText.setError("Title is required");
            return;
        }

        if (!selectedTime.isEmpty()) {
            note.setSelectedTime(selectedTime);
            noteContent += "\n\nTime: " + selectedTime;
        }

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
        note.setSelectedTime(selectedTime);
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
                utility.showToast(NoteDetailsActivity.this, "Entry deleted successfully");
                finish();
            } else {
                utility.showToast(NoteDetailsActivity.this, "Failed to delete Entry");
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
                utility.showToast(NoteDetailsActivity.this, "Entry added successfully");
                finish();
            } else {
                utility.showToast(NoteDetailsActivity.this, "Failed to add Entry");
            }
        });
    }

    private void saveNoteWithNoImage(DocumentReference documentReference, Note note) {
        documentReference.set(note).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                utility.showToast(NoteDetailsActivity.this, "Entry added successfully");
                finish();
            } else {
                utility.showToast(NoteDetailsActivity.this, "Failed to add Entry");
            }
        });
    }

    // Method to show the time picker dialog
    public void showTimePickerDialog(View view) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        // Handle the selected time
                        selectedTime = hourOfDay + ":" + minute;
                        updateSelectedTime(selectedTime);

                        saveSelectedTimeToSharedPreferences(selectedTime);

                        TextView timeTextView = findViewById(R.id.time_text_view);
                        timeTextView.setText(selectedTime);
//                        displaySelectedTime(selectedTime);
                        // Do something with the selected time (e.g., update UI)
//                        TextView timeTextView = findViewById(R.id.time_text_view); // Replace with your TextView
//                        timeTextView.setText(selectedTime);
                    }
                },
                hour,
                minute,
                DateFormat.is24HourFormat(this)
        );

        timePickerDialog.show();
    }
    private void updateSelectedTime(String time) {
        selectedTime = time;
        TextView timeTextView = findViewById(R.id.time_text_view);
        timeTextView.setText(selectedTime);
        timeTextView.invalidate();
    }

    private void saveSelectedMoodToSharedPreferences(int selectedMoodIcon) {
        SharedPreferences sharedPreferences = getSharedPreferences("NoteDetailsPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("selectedMoodIcon", selectedMoodIcon);
        editor.apply();
    }

    // Retrieve the selected mood from SharedPreferences
    private int getSelectedMoodFromSharedPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences("NoteDetailsPrefs", MODE_PRIVATE);
        return sharedPreferences.getInt("selectedMoodIcon", R.drawable.happy);
    }

    private void showMoodSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Mood");

        String[] moodOptions = {"Happy", "Sad", "Angry"};
        int[] moodIcons = {R.drawable.happy, R.drawable.sad, R.drawable.angry};

        builder.setItems(moodOptions, (dialog, which) -> {
            // Update the mood icon when an option is selected
            int selectedMoodIcon = moodIcons[which];
            ImageButton moodIconImageView = findViewById(R.id.mood_icon);
            moodIconImageView.setImageResource(selectedMoodIcon);

            saveSelectedMoodToSharedPreferences(selectedMoodIcon);
            // Update the selected mood in your Note object
            note.setSelectedMood(selectedMoodIcon);
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
