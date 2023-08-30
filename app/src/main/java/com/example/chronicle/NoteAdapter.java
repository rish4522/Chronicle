package com.example.chronicle;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.DocumentReference;

public class NoteAdapter extends FirestoreRecyclerAdapter<Note, NoteAdapter.NoteViewHolder> {
    Context context;

    public NoteAdapter(@NonNull FirestoreRecyclerOptions<Note> options, Context context) {
        super(options);
        this.context = context;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateOptions(@NonNull FirestoreRecyclerOptions<Note> newOptions) {
        notifyDataSetChanged();
    }

    @Override
    protected void onBindViewHolder(@NonNull NoteViewHolder holder, int position, @NonNull Note note) {
        holder.titleTextView.setText(note.title);
        holder.contentTextView.setText(note.content);
        holder.timestampTextView.setText(utility.timestampToString(note.timestamp));
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, NoteDetailsActivity.class);
            intent.putExtra("title", note.title);
            intent.putExtra("content", note.content);
            String docId = this.getSnapshots().getSnapshot(position).getId();
            intent.putExtra("docId", docId);
            context.startActivity(intent);
        });
        if (note.isFavorite()) {
            holder.favoriteIcon.setImageResource(R.drawable.baseline_favorite_filled_24);
        } else if (!note.isFavorite()) {
            holder.favoriteIcon.setImageResource(R.drawable.baseline_favorite_24);
        }
//        else {
//            holder.favoriteIcon.setImageResource(R.drawable.baseline_favorite_24);
//        }
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_note_item, parent, false);
        return new NoteViewHolder(view);
    }

    class NoteViewHolder extends RecyclerView.ViewHolder implements View.OnLongClickListener{

        TextView titleTextView, contentTextView, timestampTextView;
        ImageView favoriteIcon;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.note_title_text_view);
            contentTextView = itemView.findViewById(R.id.note_content_text_view);
            timestampTextView = itemView.findViewById(R.id.note_timestamp_text_view);
            itemView.setOnLongClickListener(this);

            favoriteIcon = itemView.findViewById(R.id.favorite_icon);
            favoriteIcon.setOnClickListener(v -> toggleFavorite());
        }

        @SuppressLint("NotifyDataSetChanged")
        private void toggleFavorite() {
            int position = getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                Note note = getItem(position);
                if (note != null) {
                    note.setFavorite(!note.isFavorite());

                    String docId = getSnapshots().getSnapshot(position).getId();
                    DocumentReference documentReference = utility.getCollectionReferenceForNotes().document(docId);

                    // Update the value of 'favorite' in Firestore
                    documentReference.update("favorite", note.isFavorite())
                            .addOnSuccessListener(aVoid -> {
                                if (note.isFavorite()) {
                                    utility.showToast(context, "Note marked as favorite");
                                } else {
                                    utility.showToast(context, "Note removed from favorites");
                                }
                            })
                            .addOnFailureListener(e -> utility.showToast(context, "Failed to update favorite status"));

                    // Notify the adapter that the dataset has changed
                    notifyItemChanged(position);
                }
            }
        }


        public boolean onLongClick(View view) {
            PopupMenu popupMenu = new PopupMenu(context, view);
            popupMenu.getMenuInflater().inflate(R.menu.note_popup_menu, popupMenu.getMenu());

            popupMenu.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.menu_share) {
                    shareNote();
                    return true;
                } else if (id == R.id.menu_delete) {
                    showDeleteConfirmationDialog();
                    return true;
                } else if (id == R.id.menu_favorite) {
                    toggleFavorite();
                    return true;
                }
                return false;
            });
            popupMenu.show();
            return true;
        }
        private void shareNote() {
            String title = getItem(getBindingAdapterPosition()).getTitle();
            String content = getItem(getBindingAdapterPosition()).getContent();

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            String shareBody = title + "\n\n" + content;
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Shared Note");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
            context.startActivity(Intent.createChooser(shareIntent, "Share Note"));
        }

        private void showDeleteConfirmationDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Delete Note");
            builder.setMessage("Are you sure you want to delete this note?");
            builder.setPositiveButton("Delete", (dialogInterface, i) -> deleteNote());
            builder.setNegativeButton("Cancel", null);
            AlertDialog dialog = builder.create();
            dialog.show();
        }

        private void deleteNote() {
            String docId = getSnapshots().getSnapshot(getBindingAdapterPosition()).getId();
            DocumentReference documentReference = utility.getCollectionReferenceForNotes().document(docId);

            documentReference.delete()
                    .addOnSuccessListener(aVoid -> {
                        utility.showToast(context, "Note deleted successfully");
                        notifyItemRemoved(getBindingAdapterPosition());
                        notifyItemRangeChanged(getBindingAdapterPosition(), getItemCount());
                    })
                    .addOnFailureListener(e -> utility.showToast(context, "Failed to delete note"));
        }
    }
}