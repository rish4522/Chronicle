package com.example.chronicle;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

public class FavoritesFragment extends Fragment {
    private FirestoreRecyclerOptions<Note> options;
    private FirestoreRecyclerAdapter<Note, NoteAdapter.NoteViewHolder> adapter;
    private RecyclerView recyclerView;

    public FavoritesFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);
        recyclerView = view.findViewById(R.id.favorite_recycler_view);
        setupRecyclerView();

        ImageView backIcon = view.findViewById(R.id.back_icon);
        backIcon.setOnClickListener(v -> {
            // Close the current fragment
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        return view;
    }

    public void setOptions(FirestoreRecyclerOptions<Note> options) {
        this.options = options;
    }

    private void setupRecyclerView() {
        // Create the FirestoreRecyclerAdapter using the options
        adapter = new NoteAdapter(options, requireContext());

        // Set the adapter to the RecyclerView
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
    }

    @Override
    public void onStart() {
        super.onStart();
        if (adapter != null) {
            adapter.startListening(); // Start listening for Firestore data changes
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.stopListening(); // Stop listening when the fragment is stopped
        }
    }
}
