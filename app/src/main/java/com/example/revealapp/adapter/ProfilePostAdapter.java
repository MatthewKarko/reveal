package com.example.revealapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.revealapp.R;
import com.example.revealapp.model.Post;
import com.example.revealapp.modules.GlideApp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class ProfilePostAdapter extends RecyclerView.Adapter<ProfilePostAdapter.ViewHolder> {

    private List<Post> postList;

    public ProfilePostAdapter(List<Post> postList) {
        this.postList = postList;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return new ProfilePostAdapter.ViewHolder(inflater.inflate(R.layout.profile_post_layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Post post = postList.get(postList.size() - 1 - position);


        DocumentReference theme = post.getTheme();

        if (theme != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("Themes")
                    .document(theme.getId())
                    .get()
                    .addOnCompleteListener(task -> {
                        DocumentSnapshot documentSnapshot = task.getResult();
                        holder.challengeText.setText(documentSnapshot.getString("Title"));

                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy");
                        Date date = documentSnapshot.getTimestamp("Date").toDate();
                        holder.dateText.setText(simpleDateFormat.format(date));

                    });
        }
        else {
            holder.challengeText.setText("Theme");
            holder.dateText.setText("01/01/2023");
        }

        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
        if (post.getPicture() != null && !Objects.equals(post.getPicture(), "")){
            StorageReference picReference = storageReference.child(post.getPicture());

            GlideApp.with(holder.itemView.getContext())
                    .load(picReference)
                    .placeholder(R.drawable.ic_launcher_foreground) // Placeholder image
                    .error(R.drawable.default_homepage_image) // Error image if loading fails
                    .into(holder.image);
        } else{
            GlideApp.with(holder.itemView.getContext())
                    .load(R.drawable.default_homepage_image)
                    .error(R.drawable.default_homepage_image) // Error image if loading fails
                    .into(holder.image);
        }
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        public ImageView image;
        public TextView dateText;
        public TextView challengeText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            image = itemView.findViewById(R.id.image);
            dateText = itemView.findViewById(R.id.dateText);
            challengeText = itemView.findViewById(R.id.challengeText);
        }
    }
}
